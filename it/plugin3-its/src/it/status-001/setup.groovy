def exec(String cmd){
    def process = new ProcessBuilder(cmd.tokenize(" "))
            .directory(basedir)
            .redirectErrorStream(true)
            .start()
    process.inputStream.eachLine {println it}
    process.waitFor();

    if( process.exitValue() != 0){
        throw new Exception("ERROR during the execution of \"" + cmd + "\"")
    }
}

println basedir.list()
exec('git init -b my-branch')
exec('git add .')
exec('git commit -m initial')
exec('git remote add origin https://github.com/maveniverse/mwm.git')
