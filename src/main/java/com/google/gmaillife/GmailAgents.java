package com.google.gmaillife;

import com.google.adk.agents.*;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.GoogleSearchTool;
import com.google.api.services.gmail.Gmail;
import java.util.List;

public class GmailAgents {

    Gmail gmail;
    public GmailAgents(Gmail gmail) {
        this.gmail = gmail;
    }

    // Helper to inject Gmail at runtime (used in main)
    public static BaseAgent withGmail(Gmail gmail) {
        // Don't reuse the static ROOT_AGENT — create a fresh one with real Gmail
        UnsubscriberBot tools = new UnsubscriberBot(gmail);

        LlmAgent analyzer = LlmAgent.builder()
                .name("analyzer")
                .model("gemini-2.5-flash")
                .instruction("""
You analyze unread promotional emails.
Call the tool analyzeEmailBatch when cleaning or finding unread/promo emails.
""")
                .tools(List.of(FunctionTool.create(tools, "analyzeEmailBatch")))
                .build();

        LlmAgent decider = LlmAgent.builder()
                .name("decider")
                .model("gemini-2.5-flash")
                .instruction("""
                Input: JSON array of emails from analyzer.

                Output: JSON array:
                [
                  {"id":"123", "action":"trash"},
                  {"id":"456", "action":"archive"}
                ]
                """)
                .build();

        LlmAgent actor = LlmAgent.builder()
                .name("actor")
                .model("gemini-2.5-flash")
                .instruction("""
                You execute email cleanup actions.

                For each entry:
                - If action == "trash": call trashEmail(id)
                - If action == "archive": call archiveEmail(id)

                Always call the proper tool.
                """)
                .tools(List.of(
                        FunctionTool.create(tools, "trashEmail"),
                        FunctionTool.create(tools, "archiveEmail")
                ))
                .build();

        LlmAgent lifeStory = LlmAgent.builder()
                .name("lifeStory")
                .model("gemini-2.5-flash")
                .instruction("""
                You write life stories using emails.
                ALWAYS call searchEmails, getEmail, or getThread.
                """)
                .tools(List.of(
                        FunctionTool.create(tools, "searchEmails"),
                        FunctionTool.create(tools, "getEmail"),
                        FunctionTool.create(tools, "getThread")
                ))
                .build();

        LlmAgent scanInbox = LlmAgent.builder()
                .name("scanInbox")
                .model("gemini-2.5-flash")
                .instruction("""
                        You are scanning your mail box now.
                        Use scanMailbox.
                        Call tools when needed.
                """)
                .tools(List.of(
                        FunctionTool.create(tools, "searchEmails"),
                        FunctionTool.create(tools, "getEmail"),
                        FunctionTool.create(tools, "scanMailbox")
                ))
                .build();

        LlmAgent unSubscribe = LlmAgent.builder()
                .name("unSubscribe")
                .model("gemini-2.5-flash")
                .instruction("""
        You help users unsubscribe from emails.

        WHEN USER EXPRESSES AN UNSUBSCRIBE INTENT:
        - If the user says "unsubscribe from ___" or similar AND you have NOT yet
          used any tool in this conversation turn:
              → Call searchEmails immediately.

        STRICT WORKFLOW:
        1. FIRST tool call (only once):
              → searchEmails(query)

        2. SECOND tool call (only once):
              After searchEmails returns:
                  - If a messageId exists → call unsubscribeEmail(messageId)
                  - If NO message found → respond normally (no tools)

        TOOL RESULT HANDLING:
        - If the LAST message you see is a tool RESULT, it is NOT a new user request.
        - Do NOT call searchEmails again.
        - Do NOT call unsubscribeEmail again unless it is the single allowed follow-up.

        STATE TRACKING (IMPORTANT):
        You must track your own state during this conversation turn:
        - If you already called searchEmails, NEVER call it again.
        - If unsubscribeEmail was already called, NEVER call any tool again.
        - After ANY tool call, only one more tool call is allowed (unsubscribeEmail).

        LOOP PREVENTION:
        - Never repeat any tool.
        - Never chain tools beyond searchEmails → unsubscribeEmail.
        - If unsure, respond normally.
    """)
                .tools(List.of(
                        FunctionTool.create(tools, "searchEmails"),
                        FunctionTool.create(tools, "unsubscribeEmail")
                ))
                .build();







        return SequentialAgent.builder()
                .name("Gmail Life Support")
                .subAgents(List.of(
                        analyzer,
                        decider,
                        actor,
                       lifeStory,
                        scanInbox,
                        unSubscribe,
                        FilterGenie.createAgent(gmail)
                ))
                .build();
    }


}