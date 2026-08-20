/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.extension39.internal;

import com.google.inject.Key;
import com.google.inject.name.Names;
import eu.maveniverse.maven.mwm.core.Version;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;

/**
 * This module inhibits binding in Maven 3.10+ where Resolver 2.x is used, as the bound
 * class extends internal class that has ctor signature change and would explode at runtime
 * in any Maven 3.10+ version.
 */
@Named
public class InhibitorModule extends com.google.inject.AbstractModule {
    @Override
    protected void configure() {
        if (Version.resolverVersion().startsWith("1.")) {
            bind(Key.get(LocalPathPrefixComposerFactory.class, Names.named("branch-scoped")))
                    .to(BranchScopedLocalPathPrefixComposerFactory.class)
                    .in(Singleton.class);
        }
    }
}
