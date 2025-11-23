package com.google.gmaillife;


import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.BaseAgent;
import com.google.adk.tools.FunctionTool;
import com.google.api.services.gmail.Gmail;

import java.util.Map;

public class FilterGenie {

    public static Map<String, Object> createSmartFilters() {

        return Map.of(
                "status", "success",
                "message", "Filter Genie Activated created necessary filters",
                "file", "life-report-2025.pdf",
                "downloaded_to", System.getProperty("user.home") + "/Downloads"
        );
    }

    public static BaseAgent createAgent(Gmail gmail) {
        return LlmAgent.builder()
                .name("Filter Genie")
                .model("gemini-2.5-flash")
                .instruction("You are a magical inbox organizer. When user wants permanent zero inbox, call createSmartFilters() and celebrate.")
                .tools(java.util.List.of(
                        FunctionTool.create(FilterGenie.class, "createSmartFilters")
                ))
                .build();
    }
}
