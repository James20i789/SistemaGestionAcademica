package com.gestionacademico.controller;

import java.io.*;
import javax.swing.JOptionPane;
import com.gestionacademico.model.Estudiantes;

public class ControllerEstudiante {
	FileWriter archivoEscritura;
	BufferedWriter buferEscritura;
	PrintWriter escritor;
	FileReader archivoLectura;
	BufferedReader buferLectura;

	public String registrarEstudiantes(Estudiantes es) {
		String mensaje = "";

		try {
			archivoEscritura = new FileWriter("Estudiantes.txt", true);
			buferEscritura = new BufferedWriter(archivoEscritura);
			escritor = new PrintWriter(buferEscritura);
			escritor.println(es.getDocumento() + "," + es.getNombre() + "," + es.getApellidos() + "," + es.getGeneros()
					+ "," + es.getSeccion());
			escritor.flush();
			escritor.close();
			mensaje = "Estudiante registrado correctamente";
		} catch (Exception e) {
			mensaje = "Error de registrar estudiantes";
		}
	 return mensaje;
	}

	public Estudiantes[] listarDatos() {
		Estudiantes[] es = new Estudiantes[100];
		int conteo = 0;
		try {
			archivoLectura = new FileReader("Estudiantes.txt");
			buferLectura = new BufferedReader(archivoLectura);
			String lineaTemporal;
			while ((lineaTemporal = buferLectura.readLine()) != null) {
				String[] datos = lineaTemporal.split(",");
				es[conteo] = new Estudiantes(Integer.parseInt(datos[0]), datos[1], datos[2], datos[3], datos[4]);
				conteo++;
			}

			archivoLectura.close();
			buferLectura.close();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error de cargar datos");

		}
		return es;

	}
   public void eliminar(int idocumento){ 
     try {
		 Estudiantes[] estudiantes = listarDatos();
		 FileWriter archivoEscritura = new FileWriter("Estudiantes.txt");
		 BufferedWriter buferEscritura = new BufferedWriter(archivoEscritura);
		 PrintWriter escritor = new PrintWriter(buferEscritura);

		 for (Estudiantes estudiante : estudiantes) {
			 if (estudiante != null && estudiante.getDocumento() != idocumento) {
				 escritor.println(estudiante.getDocumento() + "," + estudiante.getNombre() + "," + estudiante.getApellidos() + "," + estudiante.getGeneros() + "," + estudiante.getSeccion());
			 }
		 }

		 escritor.close();
		 buferEscritura.close();
		 archivoEscritura.close();

	 } catch (Exception e) {
		 JOptionPane.showMessageDialog(null, "Error al eliminar el estudiante");
	 }

   }

}
