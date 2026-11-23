package com.gestionacademico.controller;

import com.gestionacademico.model.Profesor;


import javax.swing.*;
import java.io.*;

public class ControllerProfesor {
    public String registrarProfesor(Profesor pro) {
        String salida = "Profesor registrado correctamente";
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter("Profesor.txt", true))) {
            escritor.write(pro.getId() + "," + pro.getNombre() + "," + pro.getApellidos() + "," + pro.getEspecialidad() + "," + pro.getEmail());
            escritor.newLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al registrar el profesor: " + e.getMessage());
        }
        return salida;
    }

    public Profesor[] listarProfesor() {
        Profesor[] profesores = new Profesor[100];
        int conteo = 0;
        try (BufferedReader buferLectura = new BufferedReader(new FileReader("Profesor.txt"))) {
            String lineaTemporal;
            while ((lineaTemporal = buferLectura.readLine()) != null) {
                String[] datos = lineaTemporal.split(",");
                profesores[conteo] = new Profesor(Integer.parseInt(datos[0]), datos[1], datos[2], datos[3], datos[4]);
                conteo++;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al leer el archivo de la lectura:" + e.getMessage());
        }

        return profesores;
    }


   public void eliminarProfesor(int id) {
    try {
        Profesor[] profesores = listarProfesor();

        if (profesores == null) {
            return;
        }

        boolean eliminado = false;

        PrintWriter escritor = new PrintWriter(
                new BufferedWriter(
                        new FileWriter("Profesor.txt")));

        for (Profesor profesor : profesores) {
            if (profesor != null) {
                if (profesor.getId() != id) {
                    escritor.println(
                            profesor.getId() + "," +
                            profesor.getNombre() + "," +
                            profesor.getApellidos() + "," +
                            profesor.getEspecialidad() + "," +
                            profesor.getEmail()
                    );
                } else {
                    eliminado = true;
                }
            }
        }

        escritor.close();

        if (eliminado) {
            JOptionPane.showMessageDialog(null,
                    "Profesor eliminado correctamente.");
        } else {
            JOptionPane.showMessageDialog(null,
                    "No se encontró el profesor.");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null,
                "Error al eliminar el profesor: " + e.getMessage());
    }
}

}
