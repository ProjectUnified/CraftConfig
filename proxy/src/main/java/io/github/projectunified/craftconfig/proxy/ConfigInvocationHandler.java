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
    private static final DefaultMethodHandler DEFAULT_METHOD_HANDLER = new DefaultMethodHandler();

    private final Class<T> clazz;
    private final ConfigNode node;
    private final boolean stickyValue;
    private final Map<List<String>, Object> subProxies = new ConcurrentHashMap<>();
    private final Map<List<String>, Object> cachedValues = new ConcurrentHashMap<>();
    private final Set<List<String>> stickyKeys = new HashSet<>();

    ConfigInvocationHandler(Class<T> clazz, ConfigNode node, boolean stickyValue, boolean addDefault) {
        this.clazz = clazz;
        this.node = node;
        this.stickyValue = stickyValue;
        if (addDefault) {
            setupDefaults();
        }
    }

    private void setupDefaults() {
        for (Method method : getAllDeclaredMethods(clazz)) {
            if (!method.isAnnotationPresent(ConfigPath.class)) continue;
            if (method.getParameterCount() != 0) continue;
            if (method.getReturnType() == void.class || method.getReturnType() == Void.class) continue;

            String[] path = extractPath(method);
            if (path == null) continue;

            if (isConfigInterface(method.getReturnType())) {
                ConfigNode childNode = node.node(path);
                new ConfigInvocationHandler<>(method.getReturnType(), childNode, stickyValue, true);
                continue;
            }

            ConfigPath configPath = method.getAnnotation(ConfigPath.class);
            Converter converter = DefaultConverterManager.getConverterIfDefault(
                    method.getGenericReturnType(), configPath.converter());

            Object defaultValue = invokeDefaultMethod(method);
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
            return handleGetter(method);
        }

        if (method.getParameterCount() == 1 && method.getReturnType() == void.class) {
            return handleSetter(method, args[0]);
        }

        if (method.isDefault()) {
            return DEFAULT_METHOD_HANDLER.invoke(proxy, method, args);
        }

        throw new UnsupportedOperationException("Method " + name + " is not supported");
    }

    private Object handleGetter(Method method) {
        String[] path = extractPath(method);
        if (path == null && method.isDefault()) {
            return invokeDefaultMethod(method);
        }
        if (path == null) {
            throw new UnsupportedOperationException("Method " + method.getName() + " has no @ConfigPath");
        }

        if (isConfigInterface(method.getReturnType())) {
            return getSubProxy(path, method.getReturnType());
        }

        List<String> pathKey = Arrays.asList(path);
        if (stickyKeys.contains(pathKey) && cachedValues.containsKey(pathKey)) {
            return cachedValues.get(pathKey);
        }

        ConfigPath configPath = method.getAnnotation(ConfigPath.class);
        Converter converter = DefaultConverterManager.getConverterIfDefault(
                method.getGenericReturnType(),
                configPath != null ? configPath.converter() : DefaultConverter.class);

        Object rawValue = node.node(path).getNormalized();
        Object value = converter.convert(rawValue);
        Object result = value != null ? value : invokeDefaultMethod(method);

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
        Converter converter = DefaultConverterManager.getConverterIfDefault(
                method.getParameterTypes()[0],
                configPath != null ? configPath.converter() : DefaultConverter.class);

        node.node(path).set(converter.convertToRaw(value));
        cachedValues.remove(Arrays.asList(path));

        node.getConfig().save();

        return null;
    }

    private String[] extractPath(Method method) {
        ConfigPath configPath = method.getAnnotation(ConfigPath.class);
        return configPath != null ? configPath.value() : null;
    }

    private String[] findSetterPath(Method setterMethod) {
        String setterName = setterMethod.getName();
        String baseName = stripPrefix(setterName);
        if (baseName == null) baseName = setterName;

        for (Method method : getAllDeclaredMethods(clazz)) {
            if (method.equals(setterMethod)) continue;
            if (method.getParameterCount() != 0) continue;
            if (method.getReturnType() == void.class) continue;

            String methodName = method.getName();
            String stripped = stripPrefix(methodName);
            String nameWithoutPrefix = stripped != null ? stripped : methodName;

            if (nameWithoutPrefix.equalsIgnoreCase(baseName)) {
                String[] path = extractPath(method);
                if (path != null) return path;
            }
        }

        return null;
    }

    private String stripPrefix(String name) {
        for (String[] prefix : new String[][]{{"get", "3"}, {"is", "2"}, {"set", "3"}}) {
            if (name.startsWith(prefix[0]) && name.length() > Integer.parseInt(prefix[1])) {
                int i = Integer.parseInt(prefix[1]);
                return Character.toLowerCase(name.charAt(i)) + name.substring(i + 1);
            }
        }
        return null;
    }

    private Object invokeDefaultMethod(Method method) {
        try {
            return DEFAULT_METHOD_HANDLER.invoke(method);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to invoke default method: " + method.getName(), e);
        }
    }

    private boolean isConfigInterface(Class<?> type) {
        return type.isAnnotationPresent(io.github.projectunified.craftconfig.annotation.ConfigNode.class)
                && !type.equals(Config.class)
                && !type.equals(ConfigNode.class);
    }

    private static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        Set<String> seen = new HashSet<>();
        List<Method> result = new ArrayList<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            for (Method method : current.getDeclaredMethods()) {
                String key = method.getName() + Arrays.toString(method.getParameterTypes());
                if (seen.add(key)) {
                    result.add(method);
                }
            }
            Collections.addAll(queue, current.getInterfaces());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <S> S getSubProxy(String[] path, Class<S> subInterface) {
        List<String> pathKey = Arrays.asList(path);
        return (S) subProxies.computeIfAbsent(pathKey, key ->
                Proxy.newProxyInstance(
                        subInterface.getClassLoader(),
                        new Class[]{subInterface},
                        new ConfigInvocationHandler<>(subInterface, node.node(path), stickyValue, false)));
    }
}
