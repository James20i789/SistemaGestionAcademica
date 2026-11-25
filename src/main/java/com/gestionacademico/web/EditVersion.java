package com.gestionacademico.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.stream.Stream;

import static com.gestionacademico.web.AcademicData.Status;

final class EditVersion {
    private EditVersion() {}

    static String forRoster(String resource, Stream<String> entries) {
        var source = new StringBuilder(resource);
        entries.forEach(entry -> source.append('\n').append(entry.length()).append(':').append(entry));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(hash) + "\"";
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String forAttendanceMark(long courseId, LocalDate date, long studentId, Status status) {
        return "\"attendance/" + courseId + "/" + date + "/" + studentId + "/" + status + "\"";
    }

    static void require(String supplied, String current) {
        if (supplied == null || supplied.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Carga la versión actual antes de guardar o eliminar.");
        }
        if (!current.equals(supplied.strip())) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Otra persona cambió estos datos. Vuelve a cargarlos y revisa tus cambios.");
        }
    }
}
