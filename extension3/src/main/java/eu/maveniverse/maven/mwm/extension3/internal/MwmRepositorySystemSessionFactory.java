/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.extension3.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Version;
import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.RepositorySystemSessionFactory;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
@Priority(100)
final class MwmRepositorySystemSessionFactory implements RepositorySystemSessionFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DefaultRepositorySystemSessionFactory defaultFactory;
    private final RepositorySystem repositorySystem;
    private final WorkspaceManager workspaceManager;

    @Inject
    public MwmRepositorySystemSessionFactory(
            DefaultRepositorySystemSessionFactory defaultFactory,
            RepositorySystem repositorySystem,
            WorkspaceManager workspaceManager) {
        this.defaultFactory = requireNonNull(defaultFactory);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.workspaceManager = requireNonNull(workspaceManager);
    }

    @Override
    public RepositorySystemSession.SessionBuilder newRepositorySessionBuilder(
            MavenExecutionRequest mavenExecutionRequest) {
        logger.info("MWM {}", Version.version());
        return defaultFactory
                .newRepositorySessionBuilder(mavenExecutionRequest)
                .setLocalRepositoryManager(newLocalRepositoryManager(
                        mavenExecutionRequest, defaultFactory.newRepositorySessionBuilder(mavenExecutionRequest)));
    }

    public LocalRepositoryManager newLocalRepositoryManager(
            MavenExecutionRequest mavenExecutionRequest, RepositorySystemSession.SessionBuilder builder) {
        try (RepositorySystemSession.CloseableSession session = builder.build()) {
            if (session.getLocalRepositoryManager() instanceof ChainedLocalRepositoryManager) {
                logger.info("Chained LRM detected; MWM is not interfering with it");
                return session.getLocalRepositoryManager();
            }

            Map<String, String> configProperties = new HashMap<>();
            session.getConfigProperties().forEach((key, value) -> {
                if (value instanceof String) {
                    configProperties.put(key, (String) value);
                }
            });
            Workspace workspace = workspaceManager
                    .detectWorkspace(
                            mavenExecutionRequest.getRootDirectory(),
                            session.getLocalRepositoryManager().getRepository().getBasePath(),
                            configProperties)
                    .orElse(null);
            if (workspace != null) {
                logger.info("Using MWM workspace: {}", workspace.workspaceId());
                return new ChainedLocalRepositoryManager(
                        session.getLocalRepositoryManager(),
                        Collections.singletonList(repositorySystem.newLocalRepositoryManager(
                                session, new LocalRepository(workspace.buildOutputDirectory()))),
                        false,
                        1,
                        0);
            }

            return session.getLocalRepositoryManager();
        }
    }
}
