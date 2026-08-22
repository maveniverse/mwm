/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import eu.maveniverse.maven.mwm.core.Config;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class ConfigurationManager {
    private static String CONFIG_FILE = "mwm.properties";

    public Config getConfig(Path projectDirectory, Map<String, String> properties) throws IOException {
        return new Config() {};
    }

    private Optional<Map<String, String>> load(Path file) throws IOException {
        if (Files.isRegularFile(file)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            }
            return Optional.of(MavenUtils.toMap(p));
        }
        return Optional.empty();
    }

    public void save(Path file, Map<String, String> properties) throws IOException {
        Files.createDirectories(file.getParent());
        FileUtils.writeFile(file, f -> {
            Properties p = new Properties();
            p.putAll(properties);
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, null);
            }
        });
    }
}
