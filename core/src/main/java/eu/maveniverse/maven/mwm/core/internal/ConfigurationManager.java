/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import eu.maveniverse.maven.mwm.core.Config;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class ConfigurationManager {
    public Config getConfig(Path projectDirectory, Map<String, String> properties) throws IOException {
        return new Config() {};
    }

    public Optional<Map<String, String>> load(String workspaceId) throws IOException {
        return Optional.empty();
    }

    public void save(Map<String, String> properties) throws IOException {}
}
