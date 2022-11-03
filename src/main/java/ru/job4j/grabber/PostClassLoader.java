package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PostClassLoader implements ClassLoader {
    private final Properties properties = new Properties();

    public PostClassLoader() {
        load();
    }

    @Override
    public void load() {
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("post.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
