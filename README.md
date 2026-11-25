# Sistema de Gestión Académica

Aplicación web en español para administrar estudiantes, profesores, cursos, matrículas, horarios, asistencias y calificaciones. Los usuarios acceden desde el navegador y trabajan sobre la misma base de datos.

## Perfiles de acceso

| Perfil | Alcance |
| --- | --- |
| Administrador | Mantiene el padrón, los cursos, los horarios, las matrículas y los usuarios. Puede gestionar la asistencia y las notas de todos los cursos. |
| Profesor | Consulta sus cursos y su matrícula. Registra y corrige asistencias, evaluaciones y notas de los cursos asignados. |
| Estudiante | Consulta sus cursos y sus horarios, y solo puede consultar y exportar sus propias asistencias y notas. |

Crea primero el registro del profesor o estudiante y luego vincúlalo a su cuenta desde **Usuarios**. Cada cuenta se vincula a un solo registro. Las contraseñas se guardan con BCrypt y se pueden cambiar en **Mi cuenta**. Un administrador puede restablecer la contraseña de otra cuenta o desactivarla. Estos cambios invalidan las sesiones anteriores de esa cuenta.

La matrícula se limita al cupo del curso. Retirar una matrícula conserva sus asistencias y calificaciones. El sistema detecta cruces de horario por profesor, aula, sección o curso. Las calificaciones usan escala de 0 a 20 y admiten dos decimales.

Para pasar lista, selecciona el curso y la fecha, revisa los estados y guarda. Volver a guardar la misma fecha edita las marcas existentes. La lista que se registró por primera vez queda asociada a esa fecha. Desde **Historial de asistencia** se puede quitar una marca y volver a asignarla desde **Pasar lista**. Los reportes de asistencia y notas se descargan como CSV con los filtros aplicados.

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
| **Cursos.txt** | Se deduplica por código. El campo de profesor debe coincidir con el documento o con un nombre completo único en el padrón. Los cursos se crean con la sección **Por asignar**. Actualiza la sección y registra su matrícula en la aplicación. |
| **Asistencias.txt** | Queda disponible para el administrador como historial sin curso y como CSV. El archivo anterior solo identificaba al estudiante por nombre, así que estos registros se conservan sin asignarles una matrícula. Las filas idénticas se importan una sola vez. |
| **Usuario.txt** | Crea las cuentas nuevamente desde **Usuarios** con contraseñas nuevas. El formato anterior almacenaba las claves en texto plano. |

La importación acepta hasta 500 KB por archivo desde el navegador. Si alguna fila falla, el lote se revierte y se informa el archivo y la fila cuando se puede identificar. El programa de escritorio original se conserva en el historial de Git.

## Desarrollo y pruebas

Con Java 21 y Maven se puede ejecutar el JAR localmente con una base H2 persistente en **data/**. Define las credenciales iniciales en el entorno y ejecuta:

~~~sh
mvn verify
java -jar target/academia.jar
~~~

Para usar PostgreSQL fuera de Compose configura **DATABASE_URL**, **DATABASE_USER** y **DATABASE_PASSWORD**. El servidor escucha en **127.0.0.1:8080** por defecto. Puedes configurar **APP_BIND**, **PORT** y **APP_TIME_ZONE**. Flyway inicializa y versiona el esquema. Spring Security gestiona las sesiones y protege las operaciones de escritura con CSRF.

GitHub Actions ejecuta la suite de integración contra H2 y PostgreSQL 17 y arranca los servicios de Docker Compose para verificar el punto de salud a través de Caddy. Las pruebas con PostgreSQL pueden reproducirse en una **base de pruebas dedicada** definiendo **TEST_DATABASE_URL**, **TEST_DATABASE_USER** y **TEST_DATABASE_PASSWORD** antes de ejecutar Maven. La suite vacía las tablas académicas de esa base al iniciar cada caso.
