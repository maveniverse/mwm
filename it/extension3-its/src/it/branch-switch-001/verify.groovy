/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
String buildLogString = buildLog.text
assert buildLogString.contains('[INFO] Using MWM workspace: origin-github.com-maveniverse-mwm-branch-a (cache: USER; install: USER_SCOPED)')
assert buildLogString.contains('[INFO] Using MWM workspace: origin-github.com-maveniverse-mwm-branch-b (cache: USER; install: USER_SCOPED)')
// each branch installs into own workspace, and not into local repository
File localRepo = new File( basedir, '../../it-repo' )
String gav = 'eu/maveniverse/maven/mwm/it/branch-switch-001/mod1/1.0-SNAPSHOT/mod1-1.0-SNAPSHOT.pom'
assert new File( localRepo, 'installed/origin-github.com-maveniverse-mwm-branch-a/' + gav ).isFile()
assert new File( localRepo, 'installed/origin-github.com-maveniverse-mwm-branch-b/' + gav ).isFile()
assert !new File( localRepo, 'eu/maveniverse/maven/mwm/it/branch-switch-001' ).exists()
