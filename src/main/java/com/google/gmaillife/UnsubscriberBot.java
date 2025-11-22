package com.google.gmaillife;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import com.google.gmaillife.model.EmailFull;
import com.google.gmaillife.model.EmailSummary;

import java.util.*;

public class UnsubscriberBot {
    private final Gmail gmail;

    public UnsubscriberBot(Gmail gmail) {
        this.gmail = gmail;
    }

    // Your existing methods (add @Schema to fix schema generation)
    @Schema(name = "analyzeEmailBatch", description = "Analyze batch of unread promotional emails")
    public List<EmailAction> analyzeEmailBatch(@Schema(description = "Max results to fetch, default 50") Long maxResults) throws Exception {
        if (maxResults == null) maxResults = 50L;
        ListMessagesResponse response = gmail.users().messages()
                .list("me")
                .setQ("is:unread category:promotions larger:1M")
                .setMaxResults(maxResults)
                .execute();

        List<EmailAction> results = new ArrayList<>();
        if (response.getMessages() == null) return results;

        for (Message msg : response.getMessages()) {
            Message full = gmail.users().messages().get("me", msg.getId())
                    .setFormat("metadata")
                    .execute();

            String subject = getHeader(full, "Subject");
            String snippet = full.getSnippet() != null ? full.getSnippet() : "";
            results.add(new EmailAction(msg.getId(), subject, snippet));
        }
        return results;
    }

    @Schema(name = "trashEmail", description = "Trash an email by ID")
    public String trashEmail(@Schema(description = "Message ID to trash") String messageId) throws Exception {
        if (messageId == null) throw new IllegalArgumentException("messageId required");
        gmail.users().messages().trash("me", messageId).execute();
        return "Trashed email: " + messageId;
    }

    @Schema(name = "archiveEmail", description = "Archive an email by ID")
    public String archiveEmail(@Schema(description = "Message ID to archive") String messageId) throws Exception {
        if (messageId == null) throw new IllegalArgumentException("messageId required");
        ModifyMessageRequest req = new ModifyMessageRequest().setRemoveLabelIds(List.of("INBOX"));
        gmail.users().messages().modify("me", messageId, req).execute();
        return "Archived email: " + messageId;
    }



    // Add this once in your class
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Schema(name = "searchEmails", description = "Search emails. Returns JSON array of email summaries.")
    public String searchEmails(
            @Schema(description = "Gmail search query") String query,
            @Schema(description = "Max results") Integer maxResults) throws Exception {

        if (query == null || query.isBlank()) query = "from:me";
        if (maxResults == null || maxResults <= 0) maxResults = 50;

        var response = gmail.users().messages().list("me")
                .setQ(query)
                .setMaxResults(maxResults.longValue())
                .execute();

        List<Map<String, Object>> emails = new ArrayList<>();
        if (response.getMessages() != null) {
            for (var m : response.getMessages()) {
                var full = gmail.users().messages().get("me", m.getId())
                        .setFormat("metadata")
                        .execute();

                Map<String, Object> email = new LinkedHashMap<>();
                email.put("id", m.getId());
                email.put("subject", getHeader(full, "Subject"));
                email.put("from", getHeader(full, "From"));
                email.put("date", getHeader(full, "Date"));
                email.put("snippet", full.getSnippet() != null ? full.getSnippet() : "");
                emails.add(email);
            }
        }

        // This is the ONLY thing ADK 0.3.0 accepts without crashing
        try {
            return MAPPER.writeValueAsString(emails);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Schema(name = "getEmail", description = "Get full email by ID. Returns JSON string.")
    public String getEmail(@Schema(description = "Message ID") String messageId) throws Exception {
        if (messageId == null) throw new IllegalArgumentException("messageId required");

        var message = gmail.users().messages().get("me", messageId).execute();

        String body = "";
        if (message.getPayload() != null && message.getPayload().getParts() != null) {
            for (var part : message.getPayload().getParts()) {
                if ("text/plain".equals(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
                    body = new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
                    break;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", message.getId());
        result.put("subject", getHeader(message, "Subject"));
        result.put("from", getHeader(message, "From"));
        result.put("date", getHeader(message, "Date"));
        result.put("body", body);
        result.put("snippet", message.getSnippet() != null ? message.getSnippet() : "");

        try {
            return MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Schema(name = "getThread", description = "Get thread by ID. Returns JSON array.")
    public String getThread(@Schema(description = "Thread ID") String threadId) throws Exception {
        if (threadId == null) return "[]";

        var thread = gmail.users().threads().get("me", threadId).execute();
        List<String> messages = new ArrayList<>();
        for (var msg : thread.getMessages()) {
            messages.add(getEmail(msg.getId()));  // getEmail already returns JSON string
        }

        try {
            return MAPPER.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Helper (add if missing)
    private String getHeader(Message message, String name) {
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        if (headers == null) return "";
        return headers.stream()
                .filter(h -> name.equals(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    // Your existing EmailAction class
    public static class EmailAction {
        public String id, subject, snippet;
        public EmailAction(String id, String subject, String snippet) {
            this.id = id; this.subject = subject; this.snippet = snippet;
        }
    }
}