package com.gestionacademico.view.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.gestionacademico.controller.ControllerAsistencia;
import com.gestionacademico.controller.ControllerEstudiante;
import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Asistencia;
import com.gestionacademico.model.Estudiantes;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class AsistenciasPanel extends JPanel {
    private  DefaultTableModel model;
    private  JTable tabla;
    private  JTextField fechaField = new JTextField(LocalDate.now().toString(), 10);
    private  ControllerAsistencia asistenciaRepository = new ControllerAsistencia();
    private  ControllerEstudiante estudianteRepository = new ControllerEstudiante();
    private  Consumer<String> onNavigate;
    public AsistenciasPanel() {

        this(destino -> { });
    }

    public AsistenciasPanel(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Gestion Asistencias");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[] { "Estudiante", "Estado" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);

        JComboBox<String> estados = new JComboBox<>(
                new String[] { "Presente", "Ausente", "Tardanza", "Justificación" });
        tabla.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(estados));

        JPanel encabezado = new JPanel(new FlowLayout(FlowLayout.LEFT));
        encabezado.setBackground(Estilos.BG_LIGHT);
        encabezado.add(new JLabel("Fecha (AAAA-MM-DD):"));
        encabezado.add(fechaField);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBackground(Estilos.BG_LIGHT);
        contenido.add(encabezado, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(contenido, BorderLayout.CENTER);




        JButton generarButton = Estilos.crearBotonPrimario("Generar reporte");
        generarButton.addActionListener(e -> generarReporte());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.setBackground(Estilos.BG_LIGHT);
        acciones.add(generarButton);
        add(acciones, BorderLayout.SOUTH);

        cargarEstudiantes();
    }

    public void cargarEstudiantes() {
        model.setRowCount(0);
        Estudiantes[] estudiantes = estudianteRepository.listarDatos();
        for (Estudiantes estudiante : estudiantes) {
            if (estudiante != null) {
                model.addRow(new Object[] {
                        estudiante.getNombre() + " " + estudiante.getApellidos(),
                        "Presente"
                });
            }
        }
    }





    private void generarReporte() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay estudiantes registrados.",
                    "Pasar lista", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "La fecha debe tener el formato AAAA-MM-DD.",
                    "Fecha inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<Asistencia> asistencias = new ArrayList<>();
        for (int fila = 0; fila < model.getRowCount(); fila++) {
            asistencias.add(new Asistencia(
                    (String) model.getValueAt(fila, 0),
                    fecha,
                    (String) model.getValueAt(fila, 1)));
        }

        try {
            asistenciaRepository.registrarAsistencias(asistencias);
            JOptionPane.showMessageDialog(this, "Reporte generado para "
                    + asistencias.size() + " estudiantes.", "Reporte generado",
                    JOptionPane.INFORMATION_MESSAGE);
            onNavigate.accept("Historial");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error al generar reporte", JOptionPane.ERROR_MESSAGE);
        }
    }
}
