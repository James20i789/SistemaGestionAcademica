package com.gestionacademico.controller;

import com.gestionacademico.model.Asistencia;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ControllerAsistencia {
    private static final String LOCAL_APP_DATA = System.getenv("LOCALAPPDATA");
    private static final Path DATA_DIRECTORY = Path.of(
            LOCAL_APP_DATA == null || LOCAL_APP_DATA.isBlank()
                    ? System.getProperty("user.home")
                    : LOCAL_APP_DATA,
            "SistemaGestionAcademica");
    private static final Path ARCHIVO = DATA_DIRECTORY.resolve("Asistencias.txt");
    private static final Path ARCHIVO_ANTERIOR = Path.of("Asistencias.txt").toAbsolutePath();

    public void registrarAsistencia(Asistencia asistencia) throws IOException {
        registrarAsistencias(List.of(asistencia));
    }

    public void registrarAsistencias(List<Asistencia> asistencias) throws IOException {
        prepararArchivo();
        try (BufferedWriter escritor = Files.newBufferedWriter(
                ARCHIVO,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE)) {
            for (Asistencia asistencia : asistencias) {
                escritor.write(asistencia.toString());
                escritor.newLine();
            }
        }
    }

    public ArrayList<Asistencia> listarAsistencias() throws IOException {
        ArrayList<Asistencia> asistencias = new ArrayList<>();
        prepararArchivo();
        if (!Files.exists(ARCHIVO)) {
            return asistencias;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split(",", -1);
                if (datos.length != 3) {
                    throw new IOException("Formato inválido en " + ARCHIVO.toAbsolutePath()
                            + ", línea " + numeroLinea + ". Se esperaban tres campos separados por comas: "
                            + linea);
                }

                try {
                    asistencias.add(new Asistencia(datos[0].trim(), LocalDate.parse(datos[1].trim()),
                            datos[2].trim()));
                } catch (DateTimeParseException e) {
                    throw new IOException("Fecha inválida en " + ARCHIVO.toAbsolutePath()
                            + ", línea " + numeroLinea + ": " + linea, e);
                }
            }
        }

        return asistencias;
    }

    public boolean eliminarAsistencia(Asistencia asistencia) throws IOException {
        ArrayList<Asistencia> asistencias = listarAsistencias();
        int indice = -1;

        for (int i = 0; i < asistencias.size(); i++) {
            Asistencia actual = asistencias.get(i);
            if (actual.getEstudiante().equals(asistencia.getEstudiante())
                    && actual.getFecha().equals(asistencia.getFecha())
                    && actual.getEstado().equals(asistencia.getEstado())) {
                indice = i;
                break;
            }
        }

        if (indice == -1) {
            return false;
        }

        asistencias.remove(indice);
        ArrayList<String> lineas = new ArrayList<>();
        for (Asistencia actual : asistencias) {
            lineas.add(actual.toString());
        }

        Files.write(ARCHIVO, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return true;
    }

    private void prepararArchivo() throws IOException {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            if (!Files.exists(ARCHIVO)
                    && !ARCHIVO_ANTERIOR.equals(ARCHIVO.toAbsolutePath())
                    && Files.exists(ARCHIVO_ANTERIOR)) {
                Files.copy(ARCHIVO_ANTERIOR, ARCHIVO);
            }
        } catch (IOException e) {
            throw new IOException("No se pudo preparar el archivo de asistencias en "
                    + ARCHIVO.toAbsolutePath() + ". Verifica los permisos de esa carpeta.", e);
        }
    }
}
