# Sistema de Gestión Académica

Aplicación web multiusuario en español para instituciones educativas. Reúne la gestión de estudiantes, profesores, cursos y registros académicos en una base de datos compartida, accesible desde el navegador.

## Objetivo

Facilitar el seguimiento académico desde la matrícula hasta la consulta de asistencias y calificaciones. Cada perfil puede realizar sus tareas y consultar la información que le corresponde.

## Funcionalidades

| Módulo | Qué permite hacer |
| --- | --- |
| Panel de inicio | Consultar el total de cursos, estudiantes y profesores, y el porcentaje de asistencia del día según el perfil. |
| Estudiantes, profesores y cursos | Crear, editar y eliminar registros, asignar docentes y configurar secciones y cupos. |
| Matrículas y horarios | Matricular o retirar estudiantes, controlar cupos y detectar cruces de horario por profesor, aula, sección o curso. |
| Asistencias | Pasar lista por curso y fecha, corregir marcas y consultar el historial con filtros. |
| Evaluaciones y calificaciones | Crear evaluaciones, registrar notas de 0 a 20 y consultar los resultados por curso y fecha. |
| Reportes e importación | Exportar asistencias y calificaciones a CSV e importar archivos de la versión de escritorio. |
| Cuentas y permisos | Administrar usuarios y restringir el acceso según su rol y su relación con los cursos. |

## Cambios recientes

- Si otra sesión cambia una asistencia o calificación, el sistema conserva el cambio guardado y pide volver a cargar la lista. La interfaz descarta las respuestas atrasadas al cambiar de curso o fecha. [Correcciones de concurrencia e historiales](https://github.com/James20i789/SistemaGestionAcademica/commit/7e138e1c130e7047ad3143257b88c3c2fadd53f6)
- Los historiales cargan por páginas y permiten consultar cursos de los que el estudiante se retiró. El CSV incluye todos los resultados de los filtros aplicados.
- La importación prioriza el documento del profesor y rechaza los archivos con codificación inválida. Si la sesión caduca al guardar, la aplicación vuelve al inicio de sesión.
- La aplicación pasó de Java Swing a una versión web con PostgreSQL y despliegue mediante Docker Compose. GitHub Actions ejecuta las pruebas en H2 y PostgreSQL y comprueba el arranque de los servicios. [Migración a la web](https://github.com/James20i789/SistemaGestionAcademica/commit/7ab64dc2c9ce3910a7d586b01d6f21a5628d75f8) · [Actualización de GitHub Actions](https://github.com/James20i789/SistemaGestionAcademica/commit/5c579f584b658c7428ae51a77a340f018f13c07e)

## Tecnologías

| Área | Herramientas |
| --- | --- |
| Interfaz web | HTML, CSS y JavaScript, con plantillas Thymeleaf. |
| Servidor y seguridad | Java 21, Spring Boot, API REST y Spring Security. |
| Datos | PostgreSQL para despliegue, H2 para desarrollo local y Flyway para las migraciones. |
| Compilación y despliegue | Maven, Docker Compose, Caddy y GitHub Actions. |

## Perfiles de acceso

| Perfil | Alcance |
| --- | --- |
| Administrador | Mantiene el padrón, los cursos, los horarios, las matrículas y los usuarios. Puede gestionar la asistencia y las notas de todos los cursos. |
| Profesor | Consulta sus cursos y su matrícula. Registra y corrige asistencias, evaluaciones y notas de los cursos asignados. |
| Estudiante | Consulta sus cursos y sus horarios, y solo puede consultar y exportar sus propias asistencias y notas. |

Crea primero el registro del profesor o estudiante y luego vincúlalo a su cuenta desde **Usuarios**. Cada cuenta se vincula a un solo registro. Las contraseñas se guardan con BCrypt y se pueden cambiar en **Mi cuenta**. Un administrador puede restablecer la contraseña de otra cuenta o desactivarla. Estos cambios invalidan las sesiones anteriores de esa cuenta.

La matrícula se limita al cupo del curso. Retirar una matrícula conserva sus asistencias y calificaciones. El sistema detecta cruces de horario por profesor, aula, sección o curso. Las calificaciones usan escala de 0 a 20 y admiten dos decimales.

Para pasar lista, selecciona el curso y la fecha, revisa los estados y guarda. Volver a guardar la misma fecha edita las marcas existentes. La lista que se registró por primera vez queda asociada a esa fecha. Desde **Historial de asistencia** se puede quitar una marca y volver a asignarla desde **Pasar lista**. Los historiales muestran hasta 100 filas por página y los archivos CSV incluyen todos los registros que coinciden con los filtros. Los estudiantes pueden filtrar el historial por cursos de los que se hayan retirado cuando conservan registros allí.

Si otra sesión cambia la asistencia o las notas mientras estás editando, el servidor conserva lo guardado y pide volver a cargar la lista. Una nota vacía de un estudiante retirado se puede volver a editar en la evaluación sin mostrarla como una calificación publicada.

## Despliegue con Docker Compose

Se necesita Docker con el complemento Compose y acceso a los repositorios de imágenes y dependencias durante la primera compilación.

1. Copia el archivo de configuración.

   ~~~sh
   cp .env.example .env
   ~~~

2. Completa en **.env** los valores de **POSTGRES_PASSWORD**, **APP_ADMIN_EMAIL** y **APP_ADMIN_PASSWORD**. Utiliza contraseñas distintas. La contraseña inicial del administrador debe tener al menos 12 caracteres y un máximo de 72 bytes UTF‑8. Conserva el archivo fuera de Git.
3. Construye e inicia los servicios.

   ~~~sh
   docker compose up --build -d --wait
   ~~~

4. Abre **http://localhost:8080** e inicia sesión con el administrador configurado. Una vez creada la cuenta, puedes retirar **APP_ADMIN_PASSWORD** de **.env**. Esa variable solo inicializa una base sin administradores activos.

El navegador llega a Caddy, que reenvía las solicitudes a Spring Boot. La aplicación y PostgreSQL se comunican por la red de Docker. Los puertos publicados pertenecen a Caddy. El volumen de PostgreSQL conserva los datos al detener y volver a iniciar los servicios.

Si ya compilaste el JAR con Java 21 y Maven, puedes construir solo la imagen de ejecución. Conserva **target/academia.jar** en el directorio del proyecto.

~~~sh
mvn verify
APP_ARTIFACT_SOURCE=prebuilt docker compose up --build -d --wait
~~~

### En una red local

Configura **LISTEN_IP=0.0.0.0**, conserva **SITE_ADDRESS=:80** para HTTP y aplica los cambios con **docker compose up -d --wait**. Desde otro equipo de la red, abre **http://IP_DEL_SERVIDOR:8080**. El firewall del servidor debe permitir ese puerto desde la red autorizada. Para transmitir credenciales y datos reales, usa un dominio con HTTPS o una red privada con un proxy HTTPS de confianza.

### En Internet con HTTPS

Instala Docker en el servidor y apunta el DNS de tu dominio a su dirección. Permite tráfico entrante a los puertos TCP 80 y 443. Configura estos valores en **.env** y vuelve a iniciar la aplicación.

~~~dotenv
LISTEN_IP=0.0.0.0
SITE_ADDRESS=academia.tudominio.com
HTTP_PORT=80
HTTPS_PORT=443
APP_SECURE_COOKIE=true
~~~

Caddy gestiona el certificado para el dominio configurado. Si el servidor está detrás de NAT, necesitará redirección de puertos o un punto de entrada accesible. Para un servicio interno, el nombre y la ruta al servidor pueden limitarse a la VPN o a la red institucional.

### Verificación y respaldo

~~~sh
docker compose ps
docker compose logs app
curl -fsS http://localhost:8080/actuator/health/readiness
docker compose exec -T db pg_dump -U academia -d academia -Fc > academia.dump
~~~

Ajusta la URL del chequeo a tu configuración de red. Guarda el respaldo fuera del host y comprueba su restauración con **pg_restore** en una base independiente. **docker compose down** conserva los volúmenes. El indicador **-v** los elimina junto con sus datos.

## Importar datos del programa de escritorio

Desde la cuenta de administrador, abre **Importación** y elige los archivos de texto UTF‑8 que quieras importar. Guarda antes una copia independiente de los archivos originales.

| Archivo | Tratamiento |
| --- | --- |
| **Estudiantes.txt** y **Profesor.txt** | Se importan por documento. Si el documento ya existe, se conserva el registro de la aplicación web. |
| **Cursos.txt** | Se deduplica por código. Se busca al profesor primero por documento y, si no coincide, por un nombre completo único en el padrón. Los cursos se crean con la sección **Por asignar**. Actualiza la sección y registra su matrícula en la aplicación. |
| **Asistencias.txt** | Queda disponible para el administrador como historial sin curso y como CSV. El archivo anterior solo identificaba al estudiante por nombre, así que estos registros se conservan sin asignarles una matrícula. Las filas idénticas se importan una sola vez. |
| **Usuario.txt** | Crea las cuentas nuevamente desde **Usuarios** con contraseñas nuevas. El formato anterior almacenaba las claves en texto plano. |

La importación acepta hasta 500 KB por archivo desde el navegador. Si alguna fila falla, el lote se revierte y se informa el archivo y la fila cuando se puede identificar. El programa de escritorio original se conserva en el historial de Git.

## Desarrollo y pruebas

Con Java 21 y Maven se puede ejecutar el JAR localmente con una base H2 persistente en **data/**. Define las credenciales iniciales en el entorno y ejecuta:

~~~sh
mvn verify
java -jar target/academia.jar
~~~

Para usar PostgreSQL fuera de Compose configura **DATABASE_URL**, **DATABASE_USER** y **DATABASE_PASSWORD**. El servidor escucha en **127.0.0.1:8080** por defecto. Puedes configurar **APP_BIND**, **PORT** y **APP_TIME_ZONE**. Docker Compose habilita el procesamiento de encabezados reenviados que sanitiza Caddy. Al ejecutar el JAR directamente, habilita **APP_FORWARD_HEADERS_STRATEGY=framework** solo si el proxy de confianza reemplaza esos encabezados y el puerto de la aplicación queda detrás de ese proxy. Flyway inicializa y versiona el esquema. Spring Security gestiona las sesiones y protege las operaciones de escritura con CSRF.

GitHub Actions ejecuta la suite de integración contra H2 y PostgreSQL 17 y arranca los servicios de Docker Compose para verificar el punto de salud a través de Caddy. Las pruebas con PostgreSQL pueden reproducirse en una **base de pruebas dedicada** definiendo **TEST_DATABASE_URL**, **TEST_DATABASE_USER** y **TEST_DATABASE_PASSWORD** antes de ejecutar Maven. La suite vacía las tablas académicas de esa base al iniciar cada caso.

### Edición y paginación en la API

Las lecturas de **/api/attendance/roster** y **/api/assessments/{id}/grades** devuelven el encabezado **ETag**. Incluye su valor en **If-Match** al guardar la lista. Para borrar una marca individual de asistencia, usa en **If-Match** el campo **version** de la fila correspondiente del historial. Una escritura sin versión devuelve **428** y una versión anterior devuelve **412**.

Los listados **/api/attendance**, **/api/grades** y **/api/legacy** aceptan **page** y **size**. Las páginas comienzan en 1 y **size** puede ir de 1 a 200, con un valor predeterminado de 100. **X-Has-More** indica si hay otra página. Las descargas de asistencia por curso y de calificaciones aplican los filtros al conjunto completo. El CSV de asistencias antiguas incluye todo su historial. Las fechas admiten años entre 0001 y 9999.

## Autor

**James Rios**, programador en desarrollo y responsable de este repositorio. [Perfil de GitHub @James20i789](https://github.com/James20i789)
