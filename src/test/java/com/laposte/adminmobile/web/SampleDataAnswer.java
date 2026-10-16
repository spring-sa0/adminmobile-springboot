package com.laposte.adminmobile.web;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Mockito default answer used by the page-rendering tests: every service method returning a
 * {@code List<T>} gets one fully-populated {@code T} (entity or report row, nested entities included),
 * so that each template's {@code th:each} body is actually evaluated against real property names.
 */
class SampleDataAnswer implements Answer<Object> {

    private static final int MAX_DEPTH = 2;

    @Override
    public Object answer(InvocationOnMock invocation) {
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (returnType == void.class) {
            return null;
        }
        if (List.class.isAssignableFrom(returnType)) {
            Type generic = invocation.getMethod().getGenericReturnType();
            if (generic instanceof ParameterizedType pt && pt.getActualTypeArguments()[0] instanceof Class<?> element) {
                return List.of(sample(element, 0));
            }
            return List.of();
        }
        if (Page.class.isAssignableFrom(returnType)) {
            // Recherches paginees en base : une ligne, sur la page demandee.
            Type generic = invocation.getMethod().getGenericReturnType();
            List<Object> contenu = generic instanceof ParameterizedType pt && pt.getActualTypeArguments()[0] instanceof Class<?> element
                    ? List.of(sample(element, 0)) : List.of();
            Pageable page = invocation.getArguments().length > 0
                    && invocation.getArguments()[invocation.getArguments().length - 1] instanceof Pageable p ? p : Pageable.unpaged();
            return new PageImpl<>(contenu, page, contenu.size());
        }
        if (returnType == byte[].class) {
            return new byte[0];
        }
        return sample(returnType, 0);
    }

    /** Simule une requete paginee en base sur {@code lignes} : renvoie la tranche demandee. */
    static <T> Answer<Page<T>> pageDe(List<T> lignes) {
        return invocation -> {
            Pageable page = invocation.getArgument(invocation.getArguments().length - 1);
            if (page == null || page.isUnpaged()) {
                return new PageImpl<>(lignes);
            }
            int debut = (int) Math.min(page.getOffset(), lignes.size());
            int fin = (int) Math.min(page.getOffset() + page.getPageSize(), lignes.size());
            return new PageImpl<>(lignes.subList(debut, fin), page, lignes.size());
        };
    }

    static Object sample(Class<?> type, int depth) {
        Object simple = simpleValue(type);
        if (simple != null || isSimple(type) || type.isEnum()) {
            return simple;
        }
        if (depth > MAX_DEPTH || type.isInterface() || Modifier.isAbstract(type.getModifiers())
                || Collection.class.isAssignableFrom(type) || type.getName().startsWith("java.")) {
            return null;
        }
        try {
            if (type.isRecord()) {
                Constructor<?> canonical = type.getDeclaredConstructors()[0];
                Object[] args = new Object[canonical.getParameterCount()];
                Class<?>[] params = canonical.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    args[i] = sample(params[i], depth + 1);
                }
                canonical.setAccessible(true);
                return canonical.newInstance(args);
            }
            Constructor<?> noArg = findNoArg(type);
            if (noArg == null) {
                Constructor<?> ctor = type.getDeclaredConstructors()[0];
                Object[] args = new Object[ctor.getParameterCount()];
                Class<?>[] params = ctor.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    args[i] = sample(params[i], depth + 1);
                }
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            }
            noArg.setAccessible(true);
            Object instance = noArg.newInstance();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Collection.class.isAssignableFrom(f.getType())) {
                        continue;
                    }
                    f.setAccessible(true);
                    f.set(instance, sample(f.getType(), depth + 1));
                }
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Constructor<?> findNoArg(Class<?> type) {
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (c.getParameterCount() == 0) {
                return c;
            }
        }
        return null;
    }

    private static boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type.getName().startsWith("java.");
    }

    private static Object simpleValue(Class<?> type) {
        if (type == String.class) return "1";
        if (type == BigDecimal.class || type == Number.class) return BigDecimal.ONE;
        if (type == BigInteger.class) return BigInteger.ONE;
        if (type == Long.class || type == long.class) return 1L;
        if (type == Integer.class || type == int.class) return 1;
        if (type == Short.class || type == short.class) return (short) 1;
        if (type == Double.class || type == double.class) return 1d;
        if (type == Float.class || type == float.class) return 1f;
        if (type == Boolean.class || type == boolean.class) return Boolean.TRUE;
        if (type == Character.class || type == char.class) return '1';
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (Date.class.isAssignableFrom(type)) return new java.sql.Timestamp(System.currentTimeMillis());
        if (type == LocalDate.class) return LocalDate.now();
        return null;
    }
}
