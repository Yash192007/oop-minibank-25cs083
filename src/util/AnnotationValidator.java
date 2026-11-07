package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import model.annotation.MaxLength;
import model.annotation.Positive;

public class AnnotationValidator {
    public static String[] validate(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Object to validate cannot be null.");
        }

        List<String> errors = new ArrayList<>();
        for (Class<?> type = obj.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Positive.class) && !field.isAnnotationPresent(MaxLength.class)) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    Positive positive = field.getAnnotation(Positive.class);
                    if (positive != null && value instanceof Number number && number.doubleValue() <= 0) {
                        errors.add(field.getName() + " " + positive.message());
                    }

                    MaxLength maxLength = field.getAnnotation(MaxLength.class);
                    if (maxLength != null && value instanceof String text && text.length() > maxLength.value()) {
                        errors.add(field.getName() + " must have at most " + maxLength.value() + " characters");
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to validate field " + field.getName(), e);
                }
            }
        }

        return errors.toArray(String[]::new);
    }
}
