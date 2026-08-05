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

// extension4 only activates on Maven 4+ (interface relocated from o.a.m.internal to o.a.m.resolver)
if (buildLogString.contains('Apache Maven 4.')) {
    assert buildLogString.contains('[INFO] MWM ')
    // TODO: this below is not on CI
    // assert buildLogString.contains('Using MWM workspace: ')
}
// On Maven 3, extension4 is expected to be inactive — Sisu silently skips it
