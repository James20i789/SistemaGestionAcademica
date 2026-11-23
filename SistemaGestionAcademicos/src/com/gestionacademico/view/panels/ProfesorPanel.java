package com.gestionacademico.view.panels;


import com.gestionacademico.controller.ControllerProfesor;
import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Profesor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ProfesorPanel extends JPanel {
    private ControllerProfesor repor = new ControllerProfesor();
    private DefaultTableModel model;
    private JTable TablaProfesor;
    private ControllerProfesor con;
    private Object[] fila;

    public ProfesorPanel() {
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Gestión de Profesores");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Documento", "Nombre", "Apellidos", "Especialidad", "Email"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        TablaProfesor = new JTable(model);
        TablaProfesor.setBackground(Estilos.BG_LIGHT);
        TablaProfesor.setRowHeight(28);

        cargaDatos();
        add(new JScrollPane(TablaProfesor), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Estilos.BG_LIGHT);
        JButton btnAdd = Estilos.crearBotonPrimario(" Nuevo");
        JButton btnDelete = Estilos.crearBotonPrimario(" Eliminar");
        actions.add(btnDelete);
        actions.add(btnAdd);
        add(actions, BorderLayout.SOUTH);
        btnAdd.addActionListener(e -> agregar());
        btnDelete.addActionListener(e -> eliminar());
    }


    private void agregar() {
        JTextField id = new JTextField();
        JTextField nombre = new JTextField();
        JTextField apellido = new JTextField();
       
        JComboBox<String> esp = new JComboBox<>(new String[]{"Matemáticas", "Lengua y Literatura", "Ciencias Naturales", "Historia", "Educación Física", "Arte", "Música", "Tecnología", "Idiomas"});
        // JTextField esp = new JTextField();
        JTextField email = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 11, 11));
        form.add(new JLabel("Documento:"));
        form.add(id);
        form.add(new JLabel("Nombre:"));
        form.add(nombre);
        form.add(new JLabel("Apellidos:"));
        form.add(apellido);
        form.add(new JLabel("Especialidad:"));
        form.add(esp);
        form.add(new JLabel("Email:"));
        form.add(email);
        int r = JOptionPane.showConfirmDialog(this, form, "Nuevo Profesor", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            try {
                int documentotxt = Integer.parseInt(id.getText());
                String nombrext = nombre.getText();
                String apellidoxt = apellido.getText();
                String especialidadxt = (String) esp.getSelectedItem();
                String emailxt = email.getText();
                if (documentotxt < 0 || nombrext.isEmpty() || apellidoxt.isEmpty() || especialidadxt.isEmpty() || emailxt.isEmpty()) {
                    return;
                }

                Profesor profesor = new Profesor(documentotxt, nombrext, apellidoxt, especialidadxt, emailxt);
                repor.registrarProfesor(profesor);
                cargaDatos();

            } catch (Exception err) {
                JOptionPane.showMessageDialog(null, err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }


        }

    }

    private  void eliminar() {
        int row = TablaProfesor.getSelectedRow();
        if (row != -1) {
            int id = (int) model.getValueAt(row, 0);
            repor.eliminarProfesor(id);
            cargaDatos();
        } else {
     JOptionPane.showMessageDialog(this, "Seleccione un estudiante para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);

        }

    }


    public void cargaDatos() {
        model.setRowCount(0);

        Profesor[] profesores = repor.listarProfesor();

        if (profesores != null) {
            for (Profesor prof : profesores) {
                if (prof != null) {
                    Object[] fila = new Object[5];

                    fila[0] = prof.getId();
                    fila[1] = prof.getNombre();
                    fila[2] = prof.getApellidos();
                    fila[3] = prof.getEspecialidad();
                    fila[4] = prof.getEmail();

                    model.addRow(fila);
                }
            }
        }
    }

}