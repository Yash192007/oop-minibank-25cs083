package repository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import model.annotation.Id;

public class Repository<T> {
    private final Map<String, T> entities = new HashMap<>();

    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null.");
        }

        Field idField = findIdField(entity.getClass());
        idField.setAccessible(true);
        try {
            Object id = idField.get(entity);
            if (id == null) {
                throw new IllegalArgumentException("@Id field cannot be null.");
            }
            entities.put(id.toString(), entity);
            return entity;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read @Id field " + idField.getName(), e);
        }
    }

    public T findById(String id) {
        return entities.get(id);
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    public boolean delete(String id) {
        return entities.remove(id) != null;
    }

    @SafeVarargs
    public final void saveAll(T... items) {
        for (T item : items) {
            save(item);
        }
    }

    public static <N extends Number> double average(N[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values must contain at least one number.");
        }

        double total = 0;
        for (N value : values) {
            if (value == null) {
                throw new IllegalArgumentException("Values cannot contain null.");
            }
            total += value.doubleValue();
        }
        return total / values.length;
    }

    private Field findIdField(Class<?> entityType) {
        for (Class<?> type = entityType; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field;
                }
            }
        }
        throw new IllegalArgumentException(
                "No field annotated with @Id found for " + entityType.getName());
    }
}
