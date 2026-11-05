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

// worktree: AUTO build output scope resolves to PROJECT
assert buildLogString.contains('[INFO] Using MWM workspace: origin-github.com-maveniverse-mwm-wt-branch (cache: USER; install: PROJECT)')

// so installed artifacts land in the worktree, not in the local repository
File installed = new File( basedir, '.mvn/target/installed/eu/maveniverse/maven/mwm/it/worktree-001/mod1/1.0-SNAPSHOT/mod1-1.0-SNAPSHOT.pom' )
assert installed.isFile()
File localRepo = new File( basedir, '../../it-repo/eu/maveniverse/maven/mwm/it/worktree-001' )
assert !localRepo.exists()
