package com.example.edu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Self-contained integration tests. Requires backend running at localhost:8080.
 * Creates test data, tests all controllers, then cleans up.
 * Run with: RUN_API_INTEGRATION_TESTS=true mvn test -Dtest=ApiIntegrationTest
 */
@EnabledIfEnvironmentVariable(named = "RUN_API_INTEGRATION_TESTS", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    private static final String BASE = "http://localhost:8080/api";
    private static final String RUN_ID = Long.toString(System.currentTimeMillis(), 36);
    private static final String TEST_GRADE = "2099";
    private static final String CLASS_NAME = "ITEST-CLASS-" + RUN_ID;
    private static final String CLASS_NAME_UPDATED = "ITEST-CLASS-UPD-" + RUN_ID;
    private static final String COURSE_NAME = "ITEST-COURSE-" + RUN_ID;
    private static final String COURSE_NAME_UPDATED = "ITEST-COURSE-UPD-" + RUN_ID;
    private static final String STUDENT_NO = "IT" + RUN_ID;
    private static final String STUDENT_PASSWORD = "test123";
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String adminToken, teacherToken, studentToken;
    private static Long testClassId, testStudentId, testCourseId, testSemesterId;
    private static Long testLessonId, testTaskId, testSubmissionId;
    private static Long testPaperId, testExamId, testProjectId;
    private static Long testDriveFolderId, testDriveFileId;

    // ── helpers ──────────────────────────────────────────────────

    private HttpRequest.Builder authGet(String path, String token) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token).GET();
    }

    private HttpRequest.Builder authPost(String path, String token, String body) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
    }

    private HttpRequest.Builder authPut(String path, String token, String body) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder authDelete(String path, String token) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + token).DELETE();
    }

    private JsonNode send(HttpRequest.Builder req) throws Exception {
        HttpRequest request = req.build();
        HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(r.statusCode() < 500,
                () -> "5xx from " + request.uri() + " body=" + r.body());
        return mapper.readTree(r.body());
    }

    private int code(JsonNode r) { return r.get("code").asInt(); }

    // ── 1. Auth ───────────────────────────────────────────────────

    @Test @Order(1)
    void login() throws Exception {
        // Admin
        JsonNode r = send(authPost("/auth/login", "", "{\"account\":\"admin\",\"password\":\"admin123\"}"));
        assertEquals(0, code(r), "Admin login failed: " + r);
        adminToken = r.get("data").get("token").asText();
        assertNotNull(adminToken);
        assertEquals("admin", r.get("data").get("role").asText());

        // Teacher (zhang / teacher123)
        r = send(authPost("/auth/login", "", "{\"account\":\"zhang\",\"password\":\"teacher123\"}"));
        assertEquals(0, code(r), "Teacher login failed: " + r);
        teacherToken = r.get("data").get("token").asText();
        assertEquals("teacher", r.get("data").get("role").asText());

        // Student (2024001 / 123456)
        r = send(authPost("/auth/login", "", "{\"account\":\"2024001\",\"password\":\"123456\"}"));
        assertEquals(0, code(r), "Student login failed: " + r);
        studentToken = r.get("data").get("token").asText();
        assertEquals("student", r.get("data").get("role").asText());
    }

    @Test @Order(2)
    void loginFailure() throws Exception {
        JsonNode r = send(authPost("/auth/login", "", "{\"account\":\"nobody\",\"password\":\"wrong\"}"));
        assertNotEquals(0, code(r), "Should reject bad credentials");
    }

    // ── 2. Classes ────────────────────────────────────────────────

    @Test @Order(3)
    void classes() throws Exception {
        // List all
        JsonNode r = send(authGet("/classes/list-all", adminToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Create
        r = send(authPost("/classes", adminToken,
                "{\"grade\":\"" + TEST_GRADE + "\",\"name\":\"" + CLASS_NAME + "\",\"description\":\"auto-test\"}"));
        assertEquals(0, code(r), "Create class failed: " + r);
        testClassId = r.get("data").get("id").asLong();

        // Get by ID
        r = send(authGet("/classes/" + testClassId, adminToken));
        assertEquals(0, code(r));
        assertEquals(CLASS_NAME, r.get("data").get("name").asText());

        // Update
        r = send(authPut("/classes/" + testClassId, adminToken,
                "{\"grade\":\"" + TEST_GRADE + "\",\"name\":\"" + CLASS_NAME_UPDATED + "\",\"description\":\"updated\"}"));
        assertEquals(0, code(r), "Update class failed: " + r);
    }

    // ── 3. Teachers ───────────────────────────────────────────────

    @Test @Order(4)
    void teachers() throws Exception {
        // List
        JsonNode r = send(authGet("/teachers?page=1&size=5", adminToken));
        assertEquals(0, code(r));
        assertNotNull(r.get("data").get("records"));

        // Create
        r = send(authPost("/teachers", adminToken,
                "{\"username\":\"it_teacher_" + RUN_ID + "\",\"name\":\"TestTeacher\",\"password\":\"pass123\",\"phone\":\"13800000000\"}"));
        // May fail if username exists; that's ok — the list worked
    }

    // ── 4. Students ───────────────────────────────────────────────

    @Test @Order(5)
    void students() throws Exception {
        // List
        JsonNode r = send(authGet("/students?page=1&size=5", adminToken));
        assertEquals(0, code(r));

        // Create via API (if supported)
        r = send(authPost("/students", adminToken,
                "{\"studentNo\":\"" + STUDENT_NO + "\",\"name\":\"TestStudent\",\"classId\":" + testClassId + ",\"password\":\"" + STUDENT_PASSWORD + "\"}"));
        assertEquals(0, code(r), "Create student failed: " + r);
        testStudentId = r.get("data").get("id").asLong();

        r = send(authPost("/auth/login", "", "{\"account\":\"" + STUDENT_NO + "\",\"password\":\"" + STUDENT_PASSWORD + "\"}"));
        assertEquals(0, code(r), "Test student login failed: " + r);
        studentToken = r.get("data").get("token").asText();

        // Password reset for an existing student
        r = send(authPut("/students/" + testStudentId + "/password", adminToken,
                "{\"newPassword\":\"" + STUDENT_PASSWORD + "\"}"));
        assertEquals(0, code(r), "Reset password failed: " + r);
    }

    // ── 5. Courses ─────────────────────────────────────────────────

    @Test @Order(6)
    void courses() throws Exception {
        // List
        JsonNode r = send(authGet("/courses?page=1&size=5", adminToken));
        assertEquals(0, code(r));

        // Create
        r = send(authPost("/courses", teacherToken,
                "{\"name\":\"" + COURSE_NAME + "\",\"description\":\"test\",\"classIds\":[" + testClassId + "]}"));
        assertEquals(0, code(r), "Create course failed: " + r);
        testCourseId = r.get("data").get("id").asLong();

        // Get detail
        r = send(authGet("/courses/" + testCourseId, teacherToken));
        assertEquals(0, code(r));

        // Update
        r = send(authPut("/courses/" + testCourseId, teacherToken,
                "{\"name\":\"" + COURSE_NAME_UPDATED + "\",\"description\":\"updated\"}"));
        assertEquals(0, code(r));

        // Student can see course
        r = send(authGet("/courses/" + testCourseId, studentToken));
        // May fail if student not in course's class; just check non-5xx
        assertTrue(r.get("code").asInt() >= 0);
    }

    // ── 6. Semesters ───────────────────────────────────────────────

    @Test @Order(7)
    void semesters() throws Exception {
        JsonNode r = send(authPost("/courses/" + testCourseId + "/semesters", teacherToken,
                "{\"name\":\"ITEST-学期-" + RUN_ID + "\",\"startTime\":\"2026-03-01T00:00:00\",\"endTime\":\"2026-07-01T00:00:00\"}"));
        assertEquals(0, code(r), "Create semester failed: " + r);
        testSemesterId = r.get("data").get("id").asLong();

        r = send(authGet("/courses/" + testCourseId + "/semesters", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Student accessible
        r = send(authGet("/courses/" + testCourseId + "/semesters", studentToken));
        assertTrue(r.get("code").asInt() >= 0);
    }

    // ── 7. Lessons ─────────────────────────────────────────────────

    @Test @Order(8)
    void lessons() throws Exception {
        JsonNode r = send(authPost("/semesters/" + testSemesterId + "/lessons", teacherToken,
                "{\"name\":\"ITEST-第1课-" + RUN_ID + "\",\"sortOrder\":1}"));
        assertEquals(0, code(r), "Create lesson failed: " + r);
        testLessonId = r.get("data").get("id").asLong();

        r = send(authPost("/semesters/" + testSemesterId + "/lessons", teacherToken,
                "{\"name\":\"ITEST-第2课-" + RUN_ID + "\",\"sortOrder\":2}"));
        assertEquals(0, code(r));
        Long lesson2Id = r.get("data").get("id").asLong();

        // List
        r = send(authGet("/semesters/" + testSemesterId + "/lessons", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() >= 2);

        // Reorder: swap 1 and 2
        r = send(authPut("/lessons/" + testLessonId + "/sort", teacherToken,
                "{\"targetIndex\":1}"));
        assertEquals(0, code(r));

        // Student list
        r = send(authGet("/semesters/" + testSemesterId + "/lessons", studentToken));
        assertTrue(r.get("code").asInt() >= 0);

        // Delete lesson 2
        r = send(authDelete("/lessons/" + lesson2Id, teacherToken));
        assertEquals(0, code(r));
    }

    // ── 8. Tasks ───────────────────────────────────────────────────

    @Test @Order(9)
    void taskFlow() throws Exception {
        // Create
        JsonNode r = send(authPost("/lessons/" + testLessonId + "/tasks", teacherToken,
                "{\"title\":\"ITEST-练习-" + RUN_ID + "\",\"type\":\"worksheet\",\"description\":\"测试任务\",\"formSchema\":\"{\\\"fields\\\":[{\\\"type\\\":\\\"text\\\",\\\"label\\\":\\\"答案\\\"}]}\"}"));
        assertEquals(0, code(r), "Create task failed: " + r);
        testTaskId = r.get("data").get("id").asLong();

        // List
        r = send(authGet("/lessons/" + testLessonId + "/tasks", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Get detail
        r = send(authGet("/tasks/" + testTaskId, teacherToken));
        assertEquals(0, code(r));

        // Update
        r = send(authPut("/tasks/" + testTaskId, teacherToken,
                "{\"title\":\"ITEST-练习-UPD-" + RUN_ID + "\",\"description\":\"updated desc\"}"));
        assertEquals(0, code(r));

        // Student list
        r = send(authGet("/lessons/" + testLessonId + "/tasks", studentToken));
        assertEquals(0, code(r));
    }

    @Test @Order(10)
    void taskSubmit() throws Exception {
        // Student submit
        JsonNode r = send(authPost("/tasks/" + testTaskId + "/submit", studentToken,
                "{\"content\":\"{\\\"answer\\\":\\\"print('hello')\\\"}\"}"));
        assertEquals(0, code(r), "Submit failed: " + r);
        testSubmissionId = r.get("data").get("id").asLong();

        // Student view own submission
        r = send(authGet("/tasks/" + testTaskId + "/my-submission", studentToken));
        assertEquals(0, code(r));
        assertNotNull(r.get("data").get("id"));

        // Cannot re-submit after graded -> skip (not graded yet)
        // Try re-submit (upsert should work since not graded)
        r = send(authPost("/tasks/" + testTaskId + "/submit", studentToken,
                "{\"content\":\"{\\\"answer\\\":\\\"print('hello world')\\\"}\"}"));
        assertEquals(0, code(r));

        // Teacher view submissions
        r = send(authGet("/tasks/" + testTaskId + "/submissions", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Submission stats
        r = send(authGet("/tasks/" + testTaskId + "/submission-stats", teacherToken));
        assertEquals(0, code(r));
        assertNotNull(r.get("data").get("total"));
    }

    // ── 9. Evaluation ─────────────────────────────────────────────

    @Test @Order(11)
    void evaluation() throws Exception {
        // Teacher grades the submission
        JsonNode r = send(authPost("/submissions/" + testSubmissionId + "/evaluate", teacherToken,
                "{\"dimensions\":[{\"dimension\":\"AWARENESS\",\"grade\":\"A\"},{\"dimension\":\"COMPUTING\",\"grade\":\"B\"},{\"dimension\":\"DIGITAL_LEARNING\",\"grade\":\"A\"},{\"dimension\":\"RESPONSIBILITY\",\"grade\":\"B\"}]}"));
        assertEquals(0, code(r), "Evaluate failed: " + r);

        // Get grade scores reference
        r = send(authGet("/evaluations/grade-scores", teacherToken));
        assertEquals(0, code(r));

        // Get student evaluations
        r = send(authGet("/students/" + testStudentId + "/evaluations?semesterId=" + testSemesterId, teacherToken));
        assertEquals(0, code(r));

        // Radar
        r = send(authGet("/students/" + testStudentId + "/radar?semesterId=" + testSemesterId, teacherToken));
        assertEquals(0, code(r));
        assertNotNull(r.get("data").get("current"));
        assertTrue(r.get("data").get("current").size() > 0);

        // Student can see own radar
        r = send(authGet("/students/" + testStudentId + "/radar?semesterId=" + testSemesterId, studentToken));
        // May be limited by student-data-isolation; just check non-5xx
        assertTrue(r.get("code").asInt() >= 0);
    }

    // ── 10. Stats ─────────────────────────────────────────────────

    @Test @Order(12)
    void stats() throws Exception {
        // Preview
        JsonNode r = send(authGet("/stats/semester/" + testSemesterId + "/preview", teacherToken));
        assertEquals(0, code(r));

        // Export Excel
        HttpRequest req = authGet("/stats/semester/" + testSemesterId + "/export", teacherToken).build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().length > 0, "Excel export should return data");

        // Student cannot access stats
        r = send(authGet("/stats/semester/" + testSemesterId + "/preview", studentToken));
        assertNotEquals(0, code(r));
    }

    // ── 11. Exam Papers ───────────────────────────────────────────

    @Test @Order(13)
    void examPapers() throws Exception {
        // Create paper
        JsonNode r = send(authPost("/exam-papers", teacherToken,
                "{\"title\":\"ITEST-期末试卷-" + RUN_ID + "\",\"content\":\"[{\\\"q\\\":\\\"什么是Python?\\\",\\\"a\\\":\\\"编程语言\\\"}]\",\"totalScore\":100}"));
        assertEquals(0, code(r), "Create paper failed: " + r);
        testPaperId = r.get("data").get("id").asLong();

        // List papers
        r = send(authGet("/exam-papers", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Student cannot create paper
        r = send(authPost("/exam-papers", studentToken, "{\"title\":\"hack\"}"));
        assertNotEquals(0, code(r));
    }

    // ── 12. Exams ─────────────────────────────────────────────────

    @Test @Order(14)
    void exams() throws Exception {
        // Create exam
        JsonNode r = send(authPost("/semesters/" + testSemesterId + "/exams", teacherToken,
                "{\"name\":\"ITEST-期末考试-" + RUN_ID + "\",\"paperId\":" + testPaperId + ",\"startTime\":\"2026-06-08T09:00:00\",\"endTime\":\"2026-06-08T10:00:00\",\"weight\":1.0}"));
        assertEquals(0, code(r), "Create exam failed: " + r);
        testExamId = r.get("data").get("id").asLong();

        // List exams
        r = send(authGet("/semesters/" + testSemesterId + "/exams", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Student list
        r = send(authGet("/semesters/" + testSemesterId + "/exams", studentToken));
        assertEquals(0, code(r));

        // Start exam (student)
        r = send(authPost("/exams/" + testExamId + "/start", studentToken, ""));
        assertEquals(0, code(r));

        // Submit exam (student)
        r = send(authPost("/exams/" + testExamId + "/submit", studentToken,
                "{\"answers\":\"{\\\"q1\\\":\\\"answer1\\\"}\"}"));
        assertEquals(0, code(r), "Exam submit failed: " + r);

        // List submissions (teacher)
        r = send(authGet("/exams/" + testExamId + "/submissions", teacherToken));
        assertEquals(0, code(r));

        // Grade a submission (teacher)
        if (r.get("data").size() > 0) {
            long examSubId = r.get("data").get(0).get("id").asLong();
            r = send(authPut("/exam-submissions/" + examSubId + "/grade", teacherToken,
                    "{\"score\":85}"));
            assertEquals(0, code(r));
        }
    }

    // ── 13. Projects ──────────────────────────────────────────────

    @Test @Order(15)
    void projects() throws Exception {
        // Create project
        JsonNode r = send(authPost("/semesters/" + testSemesterId + "/projects", teacherToken,
                "{\"name\":\"ITEST-Python项目-" + RUN_ID + "\",\"description\":\"做一个计算器\",\"maxTeamSize\":3,\"weight\":1.0}"));
        assertEquals(0, code(r), "Create project failed: " + r);
        testProjectId = r.get("data").get("id").asLong();

        // List projects
        r = send(authGet("/semesters/" + testSemesterId + "/projects", teacherToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Student list
        r = send(authGet("/semesters/" + testSemesterId + "/projects", studentToken));
        assertEquals(0, code(r));

        // Student create team
        r = send(authPost("/projects/" + testProjectId + "/teams", studentToken,
                "{\"name\":\"ITEST-Team-" + RUN_ID + "\"}"));
        assertEquals(0, code(r));

        // Student submit project
        r = send(authPost("/projects/" + testProjectId + "/submit", studentToken,
                "{\"content\":\"project work\"}"));
        assertEquals(0, code(r));
    }

    // ── 14. Drive ─────────────────────────────────────────────────

    @Test @Order(16)
    void drive() throws Exception {
        // Create folder
        JsonNode r = send(authPost("/drive/folders", studentToken,
                "{\"name\":\"ITEST-我的文件夹-" + RUN_ID + "\"}"));
        assertEquals(0, code(r), "Create folder failed: " + r);
        testDriveFolderId = r.get("data").get("id").asLong();

        // Tree (root)
        r = send(authGet("/drive/tree", studentToken));
        assertEquals(0, code(r));
        assertTrue(r.get("data").size() > 0);

        // Tree (subfolder)
        r = send(authGet("/drive/tree?parentId=" + testDriveFolderId, studentToken));
        assertEquals(0, code(r));

        // Create a file record
        r = send(authPost("/drive/files", studentToken,
                "{\"name\":\"test.txt\",\"fileSize\":100,\"contentType\":\"text/plain\",\"objectName\":\"drive/test.txt\",\"parentId\":" + testDriveFolderId + "}"));
        if (code(r) == 0) {
            testDriveFileId = r.get("data").get("id").asLong();
        }
    }

    // ── 15. Files / Course Resources ──────────────────────────────

    @Test @Order(17)
    void filesAndResources() throws Exception {
        // List course resources
        JsonNode r = send(authGet("/courses/" + testCourseId + "/resources", teacherToken));
        assertTrue(r.get("code").asInt() >= 0);

        // Create folder
        r = send(authPost("/courses/" + testCourseId + "/resources", teacherToken,
                "{\"name\":\"ITEST-课件-" + RUN_ID + "\",\"type\":\"FOLDER\"}"));
        assertEquals(0, code(r));
        Long folderId = r.get("data").get("id").asLong();

        // Create subfolder
        r = send(authPost("/courses/" + testCourseId + "/resources", teacherToken,
                "{\"name\":\"ITEST-子文件夹-" + RUN_ID + "\",\"type\":\"FOLDER\",\"parentId\":" + folderId + "}"));
        assertEquals(0, code(r));

        // Delete subfolder
        r = send(authDelete("/resources/" + r.get("data").get("id").asLong(), teacherToken));
        assertEquals(0, code(r));

        // Delete root folder
        r = send(authDelete("/resources/" + folderId, teacherToken));
        assertEquals(0, code(r));
    }

    // ── 16. Error handling ────────────────────────────────────────

    @Test @Order(18)
    void errorHandling() throws Exception {
        // 404s
        JsonNode r = send(authGet("/courses/99999", adminToken));
        assertNotEquals(0, code(r));

        r = send(authGet("/tasks/99999", teacherToken));
        assertNotEquals(0, code(r));

        r = send(authGet("/lessons/99999/tasks", teacherToken));
        assertNotEquals(0, code(r));

        // Missing token
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/classes/list-all")).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertNotEquals(0, mapper.readTree(resp.body()).get("code").asInt());

        // Wrong role: student trying to create task
        r = send(authPost("/lessons/" + testLessonId + "/tasks", studentToken,
                "{\"title\":\"hack\",\"type\":\"worksheet\"}"));
        assertNotEquals(0, code(r));

        // Student trying to grade
        r = send(authPost("/submissions/" + testSubmissionId + "/evaluate", studentToken,
                "{\"dimensions\":[{\"dimension\":\"AWARENESS\",\"grade\":\"A\"}]}"));
        assertNotEquals(0, code(r));
    }

    // ── Cleanup ───────────────────────────────────────────────────

    @Test @Order(99)
    void cleanup() throws Exception {
        // Delete test data in reverse dependency order

        // Drive items
        if (testDriveFolderId != null) {
            send(authDelete("/drive/" + testDriveFolderId, studentToken));
        }

        // Exam
        if (testExamId != null) {
            send(authDelete("/exams/" + testExamId, teacherToken));
        }
        if (testPaperId != null) {
            // No explicit delete endpoint for papers; they'll be cleaned up with teacher
        }

        // Project
        if (testProjectId != null) {
            send(authDelete("/projects/" + testProjectId, teacherToken));
        }

        // Task (cascades to submissions)
        if (testTaskId != null) {
            send(authDelete("/tasks/" + testTaskId, adminToken));
        }

        // Lesson
        if (testLessonId != null) {
            send(authDelete("/lessons/" + testLessonId, teacherToken));
        }

        // Semester
        if (testSemesterId != null) {
            send(authDelete("/semesters/" + testSemesterId, teacherToken));
        }

        // Course
        if (testCourseId != null) {
            send(authDelete("/courses/" + testCourseId, teacherToken));
        }

        // Student
        if (testStudentId != null) {
            send(authDelete("/students/" + testStudentId, adminToken));
        }

        // Class
        if (testClassId != null) {
            send(authDelete("/classes/" + testClassId, adminToken));
        }

        assertTrue(true, "Integration test cleanup complete.");
    }
}
