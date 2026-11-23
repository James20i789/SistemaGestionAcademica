package com.gestionacademico.controller;

import com.gestionacademico.model.Curso;
import java.io.*;
import javax.swing.JOptionPane;

public class ControllerCurso {
	private  String ARCHIVO = "Cursos.txt";

	public String registrar(Curso nuevo) {
		if (nuevo == null) {
			return null;
		}
		if (existeCodigo(nuevo.getCodigo())) {
			return null;
		}

		try (FileWriter archivoEscritura = new FileWriter(ARCHIVO, true);
			 BufferedWriter buferEscritura = new BufferedWriter(archivoEscritura);
			 PrintWriter escritor = new PrintWriter(buferEscritura)) {

			escritor.println(nuevo.getCodigo() + "," + nuevo.getNombre() + "," + nuevo.getProfesor() + ","
					+ nuevo.getCapacidad());
			escritor.flush();
			JOptionPane.showMessageDialog(null, "Curso registrado correctamente");
			return "Curso registrado correctamente";
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error de registrar cursos: " + e.getMessage());
			return null;
		}
	}

	private boolean existeCodigo(String codigo) {
		File archivo = new File(ARCHIVO);
		if (!archivo.exists()) {
			return false;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				if (linea.trim().isEmpty()) {
					continue;
				}
				String[] datos = linea.split(",");
				if (datos.length > 0 && datos[0].equalsIgnoreCase(codigo)) {
					return true;
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error al validar código: " + e.getMessage());
		}
		return false;
	}

	public Curso[] listarCursos() {
		Curso[] cursos = new Curso[100];
		int conteo = 0;
		String temporal;
		try (BufferedReader buferLectura = new BufferedReader(new FileReader(ARCHIVO))) {
			while ((temporal = buferLectura.readLine()) != null) {
				if (temporal.trim().isEmpty()) {
					continue;
				}
				String[] datos = temporal.split(",");
				if (datos.length >= 4) {
					cursos[conteo] = new Curso(datos[0], datos[1], datos[2], Integer.parseInt(datos[3]));
					conteo++;
				}
			}
		} catch (FileNotFoundException e) {
			return cursos;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error de cargar datos: " + e.getMessage());
		}

		return cursos;
	}

	public void eliminar(String codigo) {
		File archivoOriginal = new File(ARCHIVO);
		File archivoTemporal = new File("Cursos_temp.txt");

		try (BufferedReader br = new BufferedReader(new FileReader(archivoOriginal));
			 PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(archivoTemporal)))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				String[] datos = linea.split(",");
				if (datos.length > 0 && !datos[0].equalsIgnoreCase(codigo)) {
					pw.println(linea);
				}
			}
			pw.flush();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error al eliminar curso: " + e.getMessage());
			return;
		}

		if (archivoOriginal.delete()) {
			if (!archivoTemporal.renameTo(archivoOriginal)) {
				JOptionPane.showMessageDialog(null, "No se pudo reemplazar el archivo de cursos.");
			}
		}
	}
}

