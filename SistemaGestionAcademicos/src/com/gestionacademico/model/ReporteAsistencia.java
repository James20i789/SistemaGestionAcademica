package com.gestionacademico.model;

import java.util.ArrayList;

public class ReporteAsistencia {

    private ArrayList<Asistencia> listaAsistencias;

    public ReporteAsistencia(ArrayList<Asistencia> listaAsistencias) {
        this.listaAsistencias = new ArrayList<>();
    }

    public ArrayList<Asistencia> getListaAsistencias() {
        return listaAsistencias;
    }

    public int contarPresente() {
        int conteo = 0;
        for (Asistencia a : listaAsistencias) {
            if (a.getEstado().equals("Presente")) {
                conteo++;
            }
        }
        return conteo;
    }

    public int contarAusente() {
        int conteo = 0;
        for (Asistencia a : listaAsistencias) {
            if (a.getEstado().equals("Asistente")) {
                conteo++;
            }
        }
        return conteo;
    }

    public int contarTardanzas() {
        int contador = 0;

        for (Asistencia a : listaAsistencias) {
            if (a.getEstado().equalsIgnoreCase("Tardanza")) {
                contador++;
            }
        }

        return contador;
    }

    public double calcularAsistencias() {
        int total = listaAsistencias.size();
        if (total == 0) {
            return 0;
        }
        int presente = contarPresente();
        int ausente = contarAusente();
        int tardanzas = contarTardanzas();
        int i = (presente + ausente + tardanzas * 100) / total;
        return i;

    }


}
