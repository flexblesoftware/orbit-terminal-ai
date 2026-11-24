# 🚀 Orbit Terminal AI

[![Build and Release Signed APK](https://github.com/flexblesoftware/orbit-terminal-ai/actions/workflows/build-apk.yml/badge.svg)](https://github.com/flexblesoftware/orbit-terminal-ai/actions/workflows/build-apk.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Target ABI](https://img.shields.io/badge/Architecture-arm64--v8a-brightgreen.svg)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange.svg)]()

**Orbit Terminal AI** is a production-ready Android application combining a full-featured multi-provider AI chatbot, a complete native Debian arm64 Linux environment (powered by PRoot without root privileges), and fully integrated autonomous CLI agents (Google Antigravity CLI, Anthropic Claude Code, OpenAI Codex CLI).

---

## 🌟 Key Features

### 1. 🤖 AI Chatbot
- **Providers Supported**: Google Gemini, Groq, OpenRouter, and OpenAI.
- **Model Switch On-the-Fly**: Switch provider or model mid-conversation without context loss.
- **Real-Time Streaming**: Word-by-word streaming with typing indicators and Stop control.
- **Autonomous Tool Calling**: AI automatically runs terminal commands inside the Linux environment via `run_command`, inspects output, fixes errors, and reports results.
- **Persistent Storage**: All chat history and messages indexed locally in Room database with full-text search.

### 2. 🐧 Built-in Linux Environment
- **Distribution**: Debian GNU/Linux (arm64 root filesystem).
- **Zero Root Required**: Runs safely inside app-private storage using `proot`.
- **Pre-installed Runtimes**: Node.js (LTS), npm, Python 3, pip, git, curl, wget, gh (GitHub CLI), vim, nano.
- **Storage Resilient**: Downloads ~350 MB archive with resume support, SHA-256 verification, and automatic Wi-Fi detection.

### 3. ⚡ Integrated CLI Agents
- **Antigravity CLI** (Google): Official browser OAuth flow with token capture and automatic status verification.
- **Claude Code** (Anthropic): Automated npm setup and interactive terminal sessions.
- **OpenAI Codex CLI**: Direct installation and terminal execution.

### 4. 🔄 In-App Updates & GitHub CI/CD
- Built-in GitHub Releases update checker with Android Download Manager integration.
- Remote workflow triggering (`workflow_dispatch`) directly from the Settings/GitHub screen.

---

## 🛠️ Tech Stack
- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose, Material 3, Material You Dynamic Theming
- **Architecture**: Clean Architecture + MVVM + StateFlow
- **Dependency Injection**: Dagger Hilt 2.53.1
- **Database**: Room 2.6.1
- **Security**: Android Keystore & EncryptedSharedPreferences (AES-256-GCM)
- **CI/CD**: GitHub Actions building signed release APKs (`arm64-v8a` only)

---

## 🔑 GitHub Actions Secrets Configuration
To build signed production APKs via GitHub Actions, add these secrets to your repository settings (`Settings -> Secrets and variables -> Actions`):

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release `.jks` or `.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](LICENSE) for details.
