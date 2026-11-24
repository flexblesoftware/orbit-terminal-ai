package com.orbitterminal.app.data.github

import com.orbitterminal.app.data.security.EncryptedKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String,
    val body: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<ReleaseAsset> = emptyList(),
    @SerialName("published_at") val publishedAt: String = ""
)

@Serializable
data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("created_at") val createdAt: String = ""
)

@Singleton
class GitHubClient @Inject constructor(
    private val keyStorage: EncryptedKeyStorage
) {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val apiBase = "https://api.github.com"

    private fun token() = keyStorage.getKey(EncryptedKeyStorage.KEY_GITHUB_TOKEN)

    private fun buildRequest(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
        token()?.let { builder.addHeader("Authorization", "Bearer $it") }
        return builder
    }

    suspend fun getLatestRelease(owner: String, repo: String): GitHubRelease? =
        withContext(Dispatchers.IO) {
            val request = buildRequest("$apiBase/repos/$owner/$repo/releases/latest").get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    json.decodeFromString<GitHubRelease>(body)
                }
            } catch (_: Exception) { null }
        }

    suspend fun getLatestWorkflowRun(owner: String, repo: String, workflowId: String): WorkflowRun? =
        withContext(Dispatchers.IO) {
            val request = buildRequest(
                "$apiBase/repos/$owner/$repo/actions/workflows/$workflowId/runs?per_page=1"
            ).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val runJson = Regex(""""workflow_runs"\s*:\s*\[(\{[^]]*?)(?:,\{|\])""")
                        .find(body)?.groupValues?.get(1)
                    runJson?.let { json.decodeFromString<WorkflowRun>(it) }
                }
            } catch (_: Exception) { null }
        }

    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String = "main"
    ): Boolean = withContext(Dispatchers.IO) {
        val t = token() ?: return@withContext false
        val body = """{"ref":"$ref"}""".toRequestBody("application/json".toMediaType())
        val request = buildRequest(
            "$apiBase/repos/$owner/$repo/actions/workflows/$workflowId/dispatches"
        ).addHeader("Authorization", "Bearer $t").post(body).build()
        try {
            client.newCall(request).execute().use { it.code == 204 }
        } catch (_: Exception) { false }
    }
}
