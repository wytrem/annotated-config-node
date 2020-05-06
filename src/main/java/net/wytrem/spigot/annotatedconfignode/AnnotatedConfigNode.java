package net.wytrem.spigot.annotatedconfignode;

import net.wytrem.spigot.commentedyaml.CommentsProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class AnnotatedConfigNode {

    public void storeAtRoot(ConfigurationSection config) {
        this.store(config, "");
    }

    public void store(ConfigurationSection config, String section) {
        this.walkTree(config::set, section, true);
    }

    private void walkTree(BiConsumer<String, Object> consumer, String pathPrefix, boolean deep) {
        String path;
        for (Field field : getAnnotatedFields(this.getClass())) {
            field.setAccessible(true);
            path = field.getName();

            if (pathPrefix != null && !pathPrefix.isEmpty()) {
                path = pathPrefix + "." + path;
            }

            try {
                Object value = field.get(this);

                if (value != null) {
                    if (deep && value instanceof AnnotatedConfigNode) {
                        ((AnnotatedConfigNode) value).walkTree(consumer, path, true);
                    } else {
                        consumer.accept(path, value);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static <C extends AnnotatedConfigNode> C loadAsRoot(Class<C> clazz, YamlConfiguration yaml) {
        return loadSection(clazz, yaml, "");
    }

    public static <C extends AnnotatedConfigNode> C loadSection(Class<C> clazz, YamlConfiguration yaml, String pathPrefix) {
        try {
            Constructor<C> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            C config = constructor.newInstance();

            // Populate config
            populate(clazz, config, yaml, pathPrefix);

            return config;

        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <C extends AnnotatedConfigNode> void populate(Class<C> clazz, C config, YamlConfiguration yaml, String pathPrefix) throws IllegalAccessException {
        Object value;
        String path;
        for (Field field : getAnnotatedFields(clazz)) {
            field.setAccessible(true);
            path = field.getName();

            if (pathPrefix != null && !pathPrefix.isEmpty()) {
                path = pathPrefix + "." + path;
            }

            if (AnnotatedConfigNode.class.isAssignableFrom(field.getType())) {
                value = loadSection((Class<? extends AnnotatedConfigNode>) field.getType(), yaml, path);
            } else {
                value = yaml.get(path);
            }

            if (value != null) {
                field.set(config, value);
            }
        }
    }

    private static final String[] EMPTY = new String[]{};

    public static <C extends AnnotatedConfigNode> CommentsProvider getComments(Class<C> clazz) {
        return commentPath -> {
            try {
                ConfigNode node = findNode(clazz, commentPath);
                return node.comments();
            } catch (NoSuchFieldException e) {
                return EMPTY;
            }
        };
    }

    private static <C extends AnnotatedConfigNode> ConfigNode findNode(Class<C> clazz, String path) throws NoSuchFieldException {
        return findField(clazz, path).getAnnotation(ConfigNode.class);
    }

    @SuppressWarnings("unchecked")
    private static <C extends AnnotatedConfigNode> Field findField(Class<C> clazz, String path) throws NoSuchFieldException {
        if (!path.contains(".")) {
            for (Field field : getAnnotatedFields(clazz)) {
                if (field.getName().equals(path)) {
                    return field;
                }
            }

            throw new NoSuchFieldException();
        } else {
            String subSectionName = path.substring(0, path.indexOf('.'));
            Field subSection = clazz.getDeclaredField(subSectionName);
            Class<?> subSectionType = subSection.getType();
            if (AnnotatedConfigNode.class.isAssignableFrom(subSectionType)) {
                // Delegate to the subsection class.
                // We don't want the '.', so indexOf('.') + 1.
                return findField((Class<? extends AnnotatedConfigNode>) subSectionType, path.substring(path.indexOf('.') + 1));
            } else {
                throw new NoSuchFieldException();
            }
        }
    }

    private static List<Field> getAnnotatedFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        while (AnnotatedConfigNode.class.isAssignableFrom(clazz)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ConfigNode.class)) {
                    fields.add(field);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return fields;
    }
}
