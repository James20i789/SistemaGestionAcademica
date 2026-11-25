package com.gestionacademico.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.gestionacademico.web.AcademicData.*;
import static com.gestionacademico.web.ApiErrors.invalid;

@RestController
@RequestMapping("/api/legacy")
class LegacyImport {
    private final JdbcTemplate db;
    private final AcademicStore store;
    private final Validator validator;

    LegacyImport(JdbcTemplate db, AcademicStore store, Validator validator) {
        this.db = db;
        this.store = store;
        this.validator = validator;
    }

    @PostMapping
    @Transactional
    public LegacyResult importFiles(@AuthenticationPrincipal Accounts.SessionUser user,
                                    @Valid @RequestBody LegacyForm input) {
        AcademicApi.admin(user.account());
        store.lock();
        int students = 0;
        int teachers = 0;
        int courses = 0;
        int attendance = 0;
        for (Line line : parse("Estudiantes.txt", input.students(), 5)) {
            String[] f = line.fields;
            if (store.count("SELECT COUNT(*) FROM student WHERE document = ?", f[0]) == 0) {
                store.saveStudent(null, verified(line, new StudentForm(f[0], f[1], f[2], f[3], f[4])));
                students++;
            }
        }
        for (Line line : parse("Profesor.txt", input.teachers(), 5)) {
            String[] f = line.fields;
            if (store.count("SELECT COUNT(*) FROM teacher WHERE document = ?", f[0]) == 0) {
                store.saveTeacher(null, verified(line, new TeacherForm(f[0], f[1], f[2], f[3], f[4])));
                teachers++;
            }
        }
        List<Line> legacyCourses = parse("Cursos.txt", input.courses(), 4);
        var documents = new HashMap<String, Long>();
        var normalizedDocuments = new HashMap<String, List<Long>>();
        var names = new HashMap<String, List<Long>>();
        if (!legacyCourses.isEmpty()) {
            for (Teacher teacher : store.teachers()) {
                documents.put(teacher.document(), teacher.id());
                normalizedDocuments.computeIfAbsent(normalize(teacher.document()), key -> new ArrayList<>()).add(teacher.id());
                names.computeIfAbsent(normalize(teacher.firstName() + " " + teacher.lastName()), key -> new ArrayList<>())
                        .add(teacher.id());
            }
        }
        for (Line line : legacyCourses) {
            String[] f = line.fields;
            if (store.count("SELECT COUNT(*) FROM course WHERE code = ?", f[0].toUpperCase(Locale.ROOT)) > 0) {
                continue;
            }
            Long teacherId = documents.get(f[2]);
            if (teacherId == null) {
                String legacyTeacher = normalize(f[2]);
                List<Long> candidates = normalizedDocuments.getOrDefault(legacyTeacher,
                        names.getOrDefault(legacyTeacher, List.of()));
                if (candidates.size() != 1) {
                    throw invalid(line.label() + " indica el documento de un profesor registrado en el campo de profesor.");
                }
                teacherId = candidates.getFirst();
            }
            int capacity;
            try {
                capacity = Integer.parseInt(f[3]);
            } catch (NumberFormatException ex) {
                throw invalid(line.label() + " ingresa una capacidad numérica.");
            }
            store.saveCourse(null, verified(line, new CourseForm(f[0], f[1], teacherId,
                    capacity, "Por asignar")));
            courses++;
        }
        for (Line line : parse("Asistencias.txt", input.attendance(), 3)) {
            String[] f = line.fields;
            LocalDate date;
            Status status;
            try {
                date = LocalDate.parse(f[1]);
                if (date.getYear() < 1 || date.getYear() > 9999) {
                    throw new IllegalArgumentException();
                }
                status = Status.valueOf(normalize(f[2]).toUpperCase(Locale.ROOT));
            } catch (DateTimeParseException | IllegalArgumentException ex) {
                throw invalid(line.label() + " la fecha o el estado tienen un formato inválido.");
            }
            if (f[0].isBlank() || f[0].length() > 201) {
                throw invalid(line.label() + " revisa el nombre del estudiante.");
            }
            if (store.count("SELECT COUNT(*) FROM legacy_attendance WHERE student_name = ? AND on_date = ? AND status = ?",
                    f[0], date, status.name()) == 0) {
                db.update("INSERT INTO legacy_attendance (student_name, on_date, status) VALUES (?, ?, ?)",
                        f[0], date, status.name());
                attendance++;
            }
        }
        return new LegacyResult(students, teachers, courses, attendance);
    }

    @GetMapping
    ResponseEntity<List<LegacyRow>> attendance(@AuthenticationPrincipal Accounts.SessionUser user,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "100") int size) {
        AcademicApi.admin(user.account());
        return AcademicApi.page(store.legacyAttendance(page, size));
    }

    @GetMapping("/attendance.csv")
    void csv(@AuthenticationPrincipal Accounts.SessionUser user, HttpServletResponse response) throws IOException {
        AcademicApi.admin(user.account());
        var csv = new CsvExport(response, "asistencias-historicas.csv", "Fecha,Estudiante,Estado");
        store.exportLegacyAttendance(row -> csv.row(row.date(), row.studentName(), row.status()));
        csv.flush();
    }

    private <T> T verified(Line line, T form) {
        if (!validator.validate(form).isEmpty()) {
            throw invalid(line.label() + " revisa los datos obligatorios y su formato.");
        }
        return form;
    }

    private static List<Line> parse(String name, String contents, int columns) {
        if (contents == null || contents.isBlank()) {
            return List.of();
        }
        var result = new ArrayList<Line>();
        String[] rows = contents.replaceFirst("^\uFEFF", "").split("\\R");
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            String[] fields = Arrays.stream(rows[i].split(",", -1)).map(String::strip).toArray(String[]::new);
            var line = new Line(name, i + 1, fields);
            if (rows[i].indexOf(0) >= 0 || rows[i].indexOf('\uFFFD') >= 0) {
                throw invalid(line.label() + " revisa la codificación del archivo y guárdalo en UTF-8.");
            }
            if (fields.length != columns) {
                throw invalid(line.label() + " se esperaban " + columns + " campos separados por comas.");
            }
            result.add(line);
        }
        return result;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .strip().toLowerCase(Locale.ROOT);
    }

    private record Line(String name, int number, String[] fields) {
        String label() {
            return name + ", fila " + number + ".";
        }
    }

}
