
package com.gestionacademico.view.panels;

import javax.swing.*;

import com.gestionacademico.controller.GestionEstadistica;
import com.gestionacademico.form.Estilos;

import java.awt.*;
import java.io.IOException;

public class InicioPanel extends JPanel {
    private static final long serialVersionUID = -1L;

	public InicioPanel() {
        setBackground(Estilos.BG_LIGHT);
        setLayout(new BorderLayout(20,20));
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel title = new JLabel("Dashboard General");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        GestionEstadistica estadisticas = new GestionEstadistica();
        JPanel grid = new JPanel(new GridLayout(2,2,20,20));
        grid.setBackground(Estilos.BG_LIGHT);
        grid.add(crearStatCard("Total Cursos", String.valueOf(estadisticas.contarCursos()), new Color(51, 60, 106)));
        grid.add(crearStatCard("Estudiantes", String.valueOf(estadisticas.contarEstudiantes()), new Color(0, 180, 216)));

        String asistenciaHoy;
        try {
            asistenciaHoy = estadisticas.calcularAsistenciaHoy() + "%";
        } catch (IOException e) {
            asistenciaHoy = "N/D";
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error al cargar asistencia", JOptionPane.ERROR_MESSAGE);
        }
        grid.add(crearStatCard("Asistencia Hoy", asistenciaHoy, new Color(56, 176, 0)));
        grid.add(crearStatCard("Profesores", String.valueOf(estadisticas.contarProfesores()), new Color(255, 183, 3)));

        add(grid, BorderLayout.CENTER);
    }

    private JPanel crearStatCard(String titulo, String valor, Color accent) {
        JPanel card = Estilos.crearCard();
        card.setLayout(new BorderLayout());
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Estilos.FONT_SMALL);
        lblTitulo.setForeground(Color.GRAY);
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblValor.setForeground(accent);
        JPanel topColor = new JPanel();
        topColor.setBackground(accent);
        topColor.setPreferredSize(new Dimension(100,4));
        card.add(topColor, BorderLayout.NORTH);
        card.add(lblTitulo, BorderLayout.CENTER);
        card.add(lblValor, BorderLayout.SOUTH);
        return card;
    }




}