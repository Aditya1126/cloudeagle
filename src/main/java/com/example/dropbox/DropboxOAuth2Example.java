package com.example.dropbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class DropboxOAuth2Example {
    private static final String CLIENT_ID = "1j4odo5869wr2pg";
    private static final String REDIRECT_URI = "http://127.0.0.1:53682/callback"; // must be registered

    private static final String AUTH_URL  = "https://www.dropbox.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";

    public static void main(String[] args) throws Exception {
        // 1) Start local HTTP server to capture OAuth redirect
        BlockingQueue<Map<String,String>> authResult = new ArrayBlockingQueue<>(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 53682), 0);
        server.createContext("/callback", new CallbackHandler(authResult));
        server.setExecutor(null);
        server.start();
        System.out.println("Listening on " + REDIRECT_URI);

        // 2) PKCE + state
        String state = randomUrlSafe(32);
        String codeVerifier = randomUrlSafe(64);
        String codeChallenge = base64UrlNoPad(sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII)));

        // 3) Build authorize URL
        String scopesRaw = "team_info.read members.read"; // include members.read for listing team members
        String authorizeUrl = AUTH_URL + "?"
                + "response_type=code"
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(scopesRaw, StandardCharsets.UTF_8)
                + "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
                + "&code_challenge_method=S256"
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
                + "&token_access_type=offline"; // remove if you don't need refresh_token

        // 4) Open the browser
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI.create(authorizeUrl));
        } else {
            System.out.println("Open this URL to authorize:\n" + authorizeUrl);
        }

        // 5) Wait for redirect and validate state
        Map<String,String> params = authResult.take();
        server.stop(0);

        String returnedState = params.get("state");
        if (!Objects.equals(state, returnedState)) {
            throw new IllegalStateException("State mismatch");
        }
        String code = params.get("code");
        if (code == null) {
            throw new IllegalStateException("Missing authorization code");
        }

        // 6) Exchange code for tokens (PKCE: no client_secret)
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        String tokenBody = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8);

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

        HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) {
            System.out.println("Token error: " + tokenResponse.statusCode());
            System.out.println(tokenResponse.body());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode tokens = mapper.readTree(tokenResponse.body());
        String accessToken = tokens.path("access_token").asText();
        String refreshToken = tokens.path("refresh_token").asText(null);
        System.out.println("Access Token: " + accessToken);
        System.out.println("Refresh Token: " + refreshToken);

        // 7) Call /2/team/members/list_v2
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.dropboxapi.com/2/team/members/list_v2"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"include_removed\": false, \"limit\": 200}"))
                .build();

        HttpResponse<String> listRsp = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        if (listRsp.statusCode() != 200) {
            System.out.println("List error: " + listRsp.statusCode());
            System.out.println(listRsp.body());
            return;
        }

        System.out.println("First page:");
        System.out.println(prettyJson(listRsp.body()));
        JsonNode root = mapper.readTree(listRsp.body());
        boolean hasMore = root.path("has_more").asBoolean(false);
        String cursor = root.path("cursor").asText(null);

        // 8) Paginate with /2/team/members/list/continue_v2
        while (hasMore && cursor != null && !cursor.isEmpty()) {
            String contBody = "{\"cursor\":\"" + cursor + "\"}";
            HttpRequest contReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dropboxapi.com/2/team/members/list/continue_v2"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(contBody))
                    .build();

            HttpResponse<String> contRsp = client.send(contReq, HttpResponse.BodyHandlers.ofString());
            if (contRsp.statusCode() != 200) {
                System.out.println("Continue error: " + contRsp.statusCode());
                System.out.println(contRsp.body());
                break;
            }

            System.out.println("Next page:");
            System.out.println(prettyJson(contRsp.body()));
            JsonNode contRoot = mapper.readTree(contRsp.body());
            hasMore = contRoot.path("has_more").asBoolean(false);
            cursor = contRoot.path("cursor").asText(null);
        }
    }

    static final class CallbackHandler implements HttpHandler {
        private final BlockingQueue<Map<String,String>> sink;
        CallbackHandler(BlockingQueue<Map<String,String>> sink) { this.sink = sink; }
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> params = splitQuery(ex.getRequestURI().getRawQuery());
            String html = "<html><body><h3>Authorization received, you can close this window.</h3></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
            sink.offer(params);
        }
    }

    static Map<String,String> splitQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return Collections.emptyMap();
        return Arrays.stream(rawQuery.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> urlDecode(kv[0]),
                        kv -> kv.length > 1 ? urlDecode(kv[1]) : ""
                ));
    }

    static String urlDecode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    static byte[] sha256(byte[] data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static String base64UrlNoPad(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    static String randomUrlSafe(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return base64UrlNoPad(bytes);
    }

    static String prettyJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }
}
