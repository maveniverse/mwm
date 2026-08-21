/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support abstract class.
 */
public abstract class AbstractMojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected MavenSession mavenSession;

    @Inject
    protected WorkspaceManager workspaceManager;

    protected Map<String, String> getConfigPropertiesAsString() {
        Map<String, String> configProperties = new HashMap<>();
        mavenSession.getRepositorySession().getConfigProperties().forEach((key, value) -> {
            if (value instanceof String) {
                configProperties.put(key, (String) value);
            }
        });
        return configProperties;
    }

    protected Optional<Workspace> getWorkspace() {
        Workspace workspace =
                (Workspace) mavenSession.getRepositorySession().getData().get(Workspace.class);
        if (workspace == null) {
            Optional<Workspace> wo = workspaceManager.detectWorkspace(
                    mavenSession.getRequest().getMultiModuleProjectDirectory().toPath(),
                    mavenSession
                            .getRepositorySession()
                            .getLocalRepository()
                            .getBasedir()
                            .toPath(),
                    getConfigPropertiesAsString());
            if (wo.isPresent()) {
                workspace = wo.orElseThrow(() -> new IllegalStateException("Workspace not found"));
            }
        }
        return Optional.ofNullable(workspace);
    }

    protected void dumpWorkspace(Workspace workspace) {
        logger.info("WS ID     = {}", workspace.workspaceId());
        logger.info("WS DIS    = {}", workspace.discriminator());
        logger.info("WS cache  = {}", workspace.buildCacheDirectory());
        logger.info("WS output = {}", workspace.buildOutputDirectory());
        logger.info("--------------");
        logger.info("Considered properties:");
        workspace.properties().forEach((key, value) -> logger.info("{} = {}", key, value));
        logger.info("--------------");
    }
}
