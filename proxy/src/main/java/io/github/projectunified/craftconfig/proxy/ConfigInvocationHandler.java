package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.StickyValue;
import io.github.projectunified.craftconfig.annotation.converter.manager.DefaultConverterManager;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.proxy.defaulthandler.DefaultMethodHandler;
import io.github.projectunified.craftconfig.proxy.defaulthandler.NewJavaDefaultMethodHandler;
import io.github.projectunified.craftconfig.proxy.defaulthandler.OldJavaDefaultMethodHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

/**
 * The internal invocation handler to map the interface to the config
 */
public class ConfigInvocationHandler<T> implements InvocationHandler {
    private static final DefaultMethodHandler DEFAULT_METHOD_HANDLER;

    static {
        final float version = Float.parseFloat(System.getProperty("java.class.version"));
        if (version <= 52) {
            DEFAULT_METHOD_HANDLER = new OldJavaDefaultMethodHandler();
        } else {
            DEFAULT_METHOD_HANDLER = new NewJavaDefaultMethodHandler();
        }
    }

    private final Map<String, MethodConfigNode> nodes = new HashMap<>();
    private final Class<T> clazz;
    private final Config config;
    private final boolean stickyValue;

    ConfigInvocationHandler(Class<T> clazz, Config config, boolean stickyValue, boolean addDefault) {
        this.clazz = clazz;
        this.config = config;
        this.stickyValue = stickyValue;

        Stream<MethodConfigNode> configNodes = Arrays.stream(this.clazz.getDeclaredMethods())
                .sorted(ConfigInvocationHandler::compareMethod)
                .flatMap(method -> this.setupMethod(method).map(Stream::of).orElseGet(Stream::empty));

        if (addDefault) {
            configNodes.forEach(MethodConfigNode::addDefault);
            this.setupClassComment();
            this.config.save();
        }
    }

    private static String extractMethodName(String name) {
        if (name.startsWith("get")) return name.substring(3);
        if (name.startsWith("is")) return name.substring(2);
        return name;
    }

    private static int compareMethod(Method method1, Method method2) {
        boolean annotated1 = method1.isAnnotationPresent(ConfigPath.class);
        boolean annotated2 = method2.isAnnotationPresent(ConfigPath.class);
        if (!annotated1 && !annotated2) return 0;
        if (!annotated1) return 1;
        if (!annotated2) return -1;
        ConfigPath configPath1 = method1.getAnnotation(ConfigPath.class);
        ConfigPath configPath2 = method2.getAnnotation(ConfigPath.class);
        return Integer.compare(configPath1.priority(), configPath2.priority());
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isAssignableFrom(Boolean.class) || clazz.isAssignableFrom(Byte.class)
                || clazz.isAssignableFrom(Character.class) || clazz.isAssignableFrom(Short.class)
                || clazz.isAssignableFrom(Integer.class) || clazz.isAssignableFrom(Long.class)
                || clazz.isAssignableFrom(Float.class) || clazz.isAssignableFrom(Double.class);
    }

    private static boolean isVoidMethod(Method method) {
        return method.getReturnType() == void.class || method.getReturnType() == Void.class;
    }

    private void setupClassComment() {
        if (clazz.isAnnotationPresent(Comment.class) && config.getComment().isEmpty()) {
            config.setComment(Arrays.asList(clazz.getAnnotation(Comment.class).value()));
        }
    }

    private Optional<MethodConfigNode> setupMethod(Method method) {
        if (!method.isDefault() || method.getParameterCount() != 0) {
            return Optional.empty();
        }

        String methodName = extractMethodName(method.getName());
        if (methodName.isEmpty()) {
            return Optional.empty();
        }

        if (!method.isAnnotationPresent(ConfigPath.class)) {
            return Optional.empty();
        }
        ConfigPath configPath = method.getAnnotation(ConfigPath.class);
        String[] path = configPath.value();

        try {
            Object value = DEFAULT_METHOD_HANDLER.invoke(method);
            MethodConfigNode node = new MethodConfigNode(
                    path, config, DefaultConverterManager.getConverterIfDefault(method.getGenericReturnType(), configPath.converter()), value,
                    method.isAnnotationPresent(Comment.class) ? Arrays.asList(method.getAnnotation(Comment.class).value()) : Collections.emptyList(),
                    stickyValue || method.isAnnotationPresent(StickyValue.class)
            );
            nodes.put(methodName, node);
            return Optional.of(node);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to setup method " + method.getName(), e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ((name.equals("getConfig") || name.equals("config")) && !method.isDefault() && method.getParameterCount() == 0 && method.getReturnType().isInstance(config)) {
            return config;
        } else if (name.equals("toString")) {
            return this.clazz.toString();
        } else if (name.equals("hashCode")) {
            return this.clazz.hashCode();
        } else if (name.equals("equals")) {
            return proxy == args[0];
        } else if ((name.equals("reloadConfig") || name.equals("reload")) && !method.isDefault() && method.getParameterCount() == 0) {
            config.reload();
            nodes.values().forEach(MethodConfigNode::clearCache);
            return null;
        } else if (!isVoidMethod(method) && method.isDefault() && method.getParameterCount() == 0 && method.isAnnotationPresent(ConfigPath.class)) {
            String methodName = extractMethodName(name);
            if (nodes.containsKey(methodName)) {
                Object value = nodes.get(methodName).getValue();
                if ((isPrimitiveOrWrapper(method.getReturnType()) && isPrimitiveOrWrapper(value.getClass())) || method.getReturnType().isInstance(value)) {
                    return value;
                }
            }
        } else if (isVoidMethod(method) && !method.isDefault() && method.getParameterCount() == 1) {
            String methodName;
            if (name.startsWith("set")) {
                methodName = name.substring(3);
            } else {
                methodName = name;
            }
            if (nodes.containsKey(methodName)) {
                nodes.get(methodName).setValue(args[0]);
                config.save();
                return null;
            }
        }
        if (method.isDefault()) {
            return DEFAULT_METHOD_HANDLER.invoke(proxy, method, args);
        }
        throw new UnsupportedOperationException("Method " + method.getName() + " is not supported");
    }
}
