package com.gestionacademico.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CsvExport {
    private final BufferedWriter writer;

    CsvExport(HttpServletResponse response, String filename, String header) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write('\uFEFF');
        writer.write(header);
        writer.write("\r\n");
    }

    void row(Object... fields) {
        try {
            writer.write(Stream.of(fields).map(CsvExport::cell).collect(Collectors.joining(",")));
            writer.write("\r\n");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    void flush() throws IOException {
        writer.flush();
    }

    private static String cell(Object value) {
        String text = value.toString();
        String stripped = text.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
