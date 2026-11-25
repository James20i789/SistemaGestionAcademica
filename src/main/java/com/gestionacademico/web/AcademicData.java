package com.gestionacademico.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

final class AcademicData {
    enum Role { ADMIN, TEACHER, STUDENT }
    enum Status { PRESENTE, AUSENTE, TARDANZA, JUSTIFICACION }

    record Student(long id, String document, String firstName, String lastName, String gender, String section) {}
    record StudentForm(@NotBlank @Size(max = 40) String document,
                       @NotBlank @Size(max = 100) String firstName,
                       @NotBlank @Size(max = 100) String lastName,
                       @NotBlank @Size(max = 30) String gender,
                       @NotBlank @Size(max = 40) String section) {}

    record Teacher(long id, String document, String firstName, String lastName, String specialty, String email) {}
    record TeacherForm(@NotBlank @Size(max = 40) String document,
                       @NotBlank @Size(max = 100) String firstName,
                       @NotBlank @Size(max = 100) String lastName,
                       @NotBlank @Size(max = 120) String specialty,
                       @NotBlank @Email @Size(max = 254) String email) {}

    record Course(long id, String code, String name, long teacherId, String teacherName,
                  int capacity, String section, int enrolled) {}
    record CourseForm(@NotBlank @Size(max = 40) String code,
                      @NotBlank @Size(max = 160) String name,
                      @Positive long teacherId,
                      @Min(1) @Max(5000) int capacity,
                      @NotBlank @Size(max = 40) String section) {}

    record CourseOption(long id, String code, String name) {}
    record Page<T>(List<T> rows, boolean hasMore) {}
    record EditableRoster<T>(List<T> rows, String version) {}

    record Enrollment(long studentId, String document, String studentName, String section, boolean active) {}
    record EnrollmentForm(@NotNull Boolean active) {}
    record AttendanceRow(long courseId, String courseCode, String courseName, long studentId, String studentName,
                         LocalDate date, Status status, String version) {}
    record RosterRow(long studentId, String studentName, Status status) {}
    record AttendanceForm(@Positive long courseId, @NotNull LocalDate date,
                          @NotEmpty @Size(max = 5000) List<@NotNull @Valid AttendanceMark> marks) {}
    record AttendanceMark(@Positive long studentId, @NotNull Status status) {}

    record Timeslot(long id, long courseId, String courseName, int weekday, LocalTime startsAt, LocalTime endsAt,
                    String room) {}
    record TimeslotForm(@Positive long courseId, @Min(1) @Max(7) int weekday, @NotNull LocalTime startsAt,
                        @NotNull LocalTime endsAt, @NotBlank @Size(max = 100) String room) {}

    record Assessment(long id, long courseId, String courseName, String title, LocalDate date) {}
    record AssessmentForm(@Positive long courseId, @NotBlank @Size(max = 160) String title,
                          @NotNull LocalDate date) {}
    record GradeRow(long assessmentId, String assessment, long courseId, String courseName, long studentId,
                    String studentName, LocalDate date, BigDecimal score) {}
    record GradeForm(@NotNull List<@NotNull @Valid GradeMark> marks) {}
    record GradeMark(@Positive long studentId,
                     @DecimalMin("0.00") @DecimalMax("20.00") @Digits(integer = 2, fraction = 2) BigDecimal score) {}

    record Account(long id, String email, String name, Role role, boolean enabled, Long teacherId, Long studentId) {}
    record AccountForm(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 160) String name,
                       @NotNull Role role, @Size(max = 72) String password, @Positive Long teacherId,
                       @Positive Long studentId, @NotNull Boolean enabled) {}
    record ProfilePassword(@NotBlank @Size(max = 72) String currentPassword,
                           @NotBlank @Size(max = 72) String newPassword) {}
    record Summary(LocalDate date, long courses, long students, long teachers, long marked, long present,
                   Integer attendancePercent) {}
    record LegacyForm(@Size(max = 500000) String students, @Size(max = 500000) String teachers,
                      @Size(max = 500000) String courses, @Size(max = 500000) String attendance) {}
    record LegacyResult(int students, int teachers, int courses, int attendance) {}
    record LegacyRow(String studentName, LocalDate date, Status status) {}
}
