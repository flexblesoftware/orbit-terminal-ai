package com.orbitterminal.app.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbitterminal.app.data.agent.AgentId
import com.orbitterminal.app.data.agent.AgentManager
import com.orbitterminal.app.data.agent.AgentStatus
import com.orbitterminal.app.data.agent.AntigravityLoginHandler
import com.orbitterminal.app.data.agent.AntigravityLoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val agentManager: AgentManager,
    private val antigravityLoginHandler: AntigravityLoginHandler
) : ViewModel() {

    val agentStatuses: StateFlow<Map<AgentId, AgentStatus>> = agentManager.agentStatuses
    val antigravityLoginState: StateFlow<AntigravityLoginState> = antigravityLoginHandler.loginState

    fun refreshStatuses() {
        viewModelScope.launch {
            agentManager.refreshAllStatuses()
        }
    }

    fun installAgent(agentId: AgentId) {
        viewModelScope.launch {
            agentManager.installAgent(agentId).collect {}
        }
    }

    fun launchAgent(agentId: AgentId) {
        viewModelScope.launch {
            agentManager.launchAgentSession(agentId)
        }
    }

    fun startAntigravityLogin() {
        viewModelScope.launch {
            antigravityLoginHandler.startLoginFlow()
        }
    }

    fun submitAntigravityToken(token: String) {
        viewModelScope.launch {
            antigravityLoginHandler.submitAuthToken(token)
            agentManager.checkAgentStatus(AgentId.ANTIGRAVITY)
        }
    }
}
