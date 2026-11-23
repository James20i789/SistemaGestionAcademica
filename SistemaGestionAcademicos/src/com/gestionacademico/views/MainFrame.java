package com.gestionacademico.views;

import javax.swing.*;

import com.gestionacademico.form.Estilos;
import com.gestionacademico.model.Usuario;
import com.gestionacademico.view.panels.*;
import java.awt.*;

public class MainFrame extends JFrame {
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel contentPanel = new JPanel(cardLayout);
	private final SidebarMenu sidebar;
	private final AsistenciasPanel asistenciasPanel;
	private final HistorialPanel historialPanel;
//	private JButton btnRegistrar;
	private boolean sidebarVisible = true;

	public MainFrame(Usuario usuario) {
		setTitle("Sistema de Gestion Academico");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1100, 700);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topBar.setBackground(Color.WHITE);
		topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 232, 240)));
		JButton btnToggle = new JButton("☰");
		btnToggle.setFocusPainted(false);
		btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
		topBar.add(btnToggle);

		sidebar = new SidebarMenu(usuario, this::navegar);
		contentPanel.add(new InicioPanel(), "Inicio");
		contentPanel.add(new CursosPanel(), "Cursos");
		asistenciasPanel = new AsistenciasPanel(this::navegar);
		historialPanel = new HistorialPanel();
		contentPanel.add(asistenciasPanel, "Asistencias");
		contentPanel.add(historialPanel, "Historial");
		contentPanel.add(new ProfesorPanel(), "Profesor");
		contentPanel.setBackground(Estilos.BG_LIGHT);
		contentPanel.add(new EstudiantesPanel(), "Estudiantes");
		add(topBar, BorderLayout.BEFORE_FIRST_LINE);
		add(sidebar, BorderLayout.WEST);
		add(contentPanel, BorderLayout.CENTER);

		btnToggle.addActionListener(e -> toggleSidebar());

		navegar("Inicio");
	}

	private void navegar(String destino) {
		if ("Asistencias".equals(destino)) {
			asistenciasPanel.cargarEstudiantes();
		} else if ("Historial".equals(destino)) {
			historialPanel.cargarDatos();
		}
		cardLayout.show(contentPanel, destino);
	}

	private void toggleSidebar() {
		sidebarVisible = !sidebarVisible;
		sidebar.setVisible(sidebarVisible);
		revalidate();
		repaint();
	}
}