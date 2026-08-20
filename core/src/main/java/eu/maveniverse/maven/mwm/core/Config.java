/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

/**
 * Maven Workspace Manager configuration.
 */
public interface Config {
    /**
     * Project wide, user wide.
     */
    enum Scope {
        PROJECT,
        USER
    }

    /**
     * In case of git worktree, is current checkout joined with "default" workspace or not. If development happens on
     * single "main" branch, and feature branches are used, this should be {@code true}. As a counter example, in
     * case of Maven development, where you have "unrelated" branches like {@code maven-3.9.x} and {@code maven-3.10.x}
     * etc., feature branches are created against these, and not against {@code main} or {@code master}.
     */
    default boolean isWorktreeJoined() {
        return false;
    }

    /**
     * The scope of build cache.
     */
    default Scope getBuildCacheScope() {
        return Scope.USER;
    }

    /**
     * The scope of build output.
     */
    default Scope getBuildOutputScope() {
        return Scope.PROJECT;
    }
}
