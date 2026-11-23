package com.gestionacademico.model;

public class Estudiantes {

	private int documento;
	private String nombre;
	private String apellidos;
	private String generos;
	private String seccion;

	public Estudiantes() {

	}

	public Estudiantes(int documento, String nombre, String apellidos, String generos, String seccion) {
		this.documento = documento;
		this.nombre = nombre;
		this.apellidos = apellidos;
		this.generos = generos;
		this.seccion = seccion;
	}

	public int getDocumento() {
		return documento;
	}

	public void setDocumento(int documento) {
		this.documento = documento;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellidos() {
		return apellidos;
	}

	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	public String getGeneros() {
		return generos;
	}

	public void setGeneros(String generos) {
		this.generos = generos;
	}

	public String getSeccion() {
		return seccion;
	}

	public void setSeccion(String seccion) {
		this.seccion = seccion;
	}
}
