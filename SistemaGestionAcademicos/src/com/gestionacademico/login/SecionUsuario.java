package com.gestionacademico.login;

/**
 * SecionUsuario
 */
import com.gestionacademico.model.Usuario;
public class SecionUsuario {
    private static SecionUsuario instancia;
    private Usuario usuarioActual;

    private SecionUsuario() {
        // Constructor privado para evitar instanciación externa
    }

    public static SecionUsuario get() {
        if (instancia == null) {
            instancia = new SecionUsuario();
        }
        return instancia;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }
}
