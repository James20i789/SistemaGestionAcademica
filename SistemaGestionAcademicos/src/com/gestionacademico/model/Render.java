package com.gestionacademico.model;

public class Render extends javax.swing.table.DefaultTableCellRenderer {
    @Override
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        java.awt.Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (isSelected) {
            component.setBackground(new java.awt.Color(0, 120, 215)); // Color de fondo cuando está seleccionado
            component.setForeground(java.awt.Color.WHITE); // Color de texto cuando está seleccionado
        } else {
            component.setBackground(java.awt.Color.WHITE); // Color de fondo por defecto
            component.setForeground(java.awt.Color.BLACK); // Color de texto por defecto
        }
        return component;
    }
    
}
