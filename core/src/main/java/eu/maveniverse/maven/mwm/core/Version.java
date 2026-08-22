/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import eu.maveniverse.maven.shared.core.maven.MavenUtils;

public final class Version {
    public static final String UNKNOWN = "unknown";

    private Version() {}

    public static String version() {
        return MavenUtils.discoverArtifactVersion(
                Version.class.getClassLoader(), "eu.maveniverse.maven.mwm", "core", UNKNOWN);
    }
}
