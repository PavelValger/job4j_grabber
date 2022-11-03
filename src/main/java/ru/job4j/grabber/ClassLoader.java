package ru.job4j.grabber;

import java.util.Properties;

public interface ClassLoader {
    void load();

    Properties getProperties();
}
