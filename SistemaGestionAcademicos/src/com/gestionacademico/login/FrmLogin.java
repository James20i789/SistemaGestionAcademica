package com.gestionacademico.login;



import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import com.gestionacademico.controller.ControllerUsuario;
import com.gestionacademico.model.Usuario;
import com.gestionacademico.views.*;

public class FrmLogin {

    private static final String RESOURCE_PATH = "/com/gestionescolar/recursos/";

    private JFrame frame;
    private JTextField textUser;
    private JPasswordField textPassword;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }catch (Exception ignored) {

            }
            FrmLogin window = new FrmLogin();
            window.frame.setVisible(true);
        });
    }

   public FrmLogin() {
	initialize();
}

    private void initialize() {

        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        GradientPanel background = new GradientPanel();
        background.setLayout(null);
        frame.setContentPane(background);

        RoundedPanel card = new RoundedPanel(30);
        card.setLayout(null);
        card.setBounds(240, 42, 420, 500);
        background.add(card);

        JLabel avatar = new JLabel();
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setIcon(scaledIcon("avatar.png", 112, 112));
        avatar.setBounds(154, 22, 112, 112);
        card.add(avatar);

        JLabel title = new JLabel("User Login");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(new Color(70, 70, 70));
        title.setBounds(45, 137, 330, 35);
        card.add(title);

        textUser = createField("Username");
        textUser.setBounds(45, 195, 330, 46);
        card.add(textUser);
        card.add(createFieldIcon("user.png", 57, 208));

        textPassword = createPasswordField("Password");
        textPassword.setBounds(45, 255, 330, 46);
        card.add(textPassword);
        card.add(createFieldIcon("pass.png", 57, 268));

        JCheckBox remember = new JCheckBox("Remember me");
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        remember.setForeground(new Color(120, 120, 120));
        remember.setBounds(42, 315, 145, 28);
        card.add(remember);

        JLabel forgot = new JLabel("Forgot Password?");
        forgot.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        forgot.setForeground(new Color(150, 150, 150));
        forgot.setHorizontalAlignment(SwingConstants.RIGHT);
        forgot.setBounds(215, 315, 160, 28);
        card.add(forgot);

        JButton btnLogin = new JButton("LOGIN");
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setBackground(new Color(36, 112, 219));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setBounds(45, 370, 330, 55);

        btnLogin.addActionListener(new LoginAction());
        card.add(btnLogin);
    }

    private JTextField createField(String hint) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(new Color(80, 80, 80));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(0, 42, 0, 12)));
        field.putClientProperty("JTextField.placeholderText", hint);
        return field;
    }

    private JPasswordField createPasswordField(String hint) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(new Color(80, 80, 80));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(0, 42, 0, 12)));
        field.putClientProperty("JTextField.placeholderText", hint);
        return field;
    }

    private JLabel createFieldIcon(String iconName, int x, int y) {
        JLabel icon = new JLabel(scaledIcon(iconName, 20, 20));
        icon.setBounds(x, y, 20, 20);
        return icon;
    }

    private ImageIcon scaledIcon(String name, int width, int height) {
        java.net.URL resource = FrmLogin.class.getResource(RESOURCE_PATH + name);
        if (resource == null) {
            return null;
        }
        return new ImageIcon(new ImageIcon(resource).getImage().getScaledInstance(width, height,
                Image.SCALE_SMOOTH));
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String email = textUser.getText().trim();
            String pass = new String(textPassword.getPassword());

            if (email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Por favor, complete todos los campos", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ControllerUsuario user = new ControllerUsuario();
            if (user.validarUsuario(email, pass)) {
                JOptionPane.showMessageDialog(frame, "Inicio de sesión exitoso", "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

                Usuario usuarioActual = new Usuario(email, pass, "Email", "Pass");
                MainFrame mainFrame = new MainFrame(usuarioActual);
                mainFrame.setVisible(true);
                frame.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Correo electrónico o contraseña incorrectos", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, getHeight(), new Color(36, 112, 219),
                    getWidth(), 0, new Color(255, 241, 165)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
