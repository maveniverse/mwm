/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
def git(File dir, String... args) {
    def cmd = ['git', '-c', 'user.name=ItTester', '-c', 'user.email=mwm@maveniverse.eu'] + args.toList()
    def process = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start()
    process.inputStream.eachLine { println it }
    if (process.waitFor() != 0) {
        throw new Exception("ERROR during the execution of \"" + cmd.join(' ') + "\"")
    }
}
git(basedir, 'init', '-b', 'my-branch')
git(basedir, 'add', '.')
git(basedir, 'commit', '-m', 'initial')
git(basedir, 'remote', 'add', 'origin', 'https://github.com/maveniverse/mwm.git')
// other checkout, with different remote and branch
File other = new File(basedir.parentFile, basedir.name + '-other')
other.mkdirs()
new File(other, 'README').text = 'other'
git(other, 'init', '-b', 'main')
git(other, 'add', '.')
git(other, 'commit', '-m', 'initial')
git(other, 'remote', 'add', 'origin', 'https://github.com/maveniverse/other.git')
