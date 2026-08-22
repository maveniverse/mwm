/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Config;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

public class ConfigImpl implements Config {
    static final String CACHED_DIR = "cached-dir";
    static final String INSTALLED_DIR = "installed-dir";
    static final String DISCRIMINATOR_ELEMENTS = "discriminator-elements";
    static final String WORKTREE_JOINED = "worktree-joined";
    static final String LINK_ENFORCED = "link-enforced";
    static final String BUILD_CACHE_SCOPE = "build-cache-scope";
    static final String BUILD_OUTPUT_SCOPE = "build-output-scope";

    private final Map<String, String> config;

    public ConfigImpl(Map<String, String> config) {
        this.config = requireNonNull(config);
    }

    @Override
    public String cachedDir() {
        return config.getOrDefault(CACHED_DIR, Config.super.cachedDir());
    }

    @Override
    public String installedDir() {
        return config.getOrDefault(INSTALLED_DIR, Config.super.installedDir());
    }

    @Override
    public EnumSet<DiscriminatorElements> getDiscriminatorElements() {
        if (config.containsKey(DISCRIMINATOR_ELEMENTS)) {
            EnumSet<DiscriminatorElements> res = EnumSet.noneOf(DiscriminatorElements.class);
            Arrays.stream(config.get(DISCRIMINATOR_ELEMENTS).split(","))
                    .forEach(s -> res.add(DiscriminatorElements.valueOf(s.toUpperCase(Locale.ROOT))));
            return res;
        }
        return Config.super.getDiscriminatorElements();
    }

    @Override
    public boolean isWorktreeJoined() {
        return Boolean.parseBoolean(
                config.getOrDefault(WORKTREE_JOINED, Boolean.toString(Config.super.isWorktreeJoined())));
    }

    @Override
    public boolean isLinkEnforced() {
        return Boolean.parseBoolean(
                config.getOrDefault(LINK_ENFORCED, Boolean.toString(Config.super.isLinkEnforced())));
    }

    @Override
    public Scope getBuildCacheScope() {
        return Scope.valueOf(config.getOrDefault(
                        BUILD_CACHE_SCOPE, Config.super.getBuildCacheScope().name())
                .toUpperCase(Locale.ROOT));
    }

    @Override
    public Scope getBuildOutputScope() {
        return Scope.valueOf(config.getOrDefault(
                        BUILD_OUTPUT_SCOPE, Config.super.getBuildOutputScope().name())
                .toUpperCase(Locale.ROOT));
    }
}
