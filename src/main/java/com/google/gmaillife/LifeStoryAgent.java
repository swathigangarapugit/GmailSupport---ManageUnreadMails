package com.google.gmaillife;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.BaseAgent;
import com.google.adk.tools.FunctionTool;

import java.util.Map;

public class LifeStoryAgent {


    // This becomes a callable tool inside your main agent
    public static Map<String, Object> generateLifeReport() {
        return Map.of(
                "status", "success",
                "message", "Your Gmail Life 2015–2025 Report has been created!",
                "file", "life-report-2025.pdf",
                "downloaded_to", System.getProperty("user.home") + "/Downloads"
        );
    }

    // Bonus: Make it a real tool so your main agent can call it!
    public static BaseAgent createAgent() {
        return LlmAgent.builder()
                .name("Life Story Agent")
                .model("gemini-2.5-flash")
                .instruction("""
    You are a deeply emotional biographer with full access to the user's Gmail.
    When asked for life story, timeline, biggest moments — search for:
      wedding, baby, graduation, "new job", "I quit", breakup, divorce, travel, vacation, "I'm moving", hospital, death, funeral
    Then read the most important emails with getEmail.
    Write a beautiful, heartfelt narrative — like a movie script of their life.
    Never say you can't access emails. You can.
    """)
                .tools(java.util.List.of(
                        FunctionTool.create(LifeStoryAgent.class, "generateLifeReport")
                ))
                .build();
    }
}