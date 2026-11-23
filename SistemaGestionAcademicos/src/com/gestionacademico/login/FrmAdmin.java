package com.gestionacademico.login;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.gestionacademico.model.Usuario;

public class FrmAdmin extends JFrame {
    private JButton btnVerPerfil;
    private JButton btnEditarDatos;
    private JButton btnConfiguracionSistema;
    private JMenuItem menuGestionUsuarios;
    private Usuario usuarioActual;

    public FrmAdmin() {
        this(null);
    }

    public FrmAdmin(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        setTitle("Sistema de Escritorio");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());
        setLocationRelativeTo(null);

        btnVerPerfil = new JButton("Ver Perfil");
        btnEditarDatos = new JButton("Editar Datos");
        btnConfiguracionSistema = new JButton("Configuración Avanzada");

        add(btnVerPerfil);
        add(btnEditarDatos);
        add(btnConfiguracionSistema);

        aplicarPermisos();
    }

    private void aplicarPermisos() {
        Usuario user = (usuarioActual != null) ? usuarioActual : SecionUsuario.get().getUsuarioActual();

        if (user == null || user.getRol() == null || user.getRol().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay sesión activa o el rol es inválido.", "Sesión",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
            return;
        }

        String rol = user.getRol().trim().toUpperCase();

        switch (rol) {
            case "ADMINISTRADOR":
                btnVerPerfil.setVisible(true);
                btnEditarDatos.setVisible(true);
                btnConfiguracionSistema.setVisible(true);
                break;

            case "SUPER_USUARIO":
                btnVerPerfil.setVisible(true);
                btnEditarDatos.setVisible(true);
                btnConfiguracionSistema.setVisible(false);
                break;

            case "USUARIO":
                btnVerPerfil.setVisible(true);
                btnEditarDatos.setVisible(false);
                btnConfiguracionSistema.setVisible(false);
                break;

            default:
                JOptionPane.showMessageDialog(this, "Rol no reconocido: " + rol, "Acceso",
                        JOptionPane.WARNING_MESSAGE);
                btnVerPerfil.setVisible(true);
                btnEditarDatos.setVisible(true);
                btnConfiguracionSistema.setVisible(true);
                break;
        }
    }
 public static void main(String[] args) {
       

        FrmAdmin frmAdmin = new FrmAdmin();
        frmAdmin.setVisible(true);
 }


}
