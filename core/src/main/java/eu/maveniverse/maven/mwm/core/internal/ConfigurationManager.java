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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class ConfigurationManager {
    private static final String CONFIG_FILE = "mwm.properties";
    private static final String WORKSPACE_FILE = "workspace.properties";

    /**
     * Loads the configuration file, in this order:
     * <ul>
     *     <li>from passed in {@code projectDirectory} as {@code .mvn-local/mwm.properties}</li>
     *     <li>if not found, and properties contains {@code nisse.jgit.commonDir}, then try from there (from path as above)</li>
     *     <li>if not found, from {@code ~/.m2/mwm.properties}</li>
     * </ul>
     */
    public Config getConfig(Path projectDirectory, Map<String, String> properties) throws IOException {
        Optional<Map<String, String>> config =
                load(projectDirectory.resolve(Config.MVN_LOCAL).resolve(CONFIG_FILE));
        if (!config.isPresent()) {
            if (properties.containsKey(PropertiesManager.KEY_COMMON_DIR)) {
                config = load(Paths.get(properties.get(PropertiesManager.KEY_COMMON_DIR))
                        .getParent()
                        .resolve(Config.MVN_LOCAL)
                        .resolve(CONFIG_FILE));
            }
            if (!config.isPresent()) {
                config = load(FileUtils.discoverUserCurrentWorkingDirectory()
                        .resolve(".m2")
                        .resolve(CONFIG_FILE));
            }
        }
        return new ConfigImpl(config.orElse(new HashMap<>()));
    }

    public Optional<Map<String, String>> loadWorkspace(Path projectDirectory) throws IOException {
        return load(projectDirectory.resolve(Config.MVN_LOCAL).resolve(WORKSPACE_FILE));
    }

    public void saveWorkspace(Path projectDirectory, Map<String, String> properties) throws IOException {
        save(projectDirectory.resolve(Config.MVN_LOCAL).resolve(WORKSPACE_FILE), properties);
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

    private void save(Path file, Map<String, String> properties) throws IOException {
        Files.createDirectories(file.getParent());
        FileUtils.writeFile(file, f -> {
            Properties p = new Properties();
            p.putAll(properties);
            try (OutputStream out = Files.newOutputStream(f)) {
                p.store(out, null);
            }
        });
    }
}
