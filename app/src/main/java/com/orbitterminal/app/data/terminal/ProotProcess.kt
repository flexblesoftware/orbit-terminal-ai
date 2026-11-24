package com.orbitterminal.app.data.terminal

import java.io.File

class ProotProcess(
    private val prootBinary: File,
    private val rootfsDir: File,
    private val homeDir: File,
    private val workingDir: String = "/root",
    private val initialCommand: List<String> = listOf("/bin/bash", "-l")
) {
    private var process: Process? = null

    fun start(): Process {
        val command = mutableListOf<String>()
        command.add(prootBinary.absolutePath)
        command.add("-0")
        command.add("-r")
        command.add(rootfsDir.absolutePath)
        command.add("-b")
        command.add("/dev")
        command.add("-b")
        command.add("/proc")
        command.add("-b")
        command.add("/sys")
        command.add("-b")
        command.add("${homeDir.absolutePath}:/root")
        command.add("-w")
        command.add(workingDir)
        command.addAll(initialCommand)

        val pb = ProcessBuilder(command)
        pb.directory(homeDir)
        pb.environment().apply {
            put("TERM", "xterm-256color")
            put("HOME", "/root")
            put("USER", "root")
            put("LOGNAME", "root")
            put("SHELL", "/bin/bash")
            put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
            put("LANG", "C.UTF-8")
            put("LC_ALL", "C.UTF-8")
        }
        val proc = pb.start()
        this.process = proc
        return proc
    }

    fun isAlive(): Boolean = process?.isAlive ?: false

    fun destroy() {
        process?.destroyForcibly()
    }
}
