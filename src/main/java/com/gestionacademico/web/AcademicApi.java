package com.gestionacademico.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.gestionacademico.web.AcademicData.*;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api")
class AcademicApi {
    private final AcademicStore store;
    private final Accounts accounts;

    AcademicApi(AcademicStore store, Accounts accounts) {
        this.store = store;
        this.accounts = accounts;
    }

    @GetMapping("/me")
    Map<String, Object> me(@AuthenticationPrincipal Accounts.SessionUser user, @RequestAttribute("_csrf") CsrfToken csrf) {
        return Map.of("account", user.account(), "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName());
    }

    @PutMapping("/account/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Accounts.SessionUser user, @Valid @RequestBody ProfilePassword form) {
        accounts.changePassword(user.account().id(), form);
    }

    @GetMapping("/summary")
    Summary summary(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.summary(user.account());
    }

    @GetMapping("/students")
    List<Student> students(@AuthenticationPrincipal Accounts.SessionUser user) {
        admin(user.account());
        return store.students();
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> createStudent(@AuthenticationPrincipal Accounts.SessionUser user,
                                    @Valid @RequestBody StudentForm form) {
        admin(user.account());
        return id(store.saveStudent(null, form));
    }

    @PutMapping("/students/{id}")
    Map<String, Long> editStudent(@AuthenticationPrincipal Accounts.SessionUser user,
                                  @PathVariable long id, @Valid @RequestBody StudentForm form) {
        admin(user.account());
        return id(store.saveStudent(id, form));
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteStudent(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        admin(user.account());
        store.deleteStudent(id);
    }

    @GetMapping("/teachers")
    List<Teacher> teachers(@AuthenticationPrincipal Accounts.SessionUser user) {
        admin(user.account());
        return store.teachers();
    }

    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> createTeacher(@AuthenticationPrincipal Accounts.SessionUser user,
                                    @Valid @RequestBody TeacherForm form) {
        admin(user.account());
        return id(store.saveTeacher(null, form));
    }

    @PutMapping("/teachers/{id}")
    Map<String, Long> editTeacher(@AuthenticationPrincipal Accounts.SessionUser user,
                                  @PathVariable long id, @Valid @RequestBody TeacherForm form) {
        admin(user.account());
        return id(store.saveTeacher(id, form));
    }

    @DeleteMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTeacher(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        admin(user.account());
        store.deleteTeacher(id);
    }

    @GetMapping("/courses")
    List<Course> courses(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.courses(user.account());
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> createCourse(@AuthenticationPrincipal Accounts.SessionUser user,
                                   @Valid @RequestBody CourseForm form) {
        admin(user.account());
        return id(store.saveCourse(null, form));
    }

    @PutMapping("/courses/{id}")
    Map<String, Long> editCourse(@AuthenticationPrincipal Accounts.SessionUser user,
                                 @PathVariable long id, @Valid @RequestBody CourseForm form) {
        admin(user.account());
        return id(store.saveCourse(id, form));
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCourse(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        admin(user.account());
        store.deleteCourse(id);
    }

    @GetMapping("/courses/{courseId}/enrollments")
    List<Enrollment> enrollments(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long courseId) {
        return store.enrollments(user.account(), courseId);
    }

    @PutMapping("/courses/{courseId}/enrollments/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setEnrollment(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long courseId,
                       @PathVariable long studentId, @Valid @RequestBody EnrollmentForm form) {
        admin(user.account());
        store.setEnrollment(courseId, studentId, form.active());
    }

    @GetMapping("/attendance/roster")
    ResponseEntity<List<RosterRow>> roster(@AuthenticationPrincipal Accounts.SessionUser user, @RequestParam long courseId,
                                         @RequestParam @DateTimeFormat(iso = DATE) LocalDate date) {
        var snapshot = store.attendanceRoster(user.account(), courseId, date);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().noTransform()).eTag(snapshot.version()).body(snapshot.rows());
    }

    @PutMapping("/attendance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void saveAttendance(@AuthenticationPrincipal Accounts.SessionUser user, @Valid @RequestBody AttendanceForm form,
                        @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String version) {
        store.saveAttendance(user.account(), form, version);
    }

    @DeleteMapping("/attendance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAttendance(@AuthenticationPrincipal Accounts.SessionUser user, @RequestParam long courseId,
                          @RequestParam long studentId, @RequestParam @DateTimeFormat(iso = DATE) LocalDate date,
                          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String version) {
        store.deleteAttendance(user.account(), courseId, date, studentId, version);
    }

    @GetMapping("/attendance")
    ResponseEntity<List<AttendanceRow>> attendance(@AuthenticationPrincipal Accounts.SessionUser user,
                                                   @RequestParam(required = false) Long courseId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        return page(store.attendance(user.account(), courseId, from, to, page, size));
    }

    @GetMapping("/attendance/courses")
    List<CourseOption> attendanceCourses(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.reportCourses(user.account(), false);
    }

    @GetMapping("/attendance.csv")
    void attendanceCsv(@AuthenticationPrincipal Accounts.SessionUser user,
                       @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
                       HttpServletResponse response) throws IOException {
        AcademicStore.checkDates(from, to);
        var csv = new CsvExport(response, "asistencias.csv", "Fecha,Código,Curso,Estudiante,Estado");
        store.exportAttendance(user.account(), courseId, from, to, row ->
                csv.row(row.date(), row.courseCode(), row.courseName(), row.studentName(), row.status()));
        csv.flush();
    }

    @GetMapping("/schedule")
    List<Timeslot> schedule(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.schedule(user.account());
    }

    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> createTimeslot(@AuthenticationPrincipal Accounts.SessionUser user,
                                     @Valid @RequestBody TimeslotForm form) {
        admin(user.account());
        return id(store.saveTimeslot(null, form));
    }

    @PutMapping("/schedule/{id}")
    Map<String, Long> editTimeslot(@AuthenticationPrincipal Accounts.SessionUser user,
                                   @PathVariable long id, @Valid @RequestBody TimeslotForm form) {
        admin(user.account());
        return id(store.saveTimeslot(id, form));
    }

    @DeleteMapping("/schedule/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTimeslot(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        admin(user.account());
        store.deleteTimeslot(id);
    }

    @GetMapping("/assessments")
    List<Assessment> assessments(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.assessments(user.account());
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> createAssessment(@AuthenticationPrincipal Accounts.SessionUser user,
                                       @Valid @RequestBody AssessmentForm form) {
        return id(store.saveAssessment(user.account(), null, form));
    }

    @PutMapping("/assessments/{id}")
    Map<String, Long> editAssessment(@AuthenticationPrincipal Accounts.SessionUser user,
                                     @PathVariable long id, @Valid @RequestBody AssessmentForm form) {
        return id(store.saveAssessment(user.account(), id, form));
    }

    @DeleteMapping("/assessments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAssessment(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        store.deleteAssessment(user.account(), id);
    }

    @GetMapping("/assessments/{id}/grades")
    ResponseEntity<List<GradeRow>> gradeRoster(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id) {
        var snapshot = store.gradeRoster(user.account(), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().noTransform()).eTag(snapshot.version()).body(snapshot.rows());
    }

    @PutMapping("/assessments/{id}/grades")
    ResponseEntity<Void> saveGrades(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id,
                                    @Valid @RequestBody GradeForm form,
                                    @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String version) {
        return ResponseEntity.noContent().eTag(store.saveGrades(user.account(), id, form, version)).build();
    }

    @GetMapping("/grades")
    ResponseEntity<List<GradeRow>> grades(@AuthenticationPrincipal Accounts.SessionUser user,
                                           @RequestParam(required = false) Long courseId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return page(store.grades(user.account(), courseId, from, to, page, size));
    }

    @GetMapping("/grades/courses")
    List<CourseOption> gradeCourses(@AuthenticationPrincipal Accounts.SessionUser user) {
        return store.reportCourses(user.account(), true);
    }

    @GetMapping("/grades.csv")
    void gradesCsv(@AuthenticationPrincipal Accounts.SessionUser user,
                   @RequestParam(required = false) Long courseId,
                   @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
                   @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
                   HttpServletResponse response) throws IOException {
        AcademicStore.checkDates(from, to);
        var csv = new CsvExport(response, "calificaciones.csv", "Fecha,Curso,Evaluación,Estudiante,Nota");
        store.exportGrades(user.account(), courseId, from, to, row ->
                csv.row(row.date(), row.courseName(), row.assessment(), row.studentName(), row.score()));
        csv.flush();
    }

    @GetMapping("/accounts")
    List<Account> accounts(@AuthenticationPrincipal Accounts.SessionUser user) {
        admin(user.account());
        return accounts.list();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    void createAccount(@AuthenticationPrincipal Accounts.SessionUser user, @Valid @RequestBody AccountForm form) {
        admin(user.account());
        accounts.create(form);
    }

    @PutMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void editAccount(@AuthenticationPrincipal Accounts.SessionUser user, @PathVariable long id,
                     @Valid @RequestBody AccountForm form) {
        admin(user.account());
        accounts.update(id, user.account().id(), form);
    }

    static void admin(Account account) {
        if (account.role() != Role.ADMIN) {
            throw ApiErrors.denied();
        }
    }

    static <T> ResponseEntity<List<T>> page(Page<T> result) {
        return ResponseEntity.ok().header("X-Has-More", Boolean.toString(result.hasMore())).body(result.rows());
    }

    private static Map<String, Long> id(long id) {
        return Map.of("id", id);
    }
}
