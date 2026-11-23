package com.gestionacademico.views;

import com.gestionacademico.controller.ControllerUsuario;
import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarMenu extends JPanel {
    private static long serialVersionUID = 1L;
    private final Map<String, JPanel> items = new LinkedHashMap<>();
    private JPanel activeItem;
    private final Consumer<String> onNavigate;
    private ControllerUsuario usuarioRepository ;

    public SidebarMenu(Usuario usuario, Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.usuarioRepository = new  ControllerUsuario();

        setBackground(Estilos.SIDEBAR_BG);
        setPreferredSize(new Dimension(220, 600));
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setBackground(Estilos.SIDEBAR_BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblSistema = new JLabel("Sistema");
        lblSistema.setFont(Estilos.FONT_TITLE);
        lblSistema.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("DE Gestión Academico");
        lblSub.setFont(Estilos.FONT_SUBTITLE);
        lblSub.setForeground(new Color(180, 180, 190));

        JPanel userCard = new JPanel(new BorderLayout(10, 0));
        userCard.setBackground(new Color(30, 33, 50));
        userCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel avatar = new JLabel(usuario.getNombre().substring(0, 1).toUpperCase(), SwingConstants.CENTER);

        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(true);
        avatar.setBackground(Estilos.PRIMARY);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        avatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                registrarUsuario();
            }
        });

        JPanel userInfo = new JPanel(new GridLayout(2, 1));
        userInfo.setBackground(new Color(30, 33, 50));
        JLabel lblNombre = new JLabel(usuario.getNombre());
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblRol = new JLabel(usuario.getRol());
        lblRol.setForeground(new Color(100, 255, 150));
        lblRol.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        userInfo.add(lblNombre);
        userInfo.add(lblRol);

        userCard.add(avatar, BorderLayout.WEST);
        userCard.add(userInfo, BorderLayout.CENTER);

        top.add(lblSistema);
        top.add(lblSub);
        top.add(Box.createVerticalStrut(15));
        top.add(userCard);
        top.add(Box.createVerticalStrut(20));

        JPanel menuItems = new JPanel();
        menuItems.setBackground(Estilos.SIDEBAR_BG);
        menuItems.setLayout(new BoxLayout(menuItems, BoxLayout.Y_AXIS));

        addMenuItem(menuItems, "Inicio");
        addMenuItem(menuItems, "Cursos");
        addMenuItem(menuItems, "Asistencias");
        addMenuItem(menuItems, "Historial");
        addMenuItem(menuItems, "Profesor");
        addMenuItem(menuItems, "Estudiantes");
        top.add(menuItems);
        add(top, BorderLayout.NORTH);
    }

    private void addMenuItem(JPanel container, String nombre) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Estilos.SIDEBAR_BG);
        item.setMaximumSize(new Dimension(190, 38));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(nombre);
        lbl.setForeground(new Color(200, 200, 210));
        lbl.setFont(Estilos.FONT_MENU);
        item.add(lbl, BorderLayout.WEST);

        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (item != activeItem)
                    item.setBackground(Estilos.SIDEBAR_ITEM_HOVER);
            }

            public void mouseExited(MouseEvent e) {
                if (item != activeItem)
                    item.setBackground(Estilos.SIDEBAR_BG);
            }

            public void mouseClicked(MouseEvent e) {
                setActive(item);
                onNavigate.accept(nombre);
            }
        });

        items.put(nombre, item);
        container.add(item);
        container.add(Box.createVerticalStrut(4));

        if (activeItem == null)
            setActive(item);
    }

    private void setActive(JPanel item) {
        if (activeItem != null)
            activeItem.setBackground(Estilos.SIDEBAR_BG);
        activeItem = item;
        activeItem.setBackground(Estilos.SIDEBAR_ITEM_ACTIVE);
    }

    public void select(String nombre) {
        if (items.containsKey(nombre))
            setActive(items.get(nombre));
    }

    public void registrarUsuario() {
        JTextField nombre = new JTextField();
        JTextField correo = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> rol = new JComboBox<>(new String[]{"Administrador", "Profesor", "Estudiante"});
       
        // JTextField rol = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Nombre:"));
        form.add(nombre);
        form.add(new JLabel("Correo:"));
        form.add(correo);
        form.add(new JLabel("Password:"));
        form.add(password);
        form.add(new JLabel("Rol:"));
        form.add(rol);
        int confirmar = JOptionPane.showConfirmDialog(this, form, "Crear usuario", JOptionPane.OK_CANCEL_OPTION);
        if (confirmar == JOptionPane.OK_OPTION) {
            try {
                String nombreUsuario = nombre.getText();
                String correoUsuario = password.getText();
                String claveUsuario = password.getText();
                String rolUsuario = (String) rol.getSelectedItem();
                if (nombreUsuario.isEmpty() || correoUsuario.isEmpty() || claveUsuario.isEmpty() || rolUsuario.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Por favor ingrese un datos de los usuarios");
                    return;
                }
                Usuario usuario = new Usuario(nombreUsuario, correoUsuario, claveUsuario, rolUsuario);
                usuarioRepository.registrarUsuario(usuario);


            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }


        }
    }
}


