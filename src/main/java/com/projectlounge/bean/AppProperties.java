package com.projectlounge.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by main on 24.08.17.
 */
@Component
public class AppProperties {

    private static final Logger log = LoggerFactory.getLogger(AppProperties.class.getName());

    private final Properties properties;

    public AppProperties() throws IOException {
        properties = new Properties();
        try {
            final String path = "appProperties.txt";
            final boolean isNew = new File(path).createNewFile();
            if (!isNew) properties.load(Files.newBufferedReader(Paths.get(path)));
        } catch (IOException e) {
            log.error("Error reading settings", e);
        }
        log.debug("Settings: [{}]", properties);
    }

    public String get(final String name) {
        return properties.getProperty(name);
    }

    public Integer getInt(final String name) {
        return Integer.valueOf(properties.getProperty(name));
    }


}
