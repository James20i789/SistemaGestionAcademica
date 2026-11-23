package com.gestionacademico.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.gestionacademico.model.Usuario;

public class ControllerUsuario {
	FileWriter archivoEscritura;
	BufferedWriter buferEscritura;
	PrintWriter escritor;
	FileReader archivoLectura;
	BufferedReader buferLectura;

	public String registrarUsuario(Usuario usuario) {
		String resultado = "Error de registrar usuario";

		try (PrintWriter escritor = new PrintWriter(new BufferedWriter(new FileWriter("Usuario.txt", true)))) {
			escritor.println(usuario.getNombre() + "," + usuario.getCorreo() + "," + usuario.getClave() + ","
					+ usuario.getRol());
            resultado = "Usuario registrado";
		} catch (IOException e) {
			e.printStackTrace();

		}
		return resultado;
	}

	public boolean validarUsuario(String correo, String clave) {
		boolean usuarioValido = false;
		try {
			archivoLectura = new FileReader("Usuario.txt");
			buferLectura = new BufferedReader(archivoLectura);
			String linea;
			while ((linea = buferLectura.readLine()) != null) {
				String[] datos = linea.split(",");
				if (datos.length == 4) {
					String correoArchivo = datos[1];
					String claveArchivo = datos[2];
					if (correo.equals(correoArchivo) && clave.equals(claveArchivo)) {
						usuarioValido = true;
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (buferLectura != null)
					buferLectura.close();
				if (archivoLectura != null)
					archivoLectura.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return usuarioValido;
	}
}
