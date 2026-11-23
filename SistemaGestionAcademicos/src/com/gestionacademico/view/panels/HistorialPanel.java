package com.gestionacademico.view.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.gestionacademico.controller.ControllerAsistencia;
import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Asistencia;

import java.awt.*;
import java.io.IOException;

public class HistorialPanel extends JPanel {
    private final DefaultTableModel model;
    private  ControllerAsistencia asistenciaRepository = new ControllerAsistencia();
    private JButton btnEliminar;
    private JTable tablaHistorial;
    public HistorialPanel() {
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
       
        JLabel title = new JLabel("Historial de asistencias");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[] { "Estudiante", "Fecha", "Estado" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {

                return false;
            }
        };
         tablaHistorial = new JTable(model);
        tablaHistorial.setRowHeight(28);
        tablaHistorial.setFillsViewportHeight(true);
        add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        cargarDatos();
       JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
       actions.setBackground(Estilos.BG_LIGHT);
       btnEliminar = Estilos.crearBotonPrimario("Eliminar ");
       actions.add(btnEliminar);
       add(actions, BorderLayout.SOUTH);
       btnEliminar.addActionListener(e ->eliminarSeleccion());

    }

    public void eliminarSeleccion() {
        int filaSeleccionada = tablaHistorial.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una asistencia para eliminar.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo = tablaHistorial.convertRowIndexToModel(filaSeleccionada);
        Asistencia asistencia = new Asistencia(
                (String) model.getValueAt(filaModelo, 0),
                (java.time.LocalDate) model.getValueAt(filaModelo, 1),
                (String) model.getValueAt(filaModelo, 2));

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Deseas eliminar la asistencia seleccionada?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (asistenciaRepository.eliminarAsistencia(asistencia)) {
                cargarDatos();
            } else {
                JOptionPane.showMessageDialog(this, "La asistencia ya no existe en el archivo.",
                        "No se pudo eliminar", JOptionPane.WARNING_MESSAGE);
                cargarDatos();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error al eliminar asistencia", JOptionPane.ERROR_MESSAGE);
        }
    }
   
   
   
   
   
    public void cargarDatos() {
        model.setRowCount(0);
        try {
            for (Asistencia asistencia : asistenciaRepository.listarAsistencias()) {
                model.addRow(new Object[] {
                        asistencia.getEstudiante(),
                        asistencia.getFecha(),
                        asistencia.getEstado()
                });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error al cargar historial", JOptionPane.ERROR_MESSAGE);
        }
    }
}
