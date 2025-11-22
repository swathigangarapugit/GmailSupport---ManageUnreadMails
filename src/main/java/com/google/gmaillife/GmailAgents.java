package com.google.gmaillife;

import com.google.adk.agents.*;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.GoogleSearchTool;
import com.google.api.services.gmail.Gmail;
import java.util.List;

public class GmailAgents {

    // This is the only agent the Dev UI sees
    public static final BaseAgent ROOT_AGENT = createRootAgent();

    private static BaseAgent createRootAgent() {
        // Create tool instance — Gmail will be injected later via runner
        UnsubscriberBot tools = new UnsubscriberBot(null);

        LlmAgent analyzer = LlmAgent.builder()
                .name("analyzer")
                .model("gemini-2.5-flash")
                .instruction("You are an email analyst. Use analyze_email_batch to find unread promotional emails.")
                .tools(List.of(
                        FunctionTool.create(tools, "analyzeEmailBatch")
                ))
                .build();

        LlmAgent decider = LlmAgent.builder()
                .name("decider")
                .model("gemini-2.5-flash")
                .instruction("For each email, decide: trash, archive, or keep. Output JSON.")
                .build();

        LlmAgent actor = LlmAgent.builder()
                .name("actor")
                .model("gemini-2.5-flash")
                .instruction("Execute the decided actions using trash_email or archive_email tools.")
                .tools(List.of(
                        FunctionTool.create(tools, "trashEmail"),
                        FunctionTool.create(tools, "archiveEmail")
                ))
                .build();

        return SequentialAgent.builder()
                .name("Gmail Life Support")
                .subAgents(List.of(analyzer, decider, actor,
                        LifeStoryAgent.createAgent(),    // NEW
                        FilterGenie.createAgent()))

                .build();
    }

    // Helper to inject Gmail at runtime (used in main)
    public static BaseAgent withGmail(Gmail gmail) {
        // Don't reuse the static ROOT_AGENT — create a fresh one with real Gmail
        UnsubscriberBot tools = new UnsubscriberBot(gmail);

        LlmAgent analyzer = LlmAgent.builder()
                .name("analyzer")
                .model("gemini-2.5-flash")
                .instruction("You are an email analyst. Use analyzeEmailBatch to find unread promotional emails.")
                .tools(List.of(FunctionTool.create(tools, "analyzeEmailBatch")))
                .build();

        LlmAgent decider = LlmAgent.builder()
                .name("decider")
                .model("gemini-2.5-flash")
                .instruction("For each email, decide: trash, archive, or keep. Output JSON.")
                .build();

        LlmAgent actor = LlmAgent.builder()
                .name("actor")
                .model("gemini-2.5-flash")
                .instruction("Execute the decided actions using trashEmail or archiveEmail tools.")
                .tools(List.of(
                        FunctionTool.create(tools, "trashEmail"),
                        FunctionTool.create(tools, "archiveEmail")
                ))
                .build();

        LlmAgent lifeStory = LlmAgent.builder()
                .name("lifeStory")
                .model("gemini-2.5-flash")
                .instruction("... your instruction ...")
                .tools(List.of(
                FunctionTool.create(tools, "searchEmails"),
                FunctionTool.create(tools, "getEmail"),
                FunctionTool.create(tools, "getThread")
        ))
                .build();

        return SequentialAgent.builder()
                .name("Gmail Life Support")
                .subAgents(List.of(
                        analyzer,
                        decider,
                        actor,
                        LifeStoryAgent.createAgent(),     // your custom agent
                        FilterGenie.createAgent()        // your custom agent
                ))
                .build();
    }
}