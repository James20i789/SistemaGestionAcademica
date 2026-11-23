package com.gestionacademico.view.panels;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Curso;
import com.gestionacademico.controller.ControllerCurso;

import java.awt.*;

public class CursosPanel extends JPanel {
    private static final long serialVersionUID = 1L;
	private final ControllerCurso repo = new ControllerCurso();
    private final DefaultTableModel model;
    private final JTable tabla;

    public CursosPanel() {
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(15,15));
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel title = new JLabel("Gestión de Cursos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Código", "Nombre", "Profesor", "Capacidad"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(28);
        cargarDatos();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230,232,240)));
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Estilos.BG_LIGHT);
        JButton btnAdd = Estilos.crearBotonPrimario(" Nuevo");
        JButton btnDel = Estilos.crearBotonPrimario("Eliminar");
        actions.add(btnAdd);
        actions.add(btnDel);
        add(actions, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> mostrarDialogoNuevoCurso());
        btnDel.addActionListener(e -> eliminarSeleccionado());
    }
    private void cargarDatos() {
        model.setRowCount(0);

        Curso[] cursos = repo.listarCursos();

        for (Curso c : cursos) {
            if (c != null) { 
                model.addRow(new Object[]{
                    c.getCodigo(),
                    c.getNombre(),
                    c.getProfesor(),
                    c.getCapacidad()
                });
            }
        }
    }

    
    

    private void mostrarDialogoNuevoCurso() {
        JTextField codigo = new JTextField();
        JTextField nombre = new JTextField();
        JTextField profe = new JTextField();
        JTextField cap = new JTextField();
        JPanel form = new JPanel(new GridLayout(4,2,5,5));
        form.add(new JLabel("Código:")); form.add(codigo);
        form.add(new JLabel("Nombre:")); form.add(nombre);
        form.add(new JLabel("Profesor:")); form.add(profe);
        form.add(new JLabel("Capacidad:")); form.add(cap);
        int r = JOptionPane.showConfirmDialog(this, form, "Nuevo Curso", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            String codigoTexto = codigo.getText().trim();
            String nombreTexto = nombre.getText().trim();
            String profeTexto = profe.getText().trim();
            String capTexto = cap.getText().trim();

            if (codigoTexto.isEmpty() || nombreTexto.isEmpty() || profeTexto.isEmpty() || capTexto.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int capacidad = Integer.parseInt(capTexto);
                Curso nuevo = new Curso(codigoTexto, nombreTexto, profeTexto, capacidad);
                String resultado = repo.registrar(nuevo);

                if (resultado == null) {
                    JOptionPane.showMessageDialog(this, "No se pudo registrar el curso. Verifica que el código no exista o intenta de nuevo.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                cargarDatos();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "La capacidad debe ser un número entero.", "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row >= 0) {
            String codigo = (String) model.getValueAt(row, 0);
            repo.eliminar(codigo);
            cargarDatos();
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un curso para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
}