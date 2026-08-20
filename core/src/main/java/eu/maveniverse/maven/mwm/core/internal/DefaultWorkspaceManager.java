/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Version;
import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceHandler;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import eu.maveniverse.maven.nisse.core.NisseConfiguration;
import eu.maveniverse.maven.nisse.core.NisseManager;
import eu.maveniverse.maven.nisse.core.PropertyKeyNamingStrategies;
import eu.maveniverse.maven.nisse.core.simple.SimpleNisseConfiguration;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class DefaultWorkspaceManager implements WorkspaceManager {
    static final String KEY_REMOTE_NAME = "nisse.jgit.remoteName";
    static final String KEY_REMOTE_URL = "nisse.jgit.remoteUrl";
    static final String KEY_BRANCH_NAME = "nisse.jgit.branchName";
    static final String KEY_COMMON_DIR = "nisse.jgit.commonDir";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NisseManager nisseManager;
    private final Map<String, WorkspaceHandler> workspaceHandlers;

    @Inject
    public DefaultWorkspaceManager(NisseManager nisseManager, Map<String, WorkspaceHandler> workspaceHandlers) {
        this.nisseManager = requireNonNull(nisseManager);
        this.workspaceHandlers = requireNonNull(workspaceHandlers);
        logger.info("MWM {} (Resolver {})", Version.version(), Version.resolverVersion());
    }

    @Override
    public Optional<Workspace> detectWorkspace(
            Path projectDirectory, Path localRepository, Map<String, String> properties) {
        HashMap<String, String> props = new HashMap<>(properties);
        if (!props.containsKey(KEY_REMOTE_NAME)
                || !props.containsKey(KEY_REMOTE_URL)
                || !props.containsKey(KEY_BRANCH_NAME)) {
            logger.debug("Nisse properties absent; running Nisse");
            props.putAll(nisseProperties(projectDirectory, props));
        }
        if (!props.containsKey(KEY_REMOTE_NAME)
                || !props.containsKey(KEY_REMOTE_URL)
                || !props.containsKey(KEY_BRANCH_NAME)) {
            logger.info("Nisse properties absent after running Nisse; bailing out");
            return Optional.empty();
        }
        for (Map.Entry<String, WorkspaceHandler> entry : workspaceHandlers.entrySet()) {
            Optional<Workspace> wo = entry.getValue().detectWorkspace(projectDirectory, localRepository, props);
            if (wo.isPresent()) {
                logger.debug("Workspace detected by handler: {}", entry.getKey());
                return wo;
            }
        }
        logger.debug("No workspace detected by any handler");
        return Optional.empty();
    }

    /**
     * Creates Nisse properties as fallback; if Nisse properties detected, is not invoked.
     */
    private Map<String, String> nisseProperties(Path rootDirectory, Map<String, String> properties) {
        NisseConfiguration configuration = SimpleNisseConfiguration.builder()
                .withSystemProperties(properties)
                .withCurrentWorkingDirectory(rootDirectory)
                .withSessionRootDirectory(rootDirectory)
                .combinePropertyKeyNamingStrategy(PropertyKeyNamingStrategies.translated(
                        Collections.emptyMap(),
                        PropertyKeyNamingStrategies.sourcePrefixed(),
                        PropertyKeyNamingStrategies.defaultStrategy()))
                .build();
        return nisseManager.createProperties(configuration);
    }
}
