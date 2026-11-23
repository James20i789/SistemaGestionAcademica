package com.gestionacademico.model;
import java.time.LocalDate;

public class Asistencia {
    private String estudiante;
    private LocalDate fecha;
    private String estado;
    public Asistencia( String estudiante, LocalDate fecha, String estado) {
        this.estudiante = estudiante;
        this.fecha = fecha;
        this.estado = estado;
    }

    public String getEstudiante() {
        return estudiante;
    }
    public void setEstudiante(String estudiante) {
        this.estudiante = estudiante;
    }
    public LocalDate getFecha() {
        return fecha; }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha; }
    public String getEstado() { return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
  @Override
    public String toString(){
       return estudiante+","+fecha+","+estado;
  }
}
