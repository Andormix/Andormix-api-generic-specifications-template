package com.andormix.swipemarketapi.common.util;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class GenericSpecifications {

    private GenericSpecifications() {
        // Clase de utilidad no instanciable
    }

    /**
     * Búsqueda por texto (LIKE %search%) ignorando mayúsculas/minúsculas.
     */
    public static <T> Specification<T> containsText(String fieldName, String value) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return builder.conjunction(); // Equivalente a "1=1" (no filtra)
            }
            return builder.like(
                    builder.lower(root.get(fieldName)),
                    "%" + value.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Igualdad exacta (field = value). Sirve para Enums, Strings, Longs, Booleans, etc.
     */
    public static <T, V> Specification<T> isEqualTo(String fieldName, V value) {
        return (root, query, builder) -> {
            if (value == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get(fieldName), value);
        };
    }

    /**
     * Rango: Mayor o igual que (field >= value).
     */
    public static <T, V extends Comparable<? super V>> Specification<T> greaterThanOrEqualTo(String fieldName, V value) {
        return (root, query, builder) -> {
            if (value == null) {
                return builder.conjunction();
            }
            return builder.greaterThanOrEqualTo(root.get(fieldName), value);
        };
    }

    /**
     * Rango: Menor o igual que (field <= value).
     */
    public static <T, V extends Comparable<? super V>> Specification<T> lessThanOrEqualTo(String fieldName, V value) {
        return (root, query, builder) -> {
            if (value == null) {
                return builder.conjunction();
            }
            return builder.lessThanOrEqualTo(root.get(fieldName), value);
        };
    }

    /**
     * Navegación por relación (JOIN) para comprobar igualdad (ej: product.seller = user).
     */
    public static <T, V> Specification<T> joinIsEqualTo(String joinField, String fieldName, V value) {
        return (root, query, builder) -> {
            if (value == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get(joinField).get(fieldName), value);
        };
    }
    /**
     * Useful for queries like "Find products where category is IN [ELECTRONICS, CLOTHING]" or "Find orders with
     * status IN [PENDING, SHIPPED]".
     * */
    public static <T, V> Specification<T> isIn(String fieldName, Collection<V> values) {
        return (root, query, builder) -> {
            if (values == null || values.isEmpty()) {
                return builder.conjunction();
            }
            return root.get(fieldName).in(values);
        };
    }

    /**
     * Useful when a single search box needs to check both title and description
     * */
    public static <T> Specification<T> containsTextInFields(String value, String... fieldNames) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return builder.conjunction();
            }
            String pattern = "%" + value.trim().toLowerCase() + "%";

            jakarta.persistence.criteria.Predicate[] predicates = java.util.Arrays.stream(fieldNames)
                    .map(field -> {
                        jakarta.persistence.criteria.Path<String> path;
                        if (field.contains(".")) {
                            String[] parts = field.split("\\.");
                            path = root.get(parts[0]);
                            for (int i = 1; i < parts.length; i++) {
                                path = path.get(parts[i]);
                            }
                        } else {
                            path = root.get(field);
                        }
                        return builder.like(builder.lower(path), pattern);
                    })
                    .toArray(jakarta.persistence.criteria.Predicate[]::new);

            return builder.or(predicates);
        };
    }
}