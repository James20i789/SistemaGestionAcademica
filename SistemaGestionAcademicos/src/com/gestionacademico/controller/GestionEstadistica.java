package com.gestionacademico.controller;

import com.gestionacademico.model.Asistencia;
import com.gestionacademico.model.Curso;
import com.gestionacademico.model.Estudiantes;
import com.gestionacademico.model.Profesor;

import java.io.IOException;
import java.time.LocalDate;

public class GestionEstadistica {
    private  ControllerAsistencia controllerAsistencia = new ControllerAsistencia();
    private  ControllerCurso controllerCurso = new ControllerCurso();
    private  ControllerEstudiante controllerEstudiante = new ControllerEstudiante();
    private  ControllerProfesor controllerProfesor = new ControllerProfesor();

    public int contarCursos() {
        int cantidad = 0;
        for (Curso curso : controllerCurso.listarCursos()) {
            if (curso != null) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public int contarEstudiantes() {
        int cantidad = 0;
        for (Estudiantes estudiante : controllerEstudiante.listarDatos()) {
            if (estudiante != null) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public int calcularAsistenciaHoy() throws IOException {
        int total = 0;
        int presentes = 0;
        LocalDate hoy = LocalDate.now();

        for (Asistencia asistencia : controllerAsistencia.listarAsistencias()) {
            if (asistencia.getFecha().equals(hoy)) {
                total++;
                if ("Presente".equalsIgnoreCase(asistencia.getEstado().trim())) {
                    presentes++;
                }
            }
        }

        return total == 0 ? 0 : (int) Math.round(presentes * 100.0 / total);
    }

    public int contarProfesores() {
        int cantidad = 0;
        for (Profesor profesor : controllerProfesor.listarProfesor()) {
            if (profesor != null) {
                cantidad++;
            }
        }
        return cantidad;
    }

}
