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
// link: other checkout is detected on its own, current checkout git properties are not reused for it
assert buildLogString.contains('[INFO] Workspace origin-github.com-maveniverse-mwm-my-branch linked with origin-github.com-maveniverse-other-main')
// detect: linked workspace shows up
assert buildLogString.contains('[INFO] Linked workspaces:')
assert buildLogString.contains('[INFO]   WS ID     = origin-github.com-maveniverse-other-main')
// unlink: link is gone
assert buildLogString.contains('[INFO] Workspace origin-github.com-maveniverse-mwm-my-branch unlinked from origin-github.com-maveniverse-other-main')
File wsFile = new File( basedir, '.mvn/target/origin-github.com-maveniverse-mwm-my-branch.properties' )
assert !wsFile.text.contains('workspace.links=')
