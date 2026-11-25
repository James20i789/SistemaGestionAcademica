package com.gestionacademico.web;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.gestionacademico.web.AcademicData.*;
import static com.gestionacademico.web.ApiErrors.*;

@Service
class Accounts implements UserDetailsService, ApplicationRunner {
    private static final String SELECT = """
            SELECT id, email, name, password_hash, role, enabled, session_version, teacher_id, student_id
            FROM account
            """;
    private static final RowMapper<Credentials> ROW = (r, n) -> new Credentials(new Account(
            r.getLong("id"), r.getString("email"), r.getString("name"), Role.valueOf(r.getString("role")),
            r.getBoolean("enabled"), r.getObject("teacher_id", Long.class), r.getObject("student_id", Long.class)),
            r.getString("password_hash"), r.getLong("session_version"));

    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final String initialEmail;
    private final String initialPassword;

    Accounts(JdbcTemplate db, PasswordEncoder encoder, Validator validator,
             @Value("${academia.admin.email}") String initialEmail,
             @Value("${academia.admin.password}") String initialPassword) {
        this.db = db;
        this.encoder = encoder;
        this.validator = validator;
        this.initialEmail = initialEmail;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        lock();
        if (countAdmins() > 0) {
            return;
        }
        var form = new AccountForm(initialEmail.strip(), "Administrador", Role.ADMIN, initialPassword,
                null, null, true);
        if (!validator.validate(form).isEmpty() || initialPassword.isBlank()) {
            throw new IllegalStateException("Configura APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD para la primera instalación.");
        }
        validatePassword(initialPassword);
        insert(form);
    }

    @Override
    public SessionUser loadUserByUsername(String login) {
        return db.query(SELECT + " WHERE email = ?", ROW, email(login)).stream()
                .map(SessionUser::new).findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    boolean isCurrent(SessionUser user) {
        return db.queryForObject("""
                SELECT COUNT(*) FROM account WHERE id = ? AND enabled = TRUE AND session_version = ?
                """, Long.class, user.account().id(), user.sessionVersion()) == 1;
    }

    List<Account> list() {
        return db.query(SELECT + " ORDER BY name, id", ROW).stream().map(Credentials::account).toList();
    }

    @Transactional
    public void create(AccountForm form) {
        lock();
        validateProfile(form);
        validatePassword(form.password());
        insert(form);
    }

    @Transactional
    public void update(long id, long actorId, AccountForm form) {
        lock();
        Credentials stored = find(id);
        validateProfile(form);
        if (id == actorId) {
            throw conflict("Edita tu contraseña desde Mi cuenta. Otro administrador puede modificar esta cuenta.");
        }
        if (stored.account().enabled() && stored.account().role() == Role.ADMIN
                && (!form.enabled() || form.role() != Role.ADMIN) && countAdmins() == 1) {
            throw conflict("Debe quedar al menos un administrador activo.");
        }
        String hash = stored.hash();
        if (form.password() != null && !form.password().isEmpty()) {
            validatePassword(form.password());
            hash = encoder.encode(form.password());
        }
        db.update("""
                UPDATE account SET email = ?, name = ?, password_hash = ?, role = ?, enabled = ?,
                    teacher_id = ?, student_id = ?, session_version = session_version + 1
                WHERE id = ?
                """, email(form.email()), form.name().strip(), hash, form.role().name(),
                form.enabled(), form.teacherId(), form.studentId(), id);
    }

    @Transactional
    public void changePassword(long id, ProfilePassword form) {
        lock();
        Credentials stored = find(id);
        if (form.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(form.currentPassword(), stored.hash())) {
            throw invalid("La contraseña actual es incorrecta.");
        }
        validatePassword(form.newPassword());
        db.update("UPDATE account SET password_hash = ?, session_version = session_version + 1 WHERE id = ?",
                encoder.encode(form.newPassword()), id);
    }

    private Credentials find(long id) {
        return db.query(SELECT + " WHERE id = ?", ROW, id).stream().findFirst().orElseThrow(ApiErrors::missing);
    }

    private void insert(AccountForm form) {
        db.update("""
                INSERT INTO account (email, name, password_hash, role, enabled, teacher_id, student_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, email(form.email()), form.name().strip(), encoder.encode(form.password()), form.role().name(),
                form.enabled(), form.teacherId(), form.studentId());
    }

    private void lock() {
        db.queryForObject("SELECT id FROM write_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    private long countAdmins() {
        return db.queryForObject("SELECT COUNT(*) FROM account WHERE role = 'ADMIN' AND enabled = TRUE", Long.class);
    }

    private static void validateProfile(AccountForm form) {
        boolean expectedTeacher = form.role() == Role.TEACHER;
        boolean expectedStudent = form.role() == Role.STUDENT;
        if (expectedTeacher != Objects.nonNull(form.teacherId()) || expectedStudent != Objects.nonNull(form.studentId())) {
            throw invalid("Vincula la cuenta con el profesor o estudiante que corresponda a su rol.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("La contraseña debe tener al menos 12 caracteres y ocupar como máximo 72 bytes.");
        }
    }

    static String email(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private record Credentials(Account account, String hash, long version) {}

    static final class SessionUser extends User {
        private final Account account;
        private final long sessionVersion;

        private SessionUser(Credentials credentials) {
            super(credentials.account().email(), credentials.hash(), credentials.account().enabled(),
                    true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + credentials.account().role().name())));
            this.account = credentials.account();
            this.sessionVersion = credentials.version();
        }

        Account account() {
            return account;
        }

        long sessionVersion() {
            return sessionVersion;
        }
    }
}
