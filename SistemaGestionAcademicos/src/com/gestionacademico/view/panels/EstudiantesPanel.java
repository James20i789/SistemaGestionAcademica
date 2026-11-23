package com.gestionacademico.view.panels;


import com.gestionacademico.controller.ControllerEstudiante;
import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Estudiantes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;


public class EstudiantesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private DefaultTableModel model;
    private JLabel lbTitulo;
    private JTable TablaEstudiantes;
    private Object[] fila;
    private ControllerEstudiante repor = new ControllerEstudiante();

    public EstudiantesPanel() {
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        model = new DefaultTableModel();
        lbTitulo = new JLabel("Gestion de Estudiantes");
        lbTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(lbTitulo, BorderLayout.NORTH);
        String[] columna = {"Documentos", "Nombre", "Apellidos", "Generos", "Seccion"};
        model = new DefaultTableModel(columna, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        TablaEstudiantes = new JTable(model);
        TablaEstudiantes.setRowHeight(28);
        cargarDatos();
        JScrollPane scroll = new JScrollPane(TablaEstudiantes);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 232, 240)));
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Estilos.BG_LIGHT);
        JButton btnAdd = Estilos.crearBotonPrimario(" Nuevo ");
        JButton btnDel = Estilos.crearBotonPrimario(" Eliminar");

        actions.add(btnAdd);
        actions.add(btnDel);
        add(actions, BorderLayout.SOUTH);
        btnAdd.addActionListener(e -> mostrarDialogoNuevoEstudiantes());
        btnDel.addActionListener(e -> eliminarSeleccionado());

    }


    private void eliminarSeleccionado() {
        int row = TablaEstudiantes.getSelectedRow();

        if (row != -1) {
            int id = (int) model.getValueAt(row, 0);
            repor.eliminar(id);
            cargarDatos();
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrarDialogoNuevoEstudiantes() {
        JTextField documentos = new JTextField();
        JTextField nombre = new JTextField();
        JTextField apellidos = new JTextField();
        JComboBox<String> generos = new JComboBox<>(new String[]{"Masculino", "Femenino", "Otro"});
        JComboBox<String> seccion = new JComboBox<>(new String[]{"7-1", "7-2", "7-3", "7-4", "7-5", "8-1", "8-2", "8-3", "9-1", "9-2", "9-3", "10-1", "10-2", "10-3", "11-1", "11-2", "11-3"});

        JPanel form = new JPanel(new GridLayout(0, 2, 11, 11));
        form.add(new JLabel("Documentos:"));
        form.add(documentos);
        form.add(new JLabel("Nombre:"));
        form.add(nombre);
        form.add(new JLabel("Apellidos:"));
        form.add(apellidos);
        form.add(new JLabel("Generos:"));
        form.add(generos);
        form.add(new JLabel("Seccion:"));
        form.add(seccion);

        int r = JOptionPane.showConfirmDialog(this, form, "Nuevo Estudiante", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            try {
                String documentoTxt = documentos.getText().trim();
                String nombreTxt = nombre.getText().trim();
                String apellidosTxt = apellidos.getText().trim();
                String generoTxt = (String) generos.getSelectedItem();
                String seccionTxt = (String) seccion.getSelectedItem();

                if (documentoTxt.isEmpty() || nombreTxt.isEmpty() || apellidosTxt.isEmpty() || generoTxt.isEmpty() || seccionTxt.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int documento = Integer.parseInt(documentoTxt);
                Estudiantes es = new Estudiantes(documento, nombreTxt, apellidosTxt, generoTxt, seccionTxt);
                repor.registrarEstudiantes(es);
                cargarDatos();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El campo Documentos debe ser un número entero.", "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void cargarDatos() {
        model.setRowCount(0);
        Estudiantes[] estudiantes = repor.listarDatos();
        for (Estudiantes estudiante : estudiantes) {
            if (estudiante != null) {
                fila = new Object[5];
                fila[0] = estudiante.getDocumento();
                fila[1] = estudiante.getNombre();
                fila[2] = estudiante.getApellidos();
                fila[3] = estudiante.getGeneros();
                fila[4] = estudiante.getSeccion();
                model.addRow(fila);
            }
        }

    }

}
  
    

