package com.gestionacademico.model;

public class Curso {
    private String codigo;
    private String nombre;
    private String profesor;
    private int capacidad;

    public Curso(String codigo, String nombre, String profesor, int capacidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.profesor = profesor;
        this.capacidad = capacidad;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getProfesor() { return profesor; }
    public void setProfesor(String profesor) { this.profesor = profesor; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
}