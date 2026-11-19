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

// no worktree: AUTO build output scope resolves to USER_SCOPED (per workspace, so per branch)
assert buildLogString.contains('[INFO] Using MWM workspace: origin-github.com-maveniverse-mwm-my-branch (cache: USER; install: USER_SCOPED)')
