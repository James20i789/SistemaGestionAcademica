package com.gestionacademico.model;

public class Usuario {
    private String nombre;
    private String correo;
    private String clave;
    private String rol;

    public Usuario(String nombre,String correo ,String clave,String rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.clave = clave;
        this.rol = rol;
    }

    public String getNombre() {  
    	return nombre;  
    	}
    public void setNombre(String nombre) { this.nombre = nombre; }

	public String getCorreo() {
		return correo;
	}
    public void setCorreo(String correo) { this.correo = correo; }
    
	public String getClave() {
		return clave;
	}
    public void setClave(String clave) { this.clave = clave; }
  
    public String getRol() {  
    	return rol;  
    }
    public void setRol(String rol) { this.rol = rol; }
   
    @Override
    public String toString() {
        return "Usuario{" +
                "nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", clave='" + clave + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }

}
