package com.example.veylora.Service;


import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Thin wrapper over the Exotel REST API (SMS, call-to-flow and status lookups). Nothing here touches the database. */
@Component
public class ExotelClient {

    /** Everything needed to talk to one Exotel account. */
    public record Creds(String sid, String apiKey, String apiToken, String subdomain,
                        String callerId, String smsFrom, String dltEntityId, String defaultAppId) {
        public String smsSender() { return smsFrom != null && !smsFrom.isBlank() ? smsFrom : callerId; }
    }

    /** Status of a sent SMS or placed call, as reported by Exotel. */
    public record Status(String status, String detail, Integer durationSeconds) {}

    /** Wrong account sid / key / token. The campaign must stop instead of burning every recipient's retries. */
    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // ------------------------------------------------------------------ calls

    /** Checks the credentials. Throws AuthException when Exotel rejects them. */
    public void verify(Creds c) {
        call(c, HttpRequest.newBuilder(uri(c, "/Calls.json?PageSize=1")).GET());
    }

    /** Sends one SMS and returns Exotel's SMS sid. */
    public String sendSms(Creds c, String to, String body, String dltTemplateId, String customField) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("From", c.smsSender());
        form.put("To", to);
        form.put("Body", body);
        form.put("SmsType", "transactional");
        if (notBlank(c.dltEntityId())) form.put("DltEntityId", c.dltEntityId());
        if (notBlank(dltTemplateId)) form.put("DltTemplateId", dltTemplateId);
        if (notBlank(customField)) form.put("CustomField", customField);
        JsonNode root = call(c, post(uri(c, "/Sms/send.json"), form));
        String sid = text(root.path("SMSMessage").path("Sid"));
        if (sid == null) throw new IllegalStateException("Exotel accepted the SMS but sent no id back");
        return sid;
    }

    /** Calls the customer and, when they pick up, runs the Exotel app (flow). Returns the call sid. */
    public String connectCall(Creds c, String to, String appId, String customField) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("From", to);
        form.put("CallerId", c.callerId());
        form.put("Url", "http://my.exotel.com/" + c.sid() + "/exoml/start_voice/" + appId);
        form.put("CallType", "trans");
        if (notBlank(customField)) form.put("CustomField", customField);
        JsonNode root = call(c, post(uri(c, "/Calls/connect.json"), form));
        String sid = text(root.path("Call").path("Sid"));
        if (sid == null) throw new IllegalStateException("Exotel accepted the call but sent no id back");
        return sid;
    }

    public Status smsStatus(Creds c, String sid) {
        JsonNode m = call(c, HttpRequest.newBuilder(uri(c, "/SMS/Messages/" + enc(sid) + ".json")).GET()).path("SMSMessage");
        return new Status(lower(text(m.path("Status"))), text(m.path("DetailedStatus")), null);
    }

    public Status callStatus(Creds c, String sid) {
        JsonNode m = call(c, HttpRequest.newBuilder(uri(c, "/Calls/" + enc(sid) + ".json")).GET()).path("Call");
        String conversation = text(m.path("ConversationDuration"));
        Integer seconds = toInt(conversation != null ? conversation : text(m.path("Duration")));
        return new Status(lower(text(m.path("Status"))), null, seconds);
    }

    // ---------------------------------------------------------------- plumbing

    private JsonNode call(Creds c, HttpRequest.Builder builder) {
        String auth = Base64.getEncoder().encodeToString((c.apiKey() + ":" + c.apiToken()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = builder.header("Authorization", "Basic " + auth)
                .header("Accept", "application/json").timeout(Duration.ofSeconds(20)).build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Exotel request was interrupted");
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach Exotel: " + e.getMessage(), e);
        }
        int code = response.statusCode();
        String raw = response.body() == null ? "" : response.body();
        if (code == 401 || code == 403) {
            throw new AuthException("Exotel rejected the account sid, API key or API token (HTTP " + code + ").");
        }
        JsonNode root;
        try {
            root = raw.isBlank() ? mapper.createObjectNode() : mapper.readTree(raw);
        } catch (IOException e) {
            root = mapper.createObjectNode();
        }
        if (code < 200 || code >= 300) {
            String message = text(root.path("RestException").path("Message"));
            if (message == null) message = text(root.path("message"));
            throw new IllegalStateException("Exotel error (HTTP " + code + ")" + (message == null ? "" : ": " + message));
        }
        return root;
    }

    private static HttpRequest.Builder post(URI uri, Map<String, String> form) {
        String body = form.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(Collectors.joining("&"));
        return HttpRequest.newBuilder(uri).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private static URI uri(Creds c, String path) {
        return URI.create("https://" + c.subdomain() + "/v1/Accounts/" + enc(c.sid()) + path);
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String lower(String s) { return s == null ? null : s.toLowerCase(); }
    private static String text(JsonNode n) { return n == null || n.isMissingNode() || n.isNull() || n.asText().isBlank() ? null : n.asText(); }
    private static Integer toInt(String s) {
        try { return s == null ? null : (int) Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }
}
