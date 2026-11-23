package com.gestionacademico.form;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Estilos {
    public static final Color SIDEBAR_BG = new Color(16, 18, 27);
    public static final Color SIDEBAR_ITEM_HOVER = new Color(34, 38, 56);
    public static final Color SIDEBAR_ITEM_ACTIVE = new Color(42, 48, 75);
    public static final Color PRIMARY = new Color(67, 97, 238);
    public static final Color BG_LIGHT = new Color(245, 247, 251);
    public static final Color CARD_WHITE = Color.WHITE;

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MENU = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    public static JButton crearBotonPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.black);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JPanel crearCard() {
        JPanel p = new JPanel();
        p.setBackground(CARD_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,232,240)),
            new EmptyBorder(15,15,15,15)
        ));
        return p;
    }
}