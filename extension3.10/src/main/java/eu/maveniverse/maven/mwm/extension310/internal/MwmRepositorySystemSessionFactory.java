/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.extension310.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.extension.shared.MwmSessionWrapper;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.RepositorySystemSessionFactory;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.sisu.Priority;

@Singleton
@Named
@Priority(100)
final class MwmRepositorySystemSessionFactory implements RepositorySystemSessionFactory {
    private final DefaultRepositorySystemSessionFactory defaultFactory;
    private final MwmSessionWrapper wrapper;

    @Inject
    public MwmRepositorySystemSessionFactory(
            DefaultRepositorySystemSessionFactory defaultFactory, MwmSessionWrapper wrapper) {
        this.defaultFactory = requireNonNull(defaultFactory);
        this.wrapper = requireNonNull(wrapper);
    }

    @Override
    public RepositorySystemSession.SessionBuilder newRepositorySessionBuilder(
            MavenExecutionRequest mavenExecutionRequest) {
        return wrapper.wrap(
                mavenExecutionRequest.getRootDirectory(),
                defaultFactory.newRepositorySessionBuilder(mavenExecutionRequest));
    }
}
