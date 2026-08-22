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
import java.io.IOException;
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

    protected Optional<Workspace> getWorkspace() throws IOException {
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
        dumpWorkspace("", workspace);
    }

    private void dumpWorkspace(String indent, Workspace workspace) {
        logger.info("{}WS ID     = {}", indent, workspace.workspaceId());
        logger.info("{}WS DIS    = {}", indent, workspace.discriminator());
        logger.info("{}WS cache  = {}", indent, workspace.buildCacheDirectory());
        logger.info("{}WS output = {}", indent, workspace.buildOutputDirectory());
        logger.info("{}----------------------", indent);
        logger.info("{}Considered properties:", indent);
        workspace.properties().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> logger.info("{}{} = {}", indent, e.getKey(), e.getValue()));
        logger.info("{}----------------------", indent);
        if (!workspace.linkedWorkspaces().isEmpty()) {
            logger.info("{}Linked workspaces:", indent);
            for (Workspace w : workspace.linkedWorkspaces()) {
                dumpWorkspace(indent + "  ", w);
            }
        }
    }
}
