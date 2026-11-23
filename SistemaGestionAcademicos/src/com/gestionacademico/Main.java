package com.gestionacademico;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.gestionacademico.model.Usuario;
import com.gestionacademico.views.MainFrame;

public class Main{ 
	
	public static void main(String[] agrs) {
		SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { 
                
            }

            Usuario usuarioActual = new Usuario("Admin", "admin", "Email","admin");
            MainFrame frame = new MainFrame(usuarioActual);
            frame.setVisible(true);
        });
	
	
	}
}
