package io.github.projectunified.craftconfig.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class DefaultMethodHandler {
    private static final Map<Method, MethodHandle> CACHE = new ConcurrentHashMap<>();

    static Object invoke(Object proxy, Method method, Object... args) throws Throwable {
        MethodHandle handle = CACHE.computeIfAbsent(method, DefaultMethodHandler::resolveHandle);
        return handle.bindTo(proxy).invokeWithArguments(args);
    }

    static Object invoke(Method method, Object... args) throws Throwable {
        Class<?> clazz = method.getDeclaringClass();
        Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (o, m, a) -> null);
        return invoke(proxy, method, args);
    }

    private static MethodHandle resolveHandle(Method method) {
        try {
            return MethodHandles.lookup()
                    .findSpecial(
                            method.getDeclaringClass(),
                            method.getName(),
                            MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                            method.getDeclaringClass()
                    );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }

        try {
            Constructor<MethodHandles.Lookup> constructor =
                    MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            Class<?> clazz = method.getDeclaringClass();
            return constructor.newInstance(clazz)
                    .in(clazz)
                    .unreflectSpecial(method, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve default method: " + method.getName(), e);
        }
    }
}
