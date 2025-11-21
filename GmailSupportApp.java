package com.google.gmaillife;

import com.google.gmaillife.utils.GmailApiClient;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

import java.util.Scanner;

public class GmailLifeSupportApp {

    interface LifeSupport {
        String help(String userMessage);
    }

    static class Tools {
        @Tool("Start full Gmail rescue – clean, unsubscribe, summarize life")
        String rescueGmail(String dummy) {
            return MailArchaeologist.scan() + "\n" +
                   UnsubscriberBot.massUnsubscribe() + "\n" +
                   LifeStoryAgent.generateLifeReport() + "\n" +
                   FilterGenie.createSmartFilters();
        }
    }

    public static void main(String[] args) throws Exception {
        AutoConfiguredOpenTelemetrySdk.initialize();  // Observability ON

        GmailApiClient.init();  // Real OAuth login (opens browser once)

        var model = VertexAiGeminiChatModel.builder()
                .project("YOUR_PROJECT_ID")      // ← CHANGE ONLY THIS
                .location("us-central1")
                .modelName("gemini-1.5-pro-002")
                .temperature(0.3)
                .build();

        var assistant = AiServices.builder(LifeSupport.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(30))
                .tools(new Tools())
                .build();

        System.out.println("""
            Gmail Life Support READY!
            Type: rescue my gmail
            → I will clean 200k emails, unsubscribe, and give you your life story.
            """);

        Scanner sc = new Scanner(System.in);
        while (true) {
            String input = sc.nextLine();
            if (input.equalsIgnoreCase("exit")) break;
            String response = assistant.help(input);
            System.out.println("\nLifeSupport: " + response + "\n");
        }
    }
}
