package com.google.gmaillife;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.tools.Annotations.Schema;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UnsubscriberBot {

    private final Gmail gmail;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public UnsubscriberBot(Gmail gmail) {
        this.gmail = gmail;
    }

    @Schema(name = "analyzeEmailBatch", description = "Analyze unread promotional emails")
    public Map<String, Object> analyzeEmailBatch() throws Exception {

        // Query promotional emails (remove size filter)
        ListMessagesResponse response = gmail.users().messages()
                .list("me")
                .setQ("category:promotions is:unread")
                .execute();

        List<Map<String,Object>> arr = new ArrayList<>();

        if (response.getMessages() != null) {
            for (Message msg : response.getMessages()) {
                Message full = gmail.users().messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .execute();

                Map<String,Object> item = new LinkedHashMap<>();
                item.put("id", msg.getId());
                item.put("subject", getHeader(full, "Subject"));
                item.put("snippet", full.getSnippet());
                arr.add(item);
            }
        }

        // Return as Map (same as searchEmails)
        return Map.of("items", arr);
    }


    @Schema(name = "trashEmail", description = "Trash an email by ID")
    public Map<String,Object> trashEmail(
            @Schema(name = "messageId", description = "ID of the email to trash")
            String messageId
    ) throws Exception {

        gmail.users().messages().trash("me", messageId).execute();

        return Map.of(
                "status", "ok",
                "id", messageId
        );
    }

    @Schema(name = "markAsRead", description = "Mark an email as read")
    public Map<String,Object> markAsRead(
            @Schema(name = "messageId") String messageId
    ) throws Exception {

        ModifyMessageRequest mods = new ModifyMessageRequest()
                .setRemoveLabelIds(List.of("UNREAD"));

        gmail.users().messages().modify("me", messageId, mods).execute();

        return Map.of(
                "status", "ok",
                "id", messageId
        );
    }


    @Schema(name = "archiveEmail", description = "Archive an email by ID")
    public Map<String, Object> archiveEmail(
            @Schema(description = "ID of the email to archive") String id
    ) throws Exception {

        ModifyMessageRequest req = new ModifyMessageRequest()
                .setRemoveLabelIds(List.of("INBOX"));

        gmail.users().messages().modify("me", id, req).execute();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("id", id);

        return result;
    }



    @Schema(name = "searchEmails", description = "Search emails")
    public Map<String, Object> searchEmails(
            @Schema(description = "Gmail search query") String query
    ) throws Exception {

        if (query == null || query.isBlank()) {
            query = "from:me";
        }

        long maxResults = 50; // default internally

        var response = gmail.users().messages().list("me")
                .setQ(query)
                .setMaxResults(maxResults)
                .execute();

        List<Map<String,Object>> arr = new ArrayList<>();

        if (response.getMessages() != null) {
            for (var m : response.getMessages()) {
                var full = gmail.users().messages()
                        .get("me", m.getId())
                        .setFormat("metadata")
                        .execute();

                Map<String,Object> email = new LinkedHashMap<>();
                email.put("id", m.getId());
                email.put("subject", getHeader(full, "Subject"));
                email.put("from", getHeader(full, "From"));
                email.put("date", getHeader(full, "Date"));
                email.put("snippet", full.getSnippet());

                arr.add(email);
            }
        }

        return Map.of("items", arr);
    }



    @Schema(name = "getEmail", description = "Get full email")
    public String getEmail(String messageId) throws Exception {

        Message message = gmail.users().messages().get("me", messageId).execute();

        String body = extractBody(message);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("id", message.getId());
        result.put("subject", getHeader(message,"Subject"));
        result.put("from", getHeader(message,"From"));
        result.put("date", getHeader(message,"Date"));
        result.put("snippet", message.getSnippet());
        result.put("body", body);

        return MAPPER.writeValueAsString(result);
    }


    @Schema(name = "getThread", description = "Get thread")
    public String getThread(String threadId) throws Exception {

        Thread thread = gmail.users().threads().get("me", threadId).execute();
        List<Map<String,Object>> messages = new ArrayList<>();

        for (Message msg : thread.getMessages()) {

            Map<String,Object> one = new LinkedHashMap<>();
            one.put("id", msg.getId());
            one.put("subject", getHeader(msg,"Subject"));
            one.put("from", getHeader(msg,"From"));
            one.put("date", getHeader(msg,"Date"));
            one.put("snippet", msg.getSnippet());
            one.put("body", extractBody(msg));

            messages.add(one);
        }

        return MAPPER.writeValueAsString(Map.of("items", messages));
    }


    private String getHeader(Message message, String name) {
        if (message.getPayload() == null) return "";
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        if (headers == null) return "";
        return headers.stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    private String extractBody(Message message) {
        try {
            return extractParts(message.getPayload());
        } catch (Exception e) {
            return "";
        }
    }

    private String extractParts(MessagePart part) throws Exception {
        if (part == null) return "";

        // If body exists
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] data = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(data);
        }

        // If multipart
        if (part.getParts() != null) {
            StringBuilder sb = new StringBuilder();
            for (MessagePart p : part.getParts()) {
                sb.append(extractParts(p));
            }
            return sb.toString();
        }

        return "";
    }

    @Schema(name = "unsubscribeEmail", description = "Unsubscribe user from a mailing list using message ID")
    public Map<String, Object> unsubscribeEmail(
            @Schema(description = "Gmail message ID") String messageId
    ) throws Exception {

        // 1. Load message
        Message msg = gmail.users().messages()
                .get("me", messageId)
                .setFormat("full")
                .execute();

        // 2. Look for the List-Unsubscribe header
        String header = getHeader(msg, "List-Unsubscribe");
        if (header == null || header.isBlank()) {
            return Map.of(
                    "status", "error",
                    "reason", "no-unsubscribe-link"
            );
        }

        // 3. Parse <mailto:> or <http:> link
        Matcher m = Pattern.compile("<([^>]+)>").matcher(header);
        if (!m.find()) {
            return Map.of("status","error","reason","invalid-header");
        }

        String link = m.group(1);

        // 4. Follow link (only http/https supported here)
        if (link.startsWith("http")) {
            HttpURLConnection conn = (HttpURLConnection) new URL(link).openConnection();
            conn.setRequestMethod("GET");
            conn.getResponseCode();
        }

        return Map.of("status", "ok", "id", messageId, "action", "unsubscribed");
    }




}
