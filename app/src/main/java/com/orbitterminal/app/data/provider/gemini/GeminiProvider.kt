package com.orbitterminal.app.data.provider.gemini

import com.orbitterminal.app.data.provider.base.*
import com.orbitterminal.app.data.provider.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader

class GeminiProvider(
    private val apiKey: String,
    private val client: OkHttpClient,
    private val json: Json
) : AiProvider {
    override val providerId = "gemini"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    override fun streamChat(
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        tools: List<ToolDefinition>?
    ): Flow<StreamEvent> = flow {
        val contents = messages.map { msg ->
            buildJsonObject {
                put("role", if (msg.role == "user") "user" else "model")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", msg.content) })
                })
            }
        }

        val requestBody = buildJsonObject {
            put("contents", JsonArray(contents))
            systemPrompt?.let {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", it) })
                    })
                })
            }
            put("generationConfig", buildJsonObject {
                put("temperature", 0.7)
                put("maxOutputTokens", 8192)
            })
            if (!tools.isNullOrEmpty()) {
                put("tools", buildJsonArray {
                    add(buildJsonObject {
                        put("functionDeclarations", buildJsonArray {
                            for (t in tools) {
                                add(buildJsonObject {
                                    put("name", t.name)
                                    put("description", t.description)
                                    put("parameters", json.parseToJsonElement(t.parametersJson))
                                })
                            }
                        })
                    })
                })
            }
        }

        val url = "$baseUrl/$modelId:streamGenerateContent?alt=sse&key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw ProviderException(
                    parseGeminiError(errorBody),
                    response.code,
                    isRateLimit = response.code == 429,
                    isAuthError = response.code == 401 || response.code == 403
                )
            }

            val reader = BufferedReader(response.body!!.charStream())
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]" || data.isEmpty()) continue
                    try {
                        val obj = json.parseToJsonElement(data).jsonObject
                        val candidates = obj["candidates"]?.jsonArray ?: continue
                        val parts = candidates.firstOrNull()?.jsonObject
                            ?.get("content")?.jsonObject
                            ?.get("parts")?.jsonArray ?: continue
                        for (part in parts) {
                            val partObj = part.jsonObject
                            val text = partObj["text"]?.jsonPrimitive?.contentOrNull
                            if (!text.isNullOrEmpty()) {
                                emit(StreamEvent.TextChunk(text))
                            }
                            partObj["functionCall"]?.jsonObject?.let { fnCall ->
                                val name = fnCall["name"]?.jsonPrimitive?.content ?: ""
                                val args = fnCall["args"]?.jsonObject?.toString() ?: "{}"
                                emit(StreamEvent.ToolCall("gemini_call", name, args))
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            emit(StreamEvent.Done)
        }
    }

    private fun parseGeminiError(body: String): String = try {
        val obj = Json.parseToJsonElement(body).jsonObject
        obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Gemini API error"
    } catch (_: Exception) { "Gemini API error: $body" }
}
