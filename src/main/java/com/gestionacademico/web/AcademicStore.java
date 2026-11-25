package com.gestionacademico.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.gestionacademico.web.AcademicData.*;
import static com.gestionacademico.web.ApiErrors.*;

@Service
class AcademicStore {
    private static final RowMapper<Student> STUDENT = (r, n) -> new Student(r.getLong("id"),
            r.getString("document"), r.getString("first_name"), r.getString("last_name"),
            r.getString("gender"), r.getString("section"));
    private static final RowMapper<Teacher> TEACHER = (r, n) -> new Teacher(r.getLong("id"),
            r.getString("document"), r.getString("first_name"), r.getString("last_name"),
            r.getString("specialty"), r.getString("email"));
    private static final RowMapper<Course> COURSE = (r, n) -> new Course(r.getLong("id"), r.getString("code"),
            r.getString("name"), r.getLong("teacher_id"), r.getString("teacher_name"), r.getInt("capacity"),
            r.getString("section"), r.getInt("enrolled"));
    private static final String COURSES = """
            SELECT c.*, t.first_name || ' ' || t.last_name AS teacher_name,
                (SELECT COUNT(*) FROM enrollment e WHERE e.course_id = c.id AND e.active = TRUE) AS enrolled
            FROM course c JOIN teacher t ON t.id = c.teacher_id
            """;
    private static final RowMapper<Timeslot> TIMESLOT = (r, n) -> new Timeslot(r.getLong("id"),
            r.getLong("course_id"), r.getString("course_name"), r.getInt("weekday"),
            r.getObject("starts_at", LocalTime.class), r.getObject("ends_at", LocalTime.class),
            r.getString("room"));
    private static final RowMapper<Assessment> ASSESSMENT = (r, n) -> new Assessment(r.getLong("id"),
            r.getLong("course_id"), r.getString("course_name"), r.getString("title"),
            r.getObject("on_date", LocalDate.class));

    private final JdbcTemplate db;
    private final ZoneId zone;

    AcademicStore(JdbcTemplate db, @Value("${academia.zone}") String zone) {
        this.db = db;
        this.zone = ZoneId.of(zone);
    }

    List<Student> students() {
        return db.query("SELECT * FROM student ORDER BY last_name, first_name, id", STUDENT);
    }

    List<Teacher> teachers() {
        return db.query("SELECT * FROM teacher ORDER BY last_name, first_name, id", TEACHER);
    }

    @Transactional
    public long saveStudent(Long id, StudentForm f) {
        lock();
        if (id == null) {
            return insert("""
                    INSERT INTO student (document, first_name, last_name, gender, section) VALUES (?, ?, ?, ?, ?)
                    """, f.document().strip(), f.firstName().strip(), f.lastName().strip(), f.gender().strip(),
                    f.section().strip());
        }
        requireUpdated(db.update("""
                UPDATE student SET document = ?, first_name = ?, last_name = ?, gender = ?, section = ? WHERE id = ?
                """, f.document().strip(), f.firstName().strip(), f.lastName().strip(), f.gender().strip(),
                f.section().strip(), id));
        return id;
    }

    @Transactional
    public long saveTeacher(Long id, TeacherForm f) {
        lock();
        if (id == null) {
            return insert("""
                    INSERT INTO teacher (document, first_name, last_name, specialty, email) VALUES (?, ?, ?, ?, ?)
                    """, f.document().strip(), f.firstName().strip(), f.lastName().strip(), f.specialty().strip(),
                    Accounts.email(f.email()));
        }
        requireUpdated(db.update("""
                UPDATE teacher SET document = ?, first_name = ?, last_name = ?, specialty = ?, email = ? WHERE id = ?
                """, f.document().strip(), f.firstName().strip(), f.lastName().strip(), f.specialty().strip(),
                Accounts.email(f.email()), id));
        return id;
    }

    @Transactional
    public void deleteStudent(long id) {
        lock();
        requireUpdated(db.update("DELETE FROM student WHERE id = ?", id));
    }

    @Transactional
    public void deleteTeacher(long id) {
        lock();
        requireUpdated(db.update("DELETE FROM teacher WHERE id = ?", id));
    }

    List<Course> courses(Account actor) {
        Scope scope = courseScope(actor);
        return db.query(COURSES + " WHERE " + scope.condition + " ORDER BY c.code", COURSE, scope.params());
    }

    @Transactional
    public long saveCourse(Long id, CourseForm f) {
        lock();
        String code = f.code().strip().toUpperCase(Locale.ROOT);
        String section = f.section().strip();
        if (code.length() > 40) {
            throw invalid("El código del curso debe tener como máximo 40 caracteres.");
        }
        if (id == null) {
            return insert("""
                    INSERT INTO course (code, name, teacher_id, capacity, section) VALUES (?, ?, ?, ?, ?)
                    """, code, f.name().strip(), f.teacherId(), f.capacity(), section);
        }
        Course previous = course(id);
        if (previous.enrolled() > f.capacity()) {
            throw conflict("El cupo no puede ser menor que la matrícula activa.");
        }
        if (previous.teacherId() != f.teacherId() || !previous.section().equalsIgnoreCase(section)) {
            for (Timeslot slot : scheduleForCourse(id)) {
                ensureAvailable(slot.id(), id, f.teacherId(), section, slot.weekday(),
                        slot.startsAt(), slot.endsAt(), slot.room());
            }
        }
        requireUpdated(db.update("""
                UPDATE course SET code = ?, name = ?, teacher_id = ?, capacity = ?, section = ? WHERE id = ?
                """, code, f.name().strip(), f.teacherId(), f.capacity(), section, id));
        return id;
    }

    @Transactional
    public void deleteCourse(long id) {
        lock();
        requireUpdated(db.update("DELETE FROM course WHERE id = ?", id));
    }

    List<Enrollment> enrollments(Account actor, long courseId) {
        requireCourseEditor(actor, courseId);
        return db.query("""
                SELECT s.id, s.document, s.first_name || ' ' || s.last_name AS full_name, s.section, e.active
                FROM enrollment e JOIN student s ON s.id = e.student_id
                WHERE e.course_id = ? ORDER BY e.active DESC, s.last_name, s.first_name, s.id
                """, (r, n) -> new Enrollment(r.getLong("id"), r.getString("document"), r.getString("full_name"),
                r.getString("section"), r.getBoolean("active")), courseId);
    }

    @Transactional
    public void setEnrollment(long courseId, long studentId, boolean active) {
        lock();
        Course course = course(courseId);
        Boolean existing = db.query("""
                SELECT active FROM enrollment WHERE course_id = ? AND student_id = ?
                """, (r, n) -> r.getBoolean("active"), courseId, studentId).stream().findFirst().orElse(null);
        if (existing == null && !active) {
            throw missing();
        }
        if (Objects.equals(existing, active)) {
            return;
        }
        if (active && course.enrolled() >= course.capacity()) {
            throw conflict("El curso alcanzó su capacidad.");
        }
        if (existing == null) {
            db.update("INSERT INTO enrollment (active, course_id, student_id) VALUES (?, ?, ?)",
                    active, courseId, studentId);
        } else {
            db.update("UPDATE enrollment SET active = ? WHERE course_id = ? AND student_id = ?",
                    active, courseId, studentId);
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public EditableRoster<RosterRow> attendanceRoster(Account actor, long courseId, LocalDate date) {
        validateDate(date);
        requireCourseEditor(actor, courseId);
        return attendanceSnapshot(courseId, date);
    }

    @Transactional
    public void saveAttendance(Account actor, AttendanceForm form, String version) {
        validateDate(form.date());
        lock();
        requireCourseEditor(actor, form.courseId());
        var snapshot = attendanceSnapshot(form.courseId(), form.date());
        EditVersion.require(version, snapshot.version());
        var expected = new HashSet<>(snapshot.rows().stream().map(RosterRow::studentId).toList());
        for (AttendanceMark mark : form.marks()) {
            if (!expected.remove(mark.studentId())) {
                throw conflict("La lista contiene estudiantes duplicados o ajenos a esta asistencia.");
            }
        }
        if (!expected.isEmpty()) {
            throw conflict("Registra un estado para cada estudiante de la lista.");
        }
        db.update("DELETE FROM attendance WHERE course_id = ? AND on_date = ?", form.courseId(), form.date());
        db.batchUpdate("INSERT INTO attendance (course_id, on_date, student_id, status) VALUES (?, ?, ?, ?)",
                form.marks().stream().map(m -> new Object[] {form.courseId(), form.date(), m.studentId(),
                        m.status().name()}).toList());
    }

    @Transactional
    public void deleteAttendance(Account actor, long courseId, LocalDate date, long studentId, String version) {
        validateDate(date);
        lock();
        requireCourseEditor(actor, courseId);
        Status status = db.query("""
                SELECT status FROM attendance WHERE course_id = ? AND on_date = ? AND student_id = ?
                    AND status IS NOT NULL
                """, (r, n) -> Status.valueOf(r.getString("status")), courseId, date, studentId)
                .stream().findFirst().orElseThrow(ApiErrors::missing);
        EditVersion.require(version, EditVersion.forAttendanceMark(courseId, date, studentId, status));
        requireUpdated(db.update("""
                UPDATE attendance SET status = NULL WHERE course_id = ? AND on_date = ? AND student_id = ?
                """, courseId, date, studentId));
    }

    Page<AttendanceRow> attendance(Account actor, Long courseId, LocalDate from, LocalDate to, int page, int size) {
        return page(attendanceQuery(actor, courseId, from, to), page, size);
    }

    @Transactional(readOnly = true)
    public void exportAttendance(Account actor, Long courseId, LocalDate from, LocalDate to, Consumer<AttendanceRow> output) {
        stream(attendanceQuery(actor, courseId, from, to), output);
    }

    private ReportQuery<AttendanceRow> attendanceQuery(Account actor, Long courseId, LocalDate from, LocalDate to) {
        checkDates(from, to);
        Scope scope = recordScope(actor, "a");
        var params = new ArrayList<>(scope.values);
        String filters = historyFilters(params, courseId, from, to, "a");
        String sql = """
                SELECT c.id AS course_id, c.code, c.name AS course_name, s.id AS student_id,
                    s.first_name || ' ' || s.last_name AS full_name, a.on_date, a.status
                FROM attendance a JOIN course c ON c.id = a.course_id JOIN student s ON s.id = a.student_id
                WHERE
                """ + scope.condition + filters + """
                 AND a.status IS NOT NULL
                 ORDER BY a.on_date DESC, c.code, s.last_name, s.first_name, s.id
                """;
        return new ReportQuery<>(sql, params, AcademicStore::attendanceRow);
    }

    private static AttendanceRow attendanceRow(ResultSet r, int row) throws SQLException {
        long courseId = r.getLong("course_id");
        long studentId = r.getLong("student_id");
        LocalDate date = r.getObject("on_date", LocalDate.class);
        Status status = Status.valueOf(r.getString("status"));
        return new AttendanceRow(courseId, r.getString("code"), r.getString("course_name"), studentId,
                r.getString("full_name"), date, status, EditVersion.forAttendanceMark(courseId, date, studentId, status));
    }

    List<Timeslot> schedule(Account actor) {
        Scope scope = courseScope(actor);
        return db.query("""
                SELECT z.*, c.name AS course_name FROM schedule z JOIN course c ON c.id = z.course_id WHERE
                """ + scope.condition + " ORDER BY z.weekday, z.starts_at, c.code", TIMESLOT, scope.params());
    }

    @Transactional
    public long saveTimeslot(Long id, TimeslotForm f) {
        lock();
        if (!f.startsAt().isBefore(f.endsAt()) || f.startsAt().getSecond() != 0 || f.endsAt().getSecond() != 0
                || f.startsAt().getNano() != 0 || f.endsAt().getNano() != 0) {
            throw invalid("El inicio debe preceder al fin. Ingresa las horas en formato HH:mm.");
        }
        Course course = course(f.courseId());
        String room = f.room().strip();
        ensureAvailable(id == null ? 0 : id, course.id(), course.teacherId(), course.section(), f.weekday(),
                f.startsAt(), f.endsAt(), room);
        if (id == null) {
            return insert("INSERT INTO schedule (course_id, weekday, starts_at, ends_at, room) VALUES (?, ?, ?, ?, ?)",
                    f.courseId(), f.weekday(), f.startsAt(), f.endsAt(), room);
        }
        requireUpdated(db.update("""
                UPDATE schedule SET course_id = ?, weekday = ?, starts_at = ?, ends_at = ?, room = ? WHERE id = ?
                """, f.courseId(), f.weekday(), f.startsAt(), f.endsAt(), room, id));
        return id;
    }

    @Transactional
    public void deleteTimeslot(long id) {
        lock();
        requireUpdated(db.update("DELETE FROM schedule WHERE id = ?", id));
    }

    List<Assessment> assessments(Account actor) {
        Scope scope = courseScope(actor);
        return db.query("""
                SELECT a.*, c.name AS course_name FROM assessment a JOIN course c ON c.id = a.course_id WHERE
                """ + scope.condition + " ORDER BY a.on_date DESC, c.code, a.id", ASSESSMENT, scope.params());
    }

    @Transactional
    public long saveAssessment(Account actor, Long id, AssessmentForm f) {
        validateDate(f.date());
        lock();
        requireCourseEditor(actor, f.courseId());
        if (id == null) {
            return insert("INSERT INTO assessment (course_id, title, on_date) VALUES (?, ?, ?)",
                    f.courseId(), f.title().strip(), f.date());
        }
        Assessment previous = assessment(id);
        requireCourseEditor(actor, previous.courseId());
        if (previous.courseId() != f.courseId()) {
            throw invalid("Una evaluación conserva el curso con el que se creó.");
        }
        requireUpdated(db.update("UPDATE assessment SET title = ?, on_date = ? WHERE id = ?",
                f.title().strip(), f.date(), id));
        return id;
    }

    @Transactional
    public void deleteAssessment(Account actor, long id) {
        lock();
        Assessment item = assessment(id);
        requireCourseEditor(actor, item.courseId());
        requireUpdated(db.update("DELETE FROM assessment WHERE id = ?", id));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public EditableRoster<GradeRow> gradeRoster(Account actor, long id) {
        Assessment item = assessment(id);
        requireCourseEditor(actor, item.courseId());
        return gradeSnapshot(item);
    }

    @Transactional
    public String saveGrades(Account actor, long id, GradeForm form, String version) {
        lock();
        Assessment item = assessment(id);
        requireCourseEditor(actor, item.courseId());
        var snapshot = gradeSnapshot(item);
        EditVersion.require(version, snapshot.version());
        var expected = new HashSet<>(snapshot.rows().stream().map(GradeRow::studentId).toList());
        for (GradeMark mark : form.marks()) {
            if (!expected.remove(mark.studentId())) {
                throw conflict("La lista contiene estudiantes duplicados o ajenos a esta evaluación.");
            }
        }
        if (!expected.isEmpty()) {
            throw conflict("Envía una nota o un valor vacío para cada estudiante de la lista.");
        }
        db.update("DELETE FROM grade WHERE assessment_id = ?", id);
        if (!form.marks().isEmpty()) {
            var scores = form.marks().stream().map(m -> new Object[] {id, item.courseId(), m.studentId(), m.score()}).toList();
            db.batchUpdate("INSERT INTO grade (assessment_id, course_id, student_id, score) VALUES (?, ?, ?, ?)", scores);
        }
        return gradeSnapshot(item).version();
    }

    Page<GradeRow> grades(Account actor, Long courseId, LocalDate from, LocalDate to, int page, int size) {
        return page(gradesQuery(actor, courseId, from, to), page, size);
    }

    @Transactional(readOnly = true)
    public void exportGrades(Account actor, Long courseId, LocalDate from, LocalDate to, Consumer<GradeRow> output) {
        stream(gradesQuery(actor, courseId, from, to), output);
    }

    private ReportQuery<GradeRow> gradesQuery(Account actor, Long courseId, LocalDate from, LocalDate to) {
        checkDates(from, to);
        Scope scope = recordScope(actor, "g");
        var params = new ArrayList<>(scope.values);
        String filters = historyFilters(params, courseId, from, to, "a");
        String sql = """
                SELECT a.id, a.course_id, a.title, a.on_date, c.name AS course_name, s.id AS student_id,
                    s.first_name || ' ' || s.last_name AS student_name, g.score
                FROM grade g JOIN assessment a ON a.id = g.assessment_id JOIN course c ON c.id = a.course_id
                    JOIN student s ON s.id = g.student_id
                WHERE
                """ + scope.condition + filters + """
                 AND g.score IS NOT NULL
                 ORDER BY a.on_date DESC, c.code, a.id, s.last_name, s.id
                """;
        return new ReportQuery<>(sql, params, AcademicStore::gradeRow);
    }

    List<CourseOption> reportCourses(Account actor, boolean grades) {
        Scope scope = courseScope(actor);
        var params = new ArrayList<>(scope.values);
        String history = "";
        if (actor.role() == Role.STUDENT) {
            history = grades
                    ? " OR EXISTS (SELECT 1 FROM grade g WHERE g.course_id = c.id AND g.student_id = ? AND g.score IS NOT NULL)"
                    : " OR EXISTS (SELECT 1 FROM attendance a WHERE a.course_id = c.id AND a.student_id = ? AND a.status IS NOT NULL)";
            params.add(actor.studentId());
        }
        return db.query("SELECT c.id, c.code, c.name FROM course c WHERE (" + scope.condition + history + ") ORDER BY c.code",
                (r, n) -> new CourseOption(r.getLong("id"), r.getString("code"), r.getString("name")), params.toArray());
    }

    Page<LegacyRow> legacyAttendance(int page, int size) {
        return page(legacyQuery(), page, size);
    }

    @Transactional(readOnly = true)
    public void exportLegacyAttendance(Consumer<LegacyRow> output) {
        stream(legacyQuery(), output);
    }

    private static ReportQuery<LegacyRow> legacyQuery() {
        return new ReportQuery<>("SELECT * FROM legacy_attendance ORDER BY on_date DESC, student_name, id",
                List.of(), (r, n) -> new LegacyRow(r.getString("student_name"), r.getObject("on_date", LocalDate.class),
                        Status.valueOf(r.getString("status"))));
    }

    private <T> Page<T> page(ReportQuery<T> report, int page, int size) {
        if (page < 1 || size < 1 || size > 200) {
            throw invalid("La página debe ser positiva y el tamaño debe estar entre 1 y 200.");
        }
        var args = new ArrayList<>(report.parameters());
        args.add(size + 1);
        args.add((page - 1L) * size);
        List<T> rows = db.query(report.sql() + " LIMIT ? OFFSET ?", report.mapper(), args.toArray());
        boolean hasMore = rows.size() > size;
        return new Page<>(hasMore ? List.copyOf(rows.subList(0, size)) : rows, hasMore);
    }

    private <T> void stream(ReportQuery<T> report, Consumer<T> output) {
        try (var rows = db.queryForStream(connection -> {
            var statement = connection.prepareStatement(report.sql());
            statement.setFetchSize(256);
            for (int i = 0; i < report.parameters().size(); i++) {
                statement.setObject(i + 1, report.parameters().get(i));
            }
            return statement;
        }, report.mapper())) {
            rows.forEach(output);
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Summary summary(Account actor) {
        LocalDate today = LocalDate.now(zone);
        Scope courseScope = courseScope(actor);
        Scope records = recordScope(actor, "a");
        var parameters = new ArrayList<>(records.values);
        parameters.add(today);
        long[] figures = db.queryForObject("""
                SELECT COUNT(*) AS marked,
                    COALESCE(SUM(CASE WHEN a.status = 'PRESENTE' THEN 1 ELSE 0 END), 0) AS present
                FROM attendance a JOIN course c ON c.id = a.course_id WHERE
                """ + records.condition + " AND a.on_date = ? AND a.status IS NOT NULL",
                (r, n) -> new long[] {r.getLong("marked"), r.getLong("present")}, parameters.toArray());
        long courses = count("SELECT COUNT(*) FROM course c WHERE " + courseScope.condition, courseScope.params());
        long students = switch (actor.role()) {
            case ADMIN -> count("SELECT COUNT(*) FROM student");
            case TEACHER -> count("""
                    SELECT COUNT(DISTINCT e.student_id) FROM enrollment e JOIN course c ON c.id = e.course_id
                    WHERE c.teacher_id = ? AND e.active = TRUE
                    """, actor.teacherId());
            case STUDENT -> 1;
        };
        long teachers = actor.role() == Role.ADMIN ? count("SELECT COUNT(*) FROM teacher")
                : count("SELECT COUNT(DISTINCT c.teacher_id) FROM course c WHERE " + courseScope.condition,
                        courseScope.params());
        return new Summary(today, courses, students, teachers, figures[0], figures[1],
                figures[0] == 0 ? null : (int) Math.round(100.0 * figures[1] / figures[0]));
    }

    private EditableRoster<RosterRow> attendanceSnapshot(long courseId, LocalDate date) {
        List<RosterRow> rows = db.query("""
                SELECT s.id, s.first_name || ' ' || s.last_name AS student_name, a.status
                FROM enrollment e JOIN student s ON s.id = e.student_id
                LEFT JOIN attendance a ON a.course_id = e.course_id AND a.student_id = e.student_id AND a.on_date = ?
                WHERE e.course_id = ? AND (a.student_id IS NOT NULL OR (
                    e.active = TRUE AND NOT EXISTS (SELECT 1 FROM attendance saved
                        WHERE saved.course_id = e.course_id AND saved.on_date = ?)
                ))
                ORDER BY s.last_name, s.first_name, s.id
                """, (r, n) -> new RosterRow(r.getLong("id"), r.getString("student_name"),
                        r.getString("status") == null ? null : Status.valueOf(r.getString("status"))), date, courseId, date);
        String version = EditVersion.forRoster("attendance/" + courseId + "/" + date,
                rows.stream().sorted(Comparator.comparingLong(RosterRow::studentId))
                        .map(r -> r.studentId() + ":" + r.status() + ":" + r.studentName()));
        return new EditableRoster<>(rows, version);
    }

    private EditableRoster<GradeRow> gradeSnapshot(Assessment item) {
        List<GradeRow> rows = db.query("""
                SELECT a.id, a.course_id, a.title, a.on_date, c.name AS course_name, s.id AS student_id,
                    s.first_name || ' ' || s.last_name AS student_name, g.score
                FROM assessment a JOIN course c ON c.id = a.course_id JOIN enrollment e ON e.course_id = c.id
                JOIN student s ON s.id = e.student_id
                LEFT JOIN grade g ON g.assessment_id = a.id AND g.student_id = s.id
                WHERE a.id = ? AND (e.active = TRUE OR g.student_id IS NOT NULL)
                ORDER BY s.last_name, s.first_name, s.id
                """, AcademicStore::gradeRow, item.id());
        String version = EditVersion.forRoster("grades/" + item.id() + "/" + item.date(),
                Stream.concat(Stream.of(item.title(), item.courseName()),
                        rows.stream().sorted(Comparator.comparingLong(GradeRow::studentId))
                                .map(r -> r.studentId() + ":" + r.score() + ":" + r.studentName())));
        return new EditableRoster<>(rows, version);
    }

    private static GradeRow gradeRow(ResultSet r, int row) throws SQLException {
        return new GradeRow(r.getLong("id"), r.getString("title"), r.getLong("course_id"), r.getString("course_name"),
                r.getLong("student_id"), r.getString("student_name"), r.getObject("on_date", LocalDate.class),
                r.getBigDecimal("score"));
    }

    private static String historyFilters(List<Object> values, Long courseId, LocalDate from, LocalDate to, String alias) {
        var filter = new StringBuilder();
        if (courseId != null) {
            if (courseId <= 0) {
                throw invalid("Selecciona un curso válido.");
            }
            filter.append(" AND c.id = ?");
            values.add(courseId);
        }
        if (from != null) {
            filter.append(" AND ").append(alias).append(".on_date >= ?");
            values.add(from);
        }
        if (to != null) {
            filter.append(" AND ").append(alias).append(".on_date <= ?");
            values.add(to);
        }
        return filter.toString();
    }

    static void checkDates(LocalDate from, LocalDate to) {
        if (from != null) {
            validateDate(from);
        }
        if (to != null) {
            validateDate(to);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw invalid("La fecha inicial debe ser anterior o igual a la final.");
        }
    }

    static void validateDate(LocalDate date) {
        if (date.getYear() < 1 || date.getYear() > 9999) {
            throw invalid("El año de la fecha debe estar entre 0001 y 9999.");
        }
    }

    private static Scope courseScope(Account actor) {
        return switch (actor.role()) {
            case ADMIN -> new Scope("1 = 1", List.of());
            case TEACHER -> new Scope("c.teacher_id = ?", List.of(actor.teacherId()));
            case STUDENT -> new Scope("""
                    EXISTS (SELECT 1 FROM enrollment scope_e WHERE scope_e.course_id = c.id
                        AND scope_e.student_id = ? AND scope_e.active = TRUE)
                    """, List.of(actor.studentId()));
        };
    }

    private static Scope recordScope(Account actor, String alias) {
        if (actor.role() == Role.STUDENT) {
            return new Scope(alias + ".student_id = ?", List.of(actor.studentId()));
        }
        return courseScope(actor);
    }

    private Course requireCourseEditor(Account actor, long id) {
        if (actor.role() == Role.STUDENT) {
            throw denied();
        }
        Course course = course(id);
        if (actor.role() == Role.TEACHER && !Objects.equals(actor.teacherId(), course.teacherId())) {
            throw denied();
        }
        return course;
    }

    private Course course(long id) {
        return db.query(COURSES + " WHERE c.id = ?", COURSE, id).stream().findFirst().orElseThrow(ApiErrors::missing);
    }

    private Assessment assessment(long id) {
        return db.query("""
                SELECT a.*, c.name AS course_name FROM assessment a JOIN course c ON c.id = a.course_id WHERE a.id = ?
                """, ASSESSMENT, id).stream().findFirst().orElseThrow(ApiErrors::missing);
    }

    private List<Timeslot> scheduleForCourse(long id) {
        return db.query("""
                SELECT z.*, c.name AS course_name FROM schedule z JOIN course c ON c.id = z.course_id WHERE c.id = ?
                """, TIMESLOT, id);
    }

    private void ensureAvailable(long id, long courseId, long teacherId, String section, int day,
                                 LocalTime start, LocalTime end, String room) {
        if (count("""
                SELECT COUNT(*) FROM schedule z JOIN course c ON c.id = z.course_id
                WHERE z.id <> ? AND z.weekday = ? AND z.starts_at < ? AND z.ends_at > ?
                  AND (z.course_id = ? OR c.teacher_id = ? OR LOWER(c.section) = LOWER(?) OR LOWER(z.room) = LOWER(?))
                """, id, day, end, start, courseId, teacherId, section, room) > 0) {
            throw conflict("El horario coincide con otro del mismo curso, profesor, sección o aula.");
        }
    }

    void lock() {
        db.queryForObject("SELECT id FROM write_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    long count(String sql, Object... arguments) {
        return db.queryForObject(sql, Long.class, arguments);
    }

    private long insert(String sql, Object... arguments) {
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] {"id"});
            for (int i = 0; i < arguments.length; i++) {
                statement.setObject(i + 1, arguments[i]);
            }
            return statement;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    private static void requireUpdated(int rows) {
        if (rows == 0) {
            throw missing();
        }
    }

    private record ReportQuery<T>(String sql, List<Object> parameters, RowMapper<T> mapper) {}

    private record Scope(String condition, List<Object> values) {
        Object[] params() {
            return values.toArray();
        }
    }
}
