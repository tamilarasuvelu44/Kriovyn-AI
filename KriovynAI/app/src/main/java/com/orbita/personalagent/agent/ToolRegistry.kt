package com.orbita.personalagent.agent

class ToolRegistry(tools: List<AgentTool>) {
    private val toolMap: Map<String, AgentTool> = tools.associateBy { it.id }

    fun getTool(toolId: String): AgentTool? = toolMap[toolId]

    fun isAllowed(toolId: String): Boolean = toolMap.containsKey(toolId)

    fun allTools(): List<AgentTool> = toolMap.values.toList()
}
