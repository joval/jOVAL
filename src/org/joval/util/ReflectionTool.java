// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Some convenience methods for working with Java introspection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ReflectionTool {
    private static Map<String, Map<String, Method>> METHOD_REGISTRY = new HashMap<String, Map<String, Method>>();

    /**
     * Recursively get all the interfaces implemented by the specified class.
     */
    public static Collection<Class<?>> getInterfaces(Class<?> clazz) {
        Collection<Class<?>> accumulator = new HashSet<Class<?>>();
        getInterfaces(clazz, accumulator);
        return accumulator;
    }

    /**
     * Use introspection to list all the no-argument methods of the specified Class, organized by name.
     */
    public static Map<String, Method> getMethods(Class clazz) {
        String className = clazz.getName();
        if (METHOD_REGISTRY.containsKey(className)) {
            return METHOD_REGISTRY.get(className);
        } else {
            Map<String, Method> methods = new HashMap<String, Method>();
            Method[] m = clazz.getMethods();
            for (int i=0; i < m.length; i++) {
                methods.put(m[i].getName(), m[i]);
            }
            METHOD_REGISTRY.put(className, methods);
            return methods;
        }
    }

    /**
     * Use introspection to get the no-argument method of the specified Class, with the specified name.
     */
    public static Method getMethod(Class clazz, String name) throws NoSuchMethodException {
        Map<String, Method> methods = getMethods(clazz);
        if (methods.containsKey(name)) {
            return methods.get(name);
        } else {
            throw new NoSuchMethodException(clazz.getName() + "." + name + "()");
        }
    }

    /**
     * Safely invoke a method that takes no arguments and returns an Object. Errors are logged to the JOVALMsg default logger.
     *
     * @returns null if the method is not implemented, if there was an error, or if the method returned null.
     */
    public static Object invokeMethod(Object obj, String name) {
        Object result = null;
        try {
            Method m = obj.getClass().getMethod(name);
            result = m.invoke(obj);
        } catch (NoSuchMethodException e) {
            // Object doesn't implement the method; no big deal.
        } catch (IllegalAccessException e) {
            JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        } catch (InvocationTargetException e) {
            JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        }
        return result;
    }

    // Private

    private static void getInterfaces(Class<?> clazz, Collection<Class<?>> accumulator) {
        if (clazz != null && !accumulator.contains(clazz)) {
            for (Class<?> iface : clazz.getInterfaces()) {
                accumulator.add(iface);
                getInterfaces(iface.getSuperclass());
            }
            getInterfaces(clazz.getSuperclass(), accumulator);
        }
    }
}
