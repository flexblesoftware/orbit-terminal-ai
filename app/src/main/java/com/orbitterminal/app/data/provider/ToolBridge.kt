package com.orbitterminal.app.data.provider

import com.orbitterminal.app.data.provider.base.ToolDefinition
import com.orbitterminal.app.data.terminal.TerminalController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolBridge @Inject constructor(
    private val terminalController: TerminalController
) {
    val availableTools = listOf(
        ToolDefinition(
            name = "run_command",
            description = """Execute a shell command inside the Orbit Linux environment (Debian arm64 rootfs via proot).
                |Use this to run code, install packages, inspect files, run CLI agents, etc.
                |Output is streamed live to the terminal tab and summarized in chat.
                |Always inspect the environment first before making assumptions.""".trimMargin(),
            parametersJson = """{
                "type": "object",
                "properties": {
                    "command": {
                        "type": "string",
                        "description": "The shell command to execute inside the Linux environment"
                    },
                    "session_id": {
                        "type": "string",
                        "description": "Optional: ID of an existing terminal session to reuse. Omit to create a new session."
                    }
                },
                "required": ["command"]
            }"""
        ),
        ToolDefinition(
            name = "read_file",
            description = "Read the contents of a file inside the Linux environment.",
            parametersJson = """{
                "type": "object",
                "properties": {
                    "path": {
                        "type": "string",
                        "description": "Absolute path to the file inside the rootfs"
                    }
                },
                "required": ["path"]
            }"""
        ),
        ToolDefinition(
            name = "list_directory",
            description = "List files and directories at a given path inside the Linux environment.",
            parametersJson = """{
                "type": "object",
                "properties": {
                    "path": {
                        "type": "string",
                        "description": "Absolute path of the directory to list"
                    }
                },
                "required": ["path"]
            }"""
        )
    )

    suspend fun executeTool(name: String, argumentsJson: String): String {
        return when (name) {
            "run_command" -> {
                val args = parseArgs(argumentsJson)
                val command = args["command"] ?: return "Error: 'command' argument missing"
                val sessionId = args["session_id"]
                terminalController.runCommandAndCapture(command, sessionId)
            }
            "read_file" -> {
                val args = parseArgs(argumentsJson)
                val path = args["path"] ?: return "Error: 'path' argument missing"
                terminalController.runCommandAndCapture("cat \"$path\"", null)
            }
            "list_directory" -> {
                val args = parseArgs(argumentsJson)
                val path = args["path"] ?: return "Error: 'path' argument missing"
                terminalController.runCommandAndCapture("ls -la \"$path\"", null)
            }
            else -> "Unknown tool: $name"
        }
    }

    private fun parseArgs(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val trimmed = json.trim().removePrefix("{").removeSuffix("}")
            val regex = Regex(""""(\w+)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
            regex.findAll(trimmed).forEach { match ->
                result[match.groupValues[1]] = match.groupValues[2]
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
        } catch (_: Exception) {}
        return result
    }
}
