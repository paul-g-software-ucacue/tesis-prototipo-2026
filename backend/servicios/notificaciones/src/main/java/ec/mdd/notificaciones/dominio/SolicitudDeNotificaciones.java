package ec.mdd.notificaciones.dominio;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * El contenido de un evento {@code <servicio>.NotificacionesSolicitadas}, validado (lenguaje
 * publicado {@code SolicitudDeNotificaciones} de {@code contratos/notificaciones}, F16-H01).
 *
 * <p>Las reglas, además de las del esquema:
 *
 * <ul>
 *   <li>solo admite las claves del esquema, a cualquier profundidad: así no entra ningún secreto ni
 *       dato que nadie pidió (FR-014);
 *   <li>el periodo, el autor, el momento y de 1 a {@value #MENSAJES_MAXIMOS} mensajes;
 *   <li>cada mensaje con un destino: un rol de D-63 con una carrera con su forma, o una persona,
 *       por su usuario (de 1 a 64 caracteres) o por su identificación (D-110); el título de 1 a 200
 *       caracteres, el detalle hasta 1000 y un enlace relativo de la SPA;
 *   <li>el contenido no pasa de 64 KB, contado en UTF-8.
 * </ul>
 *
 * @param periodo el periodo del hecho
 * @param autor quién hizo la transición, o {@code sistema}
 * @param momento cuándo ocurrió
 * @param mensajes los mensajes, cada uno con su destino
 */
public record SolicitudDeNotificaciones(
        String periodo, String autor, Instant momento, List<MensajeSolicitado> mensajes) {

    /** Cuántos mensajes admite una solicitud. */
    public static final int MENSAJES_MAXIMOS = 50;

    /** 64 KB: una solicitud describe un hecho, no un archivo. */
    public static final int TAMANO_MAXIMO = 64 * 1024;

    /** Los diez roles fijos de D-63. */
    public static final Set<String> ROLES =
            Set.of(
                    "ADMINISTRADOR",
                    "COORDINADOR_CARRERA",
                    "DOCENTE",
                    "GESTOR_UNIDAD",
                    "MIEMBRO_OCS",
                    "OPERADOR_DATOS",
                    "RECTOR",
                    "RESPONSABLE_CONSOLIDACION",
                    "SECRETARIA_GENERAL",
                    "VICERRECTOR");

    private static final Pattern PERIODO = Pattern.compile("^[A-Za-z0-9._-]{1,20}$");
    private static final Pattern CARRERA = Pattern.compile("^[0-9A-Z_-]{1,20}$");
    private static final Pattern ENLACE = Pattern.compile("^/([a-z0-9-]+(/[a-z0-9-]+)*)?$");
    private static final int AUTOR_MAXIMO = 64;
    private static final int TITULO_MAXIMO = 200;
    private static final int DETALLE_MAXIMO = 1000;
    private static final Set<String> CLAVES = Set.of("periodo", "autor", "momento", "mensajes");
    private static final Set<String> CLAVES_DEL_MENSAJE =
            Set.of("destino", "titulo", "detalle", "enlace");
    private static final Set<String> CLAVES_DEL_DESTINO =
            Set.of("rol", "carrera", "usuario", "identificacion");
    private static final Set<String> CLAVES_DE_QUIEN = Set.of("rol", "usuario", "identificacion");
    // El formato de Docentes y de Usuarios V7: cédula o pasaporte.
    private static final Pattern IDENTIFICACION = Pattern.compile("^[0-9A-Z]{1,13}$");
    // El largo de nombre_usuario en Usuarios; Usuarios no fija un formato.
    private static final int USUARIO_MAXIMO = 64;

    /** Exige los datos y copia la lista. */
    public SolicitudDeNotificaciones {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(momento, "momento");
        mensajes = List.copyOf(mensajes);
    }

    /**
     * Lee y valida el contenido de un evento.
     *
     * @param contenido el contenido tal como llegó
     * @return la solicitud
     * @throws NotificacionInvalida si no cumple el lenguaje publicado
     */
    public static SolicitudDeNotificaciones de(Map<String, Object> contenido) {
        if (contenido == null) {
            throw new NotificacionInvalida("Falta el contenido.");
        }
        soloClaves(contenido, CLAVES, "la solicitud");
        String periodo = texto(contenido, "periodo");
        if (!PERIODO.matcher(periodo).matches()) {
            throw new NotificacionInvalida("El campo periodo no tiene la forma " + PERIODO);
        }
        String autor = texto(contenido, "autor");
        if (autor.isEmpty() || autor.length() > AUTOR_MAXIMO) {
            throw new NotificacionInvalida("El campo autor debe tener de 1 a 64 caracteres.");
        }
        Instant momento = momento(texto(contenido, "momento"));
        if (!(contenido.get("mensajes") instanceof Collection<?> lista)) {
            throw new NotificacionInvalida("Falta el campo mensajes, o no es una lista.");
        }
        if (lista.isEmpty() || lista.size() > MENSAJES_MAXIMOS) {
            throw new NotificacionInvalida("Una solicitud lleva de 1 a 50 mensajes.");
        }
        List<MensajeSolicitado> mensajes = new ArrayList<>();
        for (Object elemento : lista) {
            mensajes.add(mensaje(elemento));
        }
        if (tamano(contenido) > TAMANO_MAXIMO) {
            throw new NotificacionInvalida("El contenido pasa de 64 KB.");
        }
        return new SolicitudDeNotificaciones(periodo, autor, momento, mensajes);
    }

    private static MensajeSolicitado mensaje(Object elemento) {
        if (!(elemento instanceof Map<?, ?> mensaje)) {
            throw new NotificacionInvalida("Un mensaje no es un objeto.");
        }
        soloClaves(mensaje, CLAVES_DEL_MENSAJE, "un mensaje");
        String titulo = texto(mensaje, "titulo");
        if (titulo.isBlank() || titulo.length() > TITULO_MAXIMO) {
            throw new NotificacionInvalida("El título debe tener de 1 a 200 caracteres.");
        }
        String detalle = texto(mensaje, "detalle");
        if (detalle.length() > DETALLE_MAXIMO) {
            throw new NotificacionInvalida("El detalle tiene más de 1000 caracteres.");
        }
        String enlace = texto(mensaje, "enlace");
        if (!ENLACE.matcher(enlace).matches()) {
            throw new NotificacionInvalida("El enlace no es una ruta de la SPA: " + ENLACE);
        }
        return new MensajeSolicitado(destino(mensaje.get("destino")), titulo, detalle, enlace);
    }

    // Exactamente una de rol, usuario o identificacion (D-110); la carrera, solo con el rol. La
    // identificación es un dato personal: un error no la repite.
    private static DestinoDelMensaje destino(Object valor) {
        if (!(valor instanceof Map<?, ?> destino)) {
            throw new NotificacionInvalida("Falta el destino de un mensaje, o no es un objeto.");
        }
        soloClaves(destino, CLAVES_DEL_DESTINO, "un destino");
        long claves = CLAVES_DE_QUIEN.stream().filter(destino::containsKey).count();
        if (claves != 1) {
            throw new NotificacionInvalida(
                    "Un destino lleva exactamente una de las claves rol, usuario o identificacion.");
        }
        if (destino.containsKey("usuario")) {
            if (destino.containsKey("carrera")) {
                throw new NotificacionInvalida("La carrera solo va en un destino por rol.");
            }
            String usuario = texto(destino, "usuario");
            if (usuario.isEmpty() || usuario.length() > USUARIO_MAXIMO) {
                throw new NotificacionInvalida(
                        "El usuario del destino debe tener de 1 a 64 caracteres.");
            }
            return new DestinoDelMensaje.PorUsuario(usuario);
        }
        if (destino.containsKey("identificacion")) {
            if (destino.containsKey("carrera")) {
                throw new NotificacionInvalida("La carrera solo va en un destino por rol.");
            }
            if (!(destino.get("identificacion") instanceof String identificacion)
                    || !IDENTIFICACION.matcher(identificacion).matches()) {
                throw new NotificacionInvalida(
                        "La identificación del destino no tiene la forma esperada.");
            }
            return new DestinoDelMensaje.PorIdentificacion(identificacion);
        }
        String rol = texto(destino, "rol");
        if (!ROLES.contains(rol)) {
            throw new NotificacionInvalida("El rol del destino no es uno de los diez de D-63.");
        }
        Object carrera = destino.get("carrera");
        if (carrera == null) {
            return new DestinoDelMensaje.PorRol(rol, Optional.empty());
        }
        if (!(carrera instanceof String codigo) || !CARRERA.matcher(codigo).matches()) {
            throw new NotificacionInvalida("La carrera del destino no tiene la forma " + CARRERA);
        }
        return new DestinoDelMensaje.PorRol(rol, Optional.of(codigo));
    }

    private static void soloClaves(Map<?, ?> mapa, Set<String> admitidas, String donde) {
        for (Object clave : mapa.keySet()) {
            if (!admitidas.contains(String.valueOf(clave))) {
                throw new NotificacionInvalida(
                        "Una clave de " + donde + " no está en el contrato.");
            }
        }
    }

    private static String texto(Map<?, ?> mapa, String campo) {
        if (!(mapa.get(campo) instanceof String texto)) {
            throw new NotificacionInvalida("Falta el campo " + campo + ", o no es un texto.");
        }
        return texto;
    }

    private static Instant momento(String texto) {
        try {
            return OffsetDateTime.parse(texto).toInstant();
        } catch (DateTimeParseException e) {
            throw new NotificacionInvalida("El campo momento no es una fecha y hora ISO 8601.");
        }
    }

    // La suma, en UTF-8, de cada clave y de cada valor convertido en texto, a cualquier
    // profundidad: la misma medida que la bitácora (F17-H01).
    private static int tamano(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            int total = 0;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                total += bytes(String.valueOf(entrada.getKey())) + tamano(entrada.getValue());
            }
            return total;
        }
        if (valor instanceof Collection<?> lista) {
            int total = 0;
            for (Object elemento : lista) {
                total += tamano(elemento);
            }
            return total;
        }
        return valor == null ? 0 : bytes(String.valueOf(valor));
    }

    private static int bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8).length;
    }
}
