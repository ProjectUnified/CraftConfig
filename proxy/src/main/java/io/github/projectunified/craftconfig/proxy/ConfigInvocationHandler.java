package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.StickyValue;
import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.impl.DefaultConverter;
import io.github.projectunified.craftconfig.annotation.converter.manager.DefaultConverterManager;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigNode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigInvocationHandler<T> implements InvocationHandler {
    private final Class<T> clazz;
    private final ConfigNode node;
    private final boolean stickyValue;
    private final List<Method> allMethods;
    private final Map<List<String>, Object> subProxies = new ConcurrentHashMap<>();
    private final Map<List<String>, Object> cachedValues = new ConcurrentHashMap<>();
    private final Set<List<String>> stickyKeys = new HashSet<>();

    ConfigInvocationHandler(Class<T> clazz, ConfigNode node, boolean stickyValue, boolean addDefault) {
        this.clazz = clazz;
        this.node = node;
        this.stickyValue = stickyValue;
        this.allMethods = getAllDeclaredMethods(clazz);
        if (addDefault) {
            setupDefaults();
        }
    }

    private static Converter resolveConverter(java.lang.reflect.Type returnType, ConfigPath configPath) {
        return DefaultConverterManager.getConverterIfDefault(
                returnType,
                configPath != null ? configPath.converter() : DefaultConverter.class);
    }

    private static String stripPrefix(String name) {
        if ((name.startsWith("get") || name.startsWith("set")) && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    private static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        Set<String> seen = new HashSet<>();
        List<Method> result = new ArrayList<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            for (Method method : current.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge()) continue;
                String key = method.getName() + Arrays.toString(method.getParameterTypes());
                if (seen.add(key)) {
                    result.add(method);
                }
            }
            Collections.addAll(queue, current.getInterfaces());
        }
        return result;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if (name.equals("getConfig") || name.equals("config")) {
            if (method.getReturnType().isInstance(node.getConfig())) {
                return node.getConfig();
            }
        }
        if (name.equals("toString")) return clazz.toString();
        if (name.equals("hashCode")) return clazz.hashCode();
        if (name.equals("equals")) return proxy == args[0];

        if ((name.equals("reloadConfig") || name.equals("reload"))
                && method.getParameterCount() == 0 && method.getReturnType() == void.class) {
            if (node instanceof Config) {
                ((Config) node).reload();
            }
            cachedValues.clear();
            subProxies.clear();
            return null;
        }

        if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
            return handleGetter(proxy, method);
        }

        if (method.getParameterCount() == 1 && method.getReturnType() == void.class) {
            if (method.isDefault()) {
                return DefaultMethodHandler.invoke(proxy, method, args);
            }
            return handleSetter(method, args[0]);
        }

        if (method.isDefault()) {
            return DefaultMethodHandler.invoke(proxy, method, args);
        }

        throw new UnsupportedOperationException("Method " + name + " is not supported");
    }

    private void setupDefaults() {
        List<Method> sorted = new ArrayList<>(allMethods);
        sorted.sort(Comparator.comparingInt(m -> {
            ConfigPath cp = m.getAnnotation(ConfigPath.class);
            return cp != null ? cp.priority() : 0;
        }));

        for (Method method : sorted) {
            ConfigPath configPath = method.getAnnotation(ConfigPath.class);
            if (configPath == null) continue;
            if (method.getParameterCount() != 0) continue;
            if (method.getReturnType() == void.class || method.getReturnType() == Void.class) continue;

            String[] path = configPath.value();
            if (isConfigInterface(method.getReturnType())) {
                ConfigNode childNode = node.node(path);
                new ConfigInvocationHandler<>(method.getReturnType(), childNode, stickyValue, true);
                continue;
            }

            Converter converter = resolveConverter(method.getGenericReturnType(), configPath);
            Object defaultValue = invokeDefaultMethod(null, method);
            ConfigNode childNode = node.node(path);

            if (!childNode.exists()) {
                childNode.set(converter.convertToRaw(defaultValue));
            }

            if (method.isAnnotationPresent(Comment.class)) {
                String[] comment = method.getAnnotation(Comment.class).value();
                if (childNode.getComment().isEmpty()) {
                    childNode.setComment(Arrays.asList(comment));
                }
            }

            if (stickyValue || method.isAnnotationPresent(StickyValue.class)) {
                stickyKeys.add(Arrays.asList(path));
            }
        }

        if (clazz.isAnnotationPresent(Comment.class) && node.getComment().isEmpty()) {
            node.setComment(Arrays.asList(clazz.getAnnotation(Comment.class).value()));
        }

        node.getConfig().save();
    }

    private Object handleGetter(Object proxy, Method method) {
        ConfigPath configPath = method.getAnnotation(ConfigPath.class);
        if (configPath == null && method.isDefault()) {
            return invokeDefaultMethod(proxy, method);
        }
        if (configPath == null) {
            throw new UnsupportedOperationException("Method " + method.getName() + " has no @ConfigPath");
        }

        String[] path = configPath.value();

        if (isConfigInterface(method.getReturnType())) {
            return getSubProxy(node.node(path), method.getReturnType());
        }

        List<String> pathKey = Arrays.asList(path);
        if (stickyKeys.contains(pathKey) && cachedValues.containsKey(pathKey)) {
            return cachedValues.get(pathKey);
        }

        Converter converter = resolveConverter(method.getGenericReturnType(), configPath);
        Object rawValue = node.node(path).getNormalized();
        Object value = converter.convert(rawValue);
        Object result = value != null ? value : invokeDefaultMethod(proxy, method);

        if (stickyKeys.contains(pathKey)) {
            cachedValues.put(pathKey, result);
        }

        return result;
    }

    private Object handleSetter(Method method, Object value) {
        String[] path = findSetterPath(method);
        if (path == null) {
            throw new UnsupportedOperationException("Method " + method.getName() + " has no @ConfigPath");
        }

        ConfigPath configPath = method.getAnnotation(ConfigPath.class);
        Converter converter = resolveConverter(method.getParameterTypes()[0], configPath);

        node.node(path).set(converter.convertToRaw(value));
        cachedValues.remove(Arrays.asList(path));
        node.getConfig().save();

        return null;
    }

    private String[] findSetterPath(Method setterMethod) {
        String baseName = stripPrefix(setterMethod.getName());
        if (baseName == null) baseName = setterMethod.getName();

        for (Method method : allMethods) {
            if (method.equals(setterMethod)) continue;
            if (method.getParameterCount() != 0) continue;
            if (method.getReturnType() == void.class) continue;

            ConfigPath configPath = method.getAnnotation(ConfigPath.class);
            if (configPath == null) continue;

            String nameWithoutPrefix = stripPrefix(method.getName());
            if ((nameWithoutPrefix != null ? nameWithoutPrefix : method.getName()).equalsIgnoreCase(baseName)) {
                return configPath.value();
            }
        }

        return null;
    }

    private Object invokeDefaultMethod(Object proxy, Method method) {
        try {
            return proxy != null
                    ? DefaultMethodHandler.invoke(proxy, method)
                    : DefaultMethodHandler.invoke(method);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to invoke default method: " + method.getName(), e);
        }
    }

    private boolean isConfigInterface(Class<?> type) {
        return type.isAnnotationPresent(io.github.projectunified.craftconfig.annotation.ConfigNode.class)
                && !type.equals(Config.class)
                && !type.equals(ConfigNode.class);
    }

    @SuppressWarnings("unchecked")
    private <S> S getSubProxy(ConfigNode childNode, Class<S> subInterface) {
        List<String> pathKey = Arrays.asList(childNode.getPath());
        return (S) subProxies.computeIfAbsent(pathKey, key ->
                Proxy.newProxyInstance(
                        subInterface.getClassLoader(),
                        new Class[]{subInterface},
                        new ConfigInvocationHandler<>(subInterface, childNode, stickyValue, false)));
    }
}
