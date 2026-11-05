def git(File dir, String... args) {
    def cmd = ['git', '-c', 'user.name=ItTester', '-c', 'user.email=mwm@maveniverse.eu'] + args.toList()
    def process = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start()
    process.inputStream.eachLine { println it }
    if (process.waitFor() != 0) {
        throw new Exception("ERROR during the execution of \"" + cmd.join(' ') + "\"")
    }
}

// basedir becomes a linked worktree (branch "wt-branch") of a main checkout (branch "main") living next to it
File main = new File(basedir.parentFile, basedir.name + '-main')
File tmp = new File(basedir.parentFile, basedir.name + '-tmp')
git(basedir, 'init', '-b', 'main')
git(basedir, 'add', '.')
git(basedir, 'commit', '-m', 'initial')
git(basedir, 'remote', 'add', 'origin', 'https://github.com/maveniverse/mwm.git')
main.mkdirs()
assert new File(basedir, '.git').renameTo(new File(main, '.git'))
git(main, 'checkout', '-f')
// worktree add needs a fresh directory: create it elsewhere, then move it over basedir and let git fix the links
git(main, 'worktree', 'add', '-b', 'wt-branch', tmp.absolutePath)
assert new File(tmp, '.git').renameTo(new File(basedir, '.git'))
git(main, 'worktree', 'repair', basedir.absolutePath)
assert tmp.deleteDir()
git(basedir, 'status')
// config present only in main checkout; worktree must find it via commonDir
File mainConfig = new File(main, '.mvn/target/mwm.properties')
mainConfig.parentFile.mkdirs()
mainConfig.text = 'worktree-joined=false\n'
assert !new File(basedir, '.mvn/target/mwm.properties').exists()
