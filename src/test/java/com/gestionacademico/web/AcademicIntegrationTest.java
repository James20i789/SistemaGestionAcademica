package com.gestionacademico.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "academia.admin.email=admin@academia.test",
        "academia.admin.password=clave-admin-de-prueba"
})
@AutoConfigureMockMvc
class AcademicIntegrationTest {
    private static final String ADMIN_EMAIL = "admin@academia.test";
    private static final String ADMIN_PASSWORD = "clave-admin-de-prueba";
    private static final String USER_PASSWORD = "clave-usuario-de-prueba";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AcademicStore store;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv().getOrDefault("TEST_DATABASE_URL",
                "jdbc:h2:mem:academic_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DATABASE_USER", "sa"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
    }

    @BeforeEach
    void resetData() {
        db.update("DELETE FROM account WHERE email <> ?", ADMIN_EMAIL);
        for (String table : new String[] {"grade", "assessment", "attendance", "schedule", "enrollment",
                "course", "student", "teacher", "legacy_attendance"}) {
            db.update("DELETE FROM " + table);
        }
    }

    @Test
    void requiresSessionAndCsrfAndStoresOnlyPasswordHash() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location", endsWith("/login")));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(formLogin().user(ADMIN_EMAIL).password("incorrecta")).andExpect(redirectedUrl("/login?error"));

        var admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(get("/api/me").session(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.account.role").value("ADMIN")).andExpect(jsonPath("$.csrfToken").isString())
                .andExpect(content().string(not(containsString("password"))));
        mvc.perform(post("/api/students").session(admin).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertThat(db.queryForObject("SELECT password_hash FROM account WHERE email = ?", String.class, ADMIN_EMAIL))
                .startsWith("$2").doesNotContain(ADMIN_PASSWORD);
    }

    @Test
    void keepsExternalSchemeAndPortWhenRedirectingThroughTheProxy() throws Exception {
        mvc.perform(get("/").header("X-Forwarded-Host", "academia.test:18443").header("X-Forwarded-Proto", "https"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://academia.test:18443/login"));
    }

    @Test
    void maintainsLargeCatalogAndRejectsDuplicateOrLinkedRecords() throws Exception {
        var admin = admin();
        for (int i = 201; i <= 325; i++) {
            db.update("INSERT INTO student (document, first_name, last_name, gender, section) VALUES (?, ?, ?, ?, ?)",
                    String.valueOf(i), "Estudiante", "Prueba", "Otro", "7-1");
        }
        long student = create(admin, "/students", student("00088", "Ana"));
        call(admin, HttpMethod.GET, "/students", null).andExpect(jsonPath("$.length()").value(126));
        call(admin, HttpMethod.POST, "/students", student("00088", "Repetida")).andExpect(status().isConflict());
        call(admin, HttpMethod.PUT, "/students/" + student, student("00088", "Ana María")).andExpect(status().isOk());
        var teacher = create(admin, "/teachers", teacher("DOC-1"));
        var course = create(admin, "/courses", course("ABC-001", teacher, 1));
        call(admin, HttpMethod.POST, "/courses", course("abc-001", teacher, 2)).andExpect(status().isConflict());
        enroll(admin, course, student, true);
        call(admin, HttpMethod.DELETE, "/students/" + student, null).andExpect(status().isConflict());
        call(admin, HttpMethod.DELETE, "/courses/" + course, null).andExpect(status().isConflict());
        call(admin, HttpMethod.DELETE, "/teachers/" + teacher, null).andExpect(status().isConflict());
        long selected = db.queryForObject("SELECT id FROM student WHERE document = '325'", Long.class);
        call(admin, HttpMethod.DELETE, "/students/" + selected, null).andExpect(status().isNoContent());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM student", Long.class)).isEqualTo(125L);
    }

    @Test
    void serializesCapacityChecksAcrossConcurrentEnrollments() throws Exception {
        var admin = admin();
        long teacher = create(admin, "/teachers", teacher("DOC-2"));
        long course = create(admin, "/courses", course("CAP-01", teacher, 1));
        long first = create(admin, "/students", student("101", "Marta"));
        long second = create(admin, "/students", student("102", "Luis"));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> firstEnrollment = () -> tryEnrollment(course, first);
            Callable<Boolean> secondEnrollment = () -> tryEnrollment(course, second);
            var outcomes = executor.invokeAll(java.util.List.of(firstEnrollment, secondEnrollment));
            assertThat(java.util.List.of(outcomes.get(0).get(), outcomes.get(1).get())).containsExactlyInAnyOrder(true, false);
        }
        call(admin, HttpMethod.GET, "/courses/" + course + "/enrollments", null)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void scopesTeacherAndStudentOperationsAndKeepsAttendanceHistory() throws Exception {
        var admin = admin();
        long teacher = create(admin, "/teachers", teacher("DOC-3"));
        long other = create(admin, "/teachers", teacher("DOC-4"));
        long course = create(admin, "/courses", course("MAT-01", teacher, 3));
        long otherCourse = create(admin, "/courses", course("HIS-01", other, 5));
        long first = create(admin, "/students", student("201", "Lucía"));
        long second = create(admin, "/students", student("202", "Tomás"));
        enroll(admin, course, first, true);
        enroll(admin, course, second, true);
        account(admin, "prof@academia.test", "TEACHER", teacher);
        account(admin, "est@academia.test", "STUDENT", first);
        var prof = login("prof@academia.test", USER_PASSWORD);
        var student = login("est@academia.test", USER_PASSWORD);
        call(prof, HttpMethod.GET, "/courses", null).andExpect(jsonPath("$.length()").value(1));
        call(prof, HttpMethod.GET, "/students", null).andExpect(status().isForbidden());
        call(prof, HttpMethod.PUT, "/courses/" + course + "/enrollments/" + first, Map.of("active", false))
                .andExpect(status().isForbidden());
        String date = read(call(admin, HttpMethod.GET, "/summary", null)).get("date").asText();
        String attendance = "/attendance";
        String roster = attendance + "/roster?date=" + date + "&courseId=";
        call(prof, HttpMethod.GET, roster + otherCourse, null).andExpect(status().isForbidden());
        call(student, HttpMethod.GET, roster + course, null).andExpect(status().isForbidden());
        var marks = java.util.List.of(Map.of("studentId", first, "status", "PRESENTE"),
                Map.of("studentId", second, "status", "AUSENTE"));
        var form = Map.of("courseId", course, "date", date, "marks", marks);
        call(student, HttpMethod.PUT, attendance, form).andExpect(status().isForbidden());
        call(prof, HttpMethod.PUT, attendance, form).andExpect(status().isNoContent());
        call(prof, HttpMethod.PUT, attendance, form).andExpect(status().isNoContent());
        call(admin, HttpMethod.GET, "/summary", null).andExpect(jsonPath("$.marked").value(2))
                .andExpect(jsonPath("$.attendancePercent").value(50));
        call(student, HttpMethod.GET, attendance, null).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentName").value("Lucía Prueba"));
        enroll(admin, course, second, false);
        call(prof, HttpMethod.GET, roster + course, null).andExpect(jsonPath("$.length()").value(2));
        call(prof, HttpMethod.DELETE, attendance + "?courseId=" + course + "&date=" + date + "&studentId=" + second,
                null).andExpect(status().isNoContent());
        call(prof, HttpMethod.GET, attendance, null).andExpect(jsonPath("$.length()").value(1));
        call(prof, HttpMethod.PUT, attendance, form).andExpect(status().isNoContent());
        call(prof, HttpMethod.GET, attendance + ".csv", null)
                .andExpect(header().string("Content-Disposition", containsString("asistencias.csv")))
                .andExpect(content().string(containsString("Tomás Prueba")));
        call(student, HttpMethod.GET, attendance + ".csv", null)
                .andExpect(content().string(not(containsString("Tomás Prueba"))));
    }

    @Test
    void preventsScheduleOverlapsAndSupportsEdits() throws Exception {
        var admin = admin();
        long teacher = create(admin, "/teachers", teacher("DOC-5"));
        long other = create(admin, "/teachers", teacher("DOC-6"));
        long first = create(admin, "/courses", course("MAT-02", teacher, 30));
        long second = create(admin, "/courses", Map.of("code", "QUI-01", "name", "Química",
                "teacherId", other, "capacity", 30, "section", "8-2"));
        long existing = create(admin, "/schedule", slot(first, "09:00", "10:00", "Laboratorio"));
        call(admin, HttpMethod.POST, "/schedule", slot(second, "09:59", "11:00", "laboratorio"))
                .andExpect(status().isConflict());
        create(admin, "/schedule", slot(second, "10:00", "11:00", "Laboratorio"));
        call(admin, HttpMethod.PUT, "/schedule/" + existing, slot(first, "08:00", "09:00", "Aula 101"))
                .andExpect(status().isOk());
        call(admin, HttpMethod.POST, "/schedule", slot(first, "13:00", "12:00", "Aula 101"))
                .andExpect(status().isBadRequest());
        call(admin, HttpMethod.GET, "/schedule", null).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void recordsAndExportsGradesWithinTheCourseAndStudentScope() throws Exception {
        var admin = admin();
        long teacher = create(admin, "/teachers", teacher("DOC-7"));
        long course = create(admin, "/courses", course("MAT-03", teacher, 30));
        long first = create(admin, "/students", student("301", "Elena"));
        long second = create(admin, "/students", student("302", "Álvaro"));
        enroll(admin, course, first, true);
        enroll(admin, course, second, true);
        account(admin, "teacher@academia.test", "TEACHER", teacher);
        account(admin, "student@academia.test", "STUDENT", first);
        var prof = login("teacher@academia.test", USER_PASSWORD);
        var student = login("student@academia.test", USER_PASSWORD);
        long assessment = create(prof, "/assessments",
                Map.of("courseId", course, "title", "=Evaluación de álgebra", "date", "2026-09-15"));
        String path = "/assessments/" + assessment + "/grades";
        call(student, HttpMethod.GET, path, null).andExpect(status().isForbidden());
        call(prof, HttpMethod.GET, path, null).andExpect(jsonPath("$.length()").value(2));
        call(prof, HttpMethod.PUT, path, Map.of("marks", java.util.List.of(
                Map.of("studentId", first, "score", 21), Map.of("studentId", second, "score", 15))))
                .andExpect(status().isBadRequest());
        call(prof, HttpMethod.PUT, path, Map.of("marks", java.util.List.of(
                Map.of("studentId", first, "score", 18.75), Map.of("studentId", second, "score", 13))))
                .andExpect(status().isNoContent());
        call(student, HttpMethod.GET, "/grades?from=2026-09-15&to=2026-09-15", null)
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].score").value(18.75));
        call(student, HttpMethod.GET, "/grades.csv", null)
                .andExpect(content().string(containsString("'=Evaluación")))
                .andExpect(content().string(not(containsString("Álvaro"))));
        call(prof, HttpMethod.DELETE, "/assessments/" + assessment, null).andExpect(status().isNoContent());
        call(student, HttpMethod.GET, "/grades", null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void importsOldFilesAtomicallyAndRetainsUnscopedAttendance() throws Exception {
        var admin = admin();
        call(admin, HttpMethod.POST, "/legacy", Map.of("students", "211,Julia,Prueba,Otro,7-1",
                "courses", "IMP-01,Geometría,Profesor inexistente,22")).andExpect(status().isBadRequest());
        call(admin, HttpMethod.GET, "/students", null).andExpect(jsonPath("$.length()").value(0));
        var files = Map.of(
                "students", "211,Julia,Prueba,Otro,7-1",
                "teachers", "123,María,Prueba,Matemáticas,docente@academia.test",
                "courses", "IMP-01,Geometría,Maria Prueba,22",
                "attendance", "Julia Prueba,2026-09-16,Justificación");
        call(admin, HttpMethod.POST, "/legacy", files).andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").value(1)).andExpect(jsonPath("$.attendance").value(1));
        call(admin, HttpMethod.POST, "/legacy", files).andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").value(0)).andExpect(jsonPath("$.attendance").value(0));
        call(admin, HttpMethod.GET, "/attendance", null).andExpect(jsonPath("$.length()").value(0));
        call(admin, HttpMethod.GET, "/legacy", null).andExpect(jsonPath("$[0].status").value("JUSTIFICACION"));
    }

    @Test
    void revokesSessionsAfterPasswordChangesAndAdministrativeDeactivation() throws Exception {
        var admin = admin();
        long teacher = create(admin, "/teachers", teacher("DOC-8"));
        String email = "prueba@academia.test";
        account(admin, email, "TEACHER", teacher);
        var session = login(email, USER_PASSWORD);
        call(session, HttpMethod.PUT, "/account/password",
                Map.of("currentPassword", USER_PASSWORD, "newPassword", "otra-clave-de-prueba"))
                .andExpect(status().isNoContent());
        call(session, HttpMethod.GET, "/summary", null).andExpect(status().isUnauthorized());
        mvc.perform(formLogin().user(email).password(USER_PASSWORD)).andExpect(redirectedUrl("/login?error"));
        var next = login(email, "otra-clave-de-prueba");
        long id = read(call(next, HttpMethod.GET, "/me", null))
                .get("account").get("id").asLong();
        call(admin, HttpMethod.PUT, "/accounts/" + id, Map.of("email", email, "name", "Docente", "role", "TEACHER",
                "teacherId", teacher, "enabled", false)).andExpect(status().isNoContent());
        call(next, HttpMethod.GET, "/summary", null).andExpect(status().isUnauthorized());
    }

    private boolean tryEnrollment(long course, long student) {
        try {
            store.setEnrollment(course, student, true);
            return true;
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(409);
            return false;
        }
    }

    private static Map<String, Object> student(String document, String name) {
        return Map.of("document", document, "firstName", name, "lastName", "Prueba", "gender", "Otro", "section", "7-1");
    }

    private static Map<String, Object> teacher(String document) {
        return Map.of("document", document, "firstName", "Profesora", "lastName", document, "specialty", "Ciencias",
                "email", document + "@academia.test");
    }

    private static Map<String, Object> course(String code, long teacher, int capacity) {
        return Map.of("code", code, "name", "Matemática", "teacherId", teacher, "capacity", capacity, "section", "7-1");
    }

    private static Map<String, Object> slot(long course, String start, String end, String room) {
        return Map.of("courseId", course, "weekday", 1, "startsAt", start, "endsAt", end, "room", room);
    }

    private MockHttpSession admin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(email).password(password))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private void account(MockHttpSession admin, String email, String role, long profile) throws Exception {
        call(admin, HttpMethod.POST, "/accounts", Map.of("name", email, "email", email, "role", role,
                "password", USER_PASSWORD, "enabled", true, role.equals("TEACHER") ? "teacherId" : "studentId", profile))
                .andExpect(status().isCreated());
    }

    private void enroll(MockHttpSession admin, long course, long student, boolean active) throws Exception {
        call(admin, HttpMethod.PUT, "/courses/" + course + "/enrollments/" + student, Map.of("active", active))
                .andExpect(status().isNoContent());
    }

    private long create(MockHttpSession session, String path, Object data) throws Exception {
        return read(call(session, HttpMethod.POST, path, data).andExpect(status().isCreated())).get("id").asLong();
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    private ResultActions call(MockHttpSession session, HttpMethod method, String path, Object data) throws Exception {
        var req = request(method, "/api" + path);
        if (!session.isInvalid()) {
            req.session(session);
        }
        if (!HttpMethod.GET.equals(method)) {
            req.with(csrf().asHeader());
        }
        if (data != null) {
            req.contentType("application/json").content(json.writeValueAsBytes(data));
        }
        return mvc.perform(req);
    }
}
