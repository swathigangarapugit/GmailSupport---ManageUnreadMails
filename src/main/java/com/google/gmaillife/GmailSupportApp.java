package com.google.gmaillife;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;

public class GmailLifeSupportApp {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/gmail.labels"
);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    interface LifeSupport {
        String help(String userMessage);
    }

    static class Tools {
        @Tool("Start full Gmail rescue – clean, unsubscribe, summarize life")
        String rescueGmail(String dummy) {
            return MailArchaeologist.scan() + "\n" +
                   UnsubscriberBot.massUnsubscribe() + "\n" +
                   LifeStoryAgent.generateLifeReport() + "\n" +
                   FilterGenie.createSmartFilters() +
                   "\n\nGmail OAuth + Gemini fully authenticated!";
        }
    }

    public static void main(String[] args) throws Exception {
        AutoConfiguredOpenTelemetrySdk.initialize();  // Observability

        // ---- USER OAUTH (opens browser once) ----
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);
        System.out.println("Gmail access granted for: " + credential.getAccessToken().substring(0, 20) + "...");

        // ---- GEMINI (still uses your project ID) ----
        var model = VertexAiGeminiChatModel.builder()
                .project("project-88985c67-16dd-48e2-908")   // ← put your real project ID
                .location("us-central1")
                .modelName("gemini-1.5-pro-002")
                .temperature(0.3)
                .build();

        var assistant = AiServices.builder(LifeSupport.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(30))
                .tools(new Tools())
                .build();

        System.out.println("\nGmail Life Support READY! Type: rescue my gmail");
        Scanner sc = new Scanner(System.in);
        while (true) {
            String input = sc.nextLine();
            if (input.equalsIgnoreCase("exit")) break;
            String response = assistant.help(input);
            System.out.println("\nLifeSupport: " + response + "\n");
        }
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        var in = GmailLifeSupportApp.class.getResourceAsStream("/credentials.json");
        var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
