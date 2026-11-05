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

assert buildLogString.contains('[INFO] MWM workspace detected')
assert buildLogString.contains('[INFO] WS ID     = origin-github.com-maveniverse-mwm-wt-branch')
assert buildLogString.contains('git.commonDir = ')
// worktree-joined=false: main checkout workspace is not linked
assert !buildLogString.contains('Linked workspaces:')
