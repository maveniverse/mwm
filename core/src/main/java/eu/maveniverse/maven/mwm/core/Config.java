/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.util.EnumSet;

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
     * In case of git worktree, is current branch tailed with default branch workspace or not.
     * <p>
     * If development happens on single "main" branch, and feature branches are used, this should be {@code true}.
     * As a counter example, in case of Maven development, where you have "unrelated" branches like
     * {@code maven-3.9.x} and {@code maven-3.10.x}, feature branches are created against these, and this
     * should be {@code false}.
     */
    default boolean isWorktreeJoined() {
        return true;
    }

    /**
     * Discriminator elements.
     */
    enum DiscriminatorElements {
        REMOTE_NAME,
        HOST_NAME,
        OWNER_NAME,
        REPOSITORY_NAME,
        BRANCH_NAME
    }

    /**
     * Returns the elements creating workspace discriminator.
     */
    default EnumSet<DiscriminatorElements> getDiscriminatorElements() {
        return EnumSet.allOf(DiscriminatorElements.class);
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
