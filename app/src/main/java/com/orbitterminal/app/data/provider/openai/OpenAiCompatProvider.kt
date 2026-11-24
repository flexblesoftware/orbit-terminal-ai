package com.orbitterminal.app.data.provider.openai

import com.orbitterminal.app.data.provider.base.*
import com.orbitterminal.app.data.provider.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader

class OpenAiCompatProvider(
    override val providerId: String,
    private val apiKey: String,
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val json: Json,
    private val extraHeaders: Map<String, String> = emptyMap()
) : AiProvider {

    override fun streamChat(
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        tools: List<ToolDefinition>?
    ): Flow<StreamEvent> = flow {
        val msgArray = buildJsonArray {
            systemPrompt?.let {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", it)
                })
            }
            for (msg in messages) {
                add(buildJsonObject {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        val body = buildJsonObject {
            put("model", modelId)
            put("messages", msgArray)
            put("stream", true)
            put("temperature", 0.7)
            put("max_tokens", 8192)
            if (!tools.isNullOrEmpty()) {
                put("tools", buildJsonArray {
                    for (tool in tools) {
                        add(buildJsonObject {
                            put("type", "function")
                            put("function", buildJsonObject {
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", json.parseToJsonElement(tool.parametersJson))
                            })
                        })
                    }
                })
                put("tool_choice", "auto")
            }
        }

        val requestBuilder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))

        for ((k, v) in extraHeaders) requestBuilder.addHeader(k, v)

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw ProviderException(
                    parseError(errorBody),
                    response.code,
                    isRateLimit = response.code == 429,
                    isAuthError = response.code == 401
                )
            }

            val toolCallAccumulators = mutableMapOf<Int, StringBuilder>()
            val toolCallNames = mutableMapOf<Int, String>()
            val toolCallIds = mutableMapOf<Int, String>()

            val reader = BufferedReader(response.body!!.charStream())
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data: ")) continue
                val data = l.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val obj = json.parseToJsonElement(data).jsonObject
                    val choice = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: continue
                    val delta = choice["delta"]?.jsonObject ?: continue

                    val text = delta["content"]?.jsonPrimitive?.contentOrNull
                    if (!text.isNullOrEmpty()) emit(StreamEvent.TextChunk(text))

                    delta["tool_calls"]?.jsonArray?.forEach { tc ->
                        val tcObj = tc.jsonObject
                        val idx = tcObj["index"]?.jsonPrimitive?.int ?: 0
                        tcObj["id"]?.jsonPrimitive?.contentOrNull?.let { toolCallIds[idx] = it }
                        tcObj["function"]?.jsonObject?.let { fn ->
                            fn["name"]?.jsonPrimitive?.contentOrNull?.let { toolCallNames[idx] = it }
                            fn["arguments"]?.jsonPrimitive?.contentOrNull?.let { args ->
                                toolCallAccumulators.getOrPut(idx) { StringBuilder() }.append(args)
                            }
                        }
                    }

                    val finishReason = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
                    if (finishReason == "tool_calls") {
                        for ((idx, argBuilder) in toolCallAccumulators) {
                            emit(StreamEvent.ToolCall(
                                id = toolCallIds[idx] ?: "tc_$idx",
                                name = toolCallNames[idx] ?: "",
                                arguments = argBuilder.toString()
                            ))
                        }
                    }
                } catch (_: Exception) {}
            }
            emit(StreamEvent.Done)
        }
    }

    private fun parseError(body: String): String = try {
        val obj = Json.parseToJsonElement(body).jsonObject
        obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "API error ($providerId)"
    } catch (_: Exception) { "API error: $body" }
}
