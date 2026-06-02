package com.example.edu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    private static final String BASE = "http://localhost:8080/api";
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String adminToken, teacherToken, studentToken;

    private HttpRequest.Builder authGet(String path, String token) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token).GET();
    }

    private HttpRequest.Builder authPost(String path, String token, String body) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder authDelete(String path, String token) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token).DELETE();
    }

    private JsonNode send(HttpRequest.Builder req) throws Exception {
        HttpResponse<String> r = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(r.body());
    }

    @Test @Order(1)
    void login() throws Exception {
        JsonNode r = send(authPost("/auth/login", "", "{\"account\":\"admin\",\"password\":\"admin123\"}"));
        assertEquals(0, r.get("code").asInt());
        adminToken = r.get("data").get("token").asText();
        assertNotNull(adminToken);

        r = send(authPost("/auth/login", "", "{\"account\":\"zhang\",\"password\":\"teacher123\"}"));
        assertEquals(0, r.get("code").asInt());
        teacherToken = r.get("data").get("token").asText();

        r = send(authPost("/auth/login", "", "{\"account\":\"2024001\",\"password\":\"123456\"}"));
        assertEquals(0, r.get("code").asInt());
        studentToken = r.get("data").get("token").asText();
    }

    @Test @Order(2)
    void classes() throws Exception {
        JsonNode r = send(authGet("/classes/list-all", adminToken));
        assertEquals(0, r.get("code").asInt());
        assertTrue(r.get("data").size() > 0, "Should have classes");
    }

    @Test @Order(3)
    void courses() throws Exception {
        JsonNode r = send(authGet("/courses?page=1&size=5", adminToken));
        assertEquals(0, r.get("code").asInt());
    }

    @Test @Order(4)
    void taskFlow() throws Exception {
        // Create
        JsonNode r = send(authPost("/lessons/1/tasks", teacherToken, "{\"title\":\"Integration-Test\",\"type\":\"worksheet\"}"));
        assertEquals(0, r.get("code").asInt());
        int taskId = r.get("data").get("id").asInt();

        // List
        r = send(authGet("/lessons/1/tasks", teacherToken));
        assertEquals(0, r.get("code").asInt());

        // Submit
        r = send(authPost("/tasks/" + taskId + "/submit", studentToken, "{\"content\":\"my answer\"}"));
        assertEquals(0, r.get("code").asInt());

        // Teacher views
        r = send(authGet("/tasks/" + taskId + "/submissions", teacherToken));
        assertEquals(0, r.get("code").asInt());
        assertTrue(r.get("data").size() >= 1);

        // Delete
        r = send(authDelete("/tasks/" + taskId, adminToken));
        assertEquals(0, r.get("code").asInt());
    }

    @Test @Order(5)
    void evaluation() throws Exception {
        JsonNode r = send(authGet("/students/127/radar?semesterId=1", adminToken));
        assertEquals(0, r.get("code").asInt());
    }

    @Test @Order(6)
    void stats() throws Exception {
        JsonNode r = send(authGet("/stats/semester/1/preview", adminToken));
        assertEquals(0, r.get("code").asInt());
    }

    @Test @Order(7)
    void examEndpoints() throws Exception {
        assertEquals(0, send(authGet("/exam-papers", adminToken)).get("code").asInt());
        assertEquals(0, send(authGet("/semesters/1/projects", adminToken)).get("code").asInt());
        assertEquals(0, send(authGet("/drive/tree", adminToken)).get("code").asInt());
    }

    @Test @Order(8)
    void authRejection() throws Exception {
        // Missing token
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/classes/list-all")).GET().build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode body = mapper.readTree(r.body());
        assertTrue(body.get("code").asInt() != 0);

        // Wrong role
        JsonNode r2 = send(authPost("/lessons/1/tasks", studentToken, "{\"title\":\"hack\",\"type\":\"worksheet\"}"));
        assertTrue(r2.get("code").asInt() != 0);
    }
}
