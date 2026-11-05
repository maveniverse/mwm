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

// "/" is not path safe: replaced, and hash of original branch name appended to keep "feature/x" and "feature_x" distinct
assert buildLogString.contains('[INFO] WS ID     = origin-github.com-maveniverse-mwm-feature_x-e6ef9b9f')
assert buildLogString.contains('[INFO] git.branchName = feature/x')
