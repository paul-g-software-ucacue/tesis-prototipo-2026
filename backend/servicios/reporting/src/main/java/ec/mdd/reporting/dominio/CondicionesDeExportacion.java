package ec.mdd.reporting.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Si la versión publicada vigente se puede exportar a SIGA (RF-53, F19-H02, D-120): firmada por
 * toda su cadena y sin inconsistencias abiertas.
 *
 * <p><b>Firmada:</b> el documento firmable se generó de esta versión (su hash de contenido es el de
 * la publicación), tiene al menos un paso, todos firmados, y sus firmas no se anularon ni se
 * rechazó en firma. En el flujo normal una versión no llega a PUBLICADO sin eso; RF-53 pide
 * comprobarlo, no darlo por hecho.
 *
 * <p><b>Sin inconsistencias:</b> 0 duras y 0 blandas sin justificar (MD-07), las que Generación
 * registró al congelar la versión, sin volver a evaluarla con las reglas de hoy. Si no hay
 * registro, no se exporta: solo sale lo que se pudo comprobar.
 *
 * <p>Junta todos los motivos, sin cortar en el primero, para decir de una vez todo lo que falla.
 */
public final class CondicionesDeExportacion {

    private final Publicacion publicacion;
    private final Optional<FirmasDeLaVersion> firmas;
    private final Optional<InconsistenciasAbiertas> inconsistencias;
    private final List<MotivoDeRechazo> motivos;

    private CondicionesDeExportacion(
            Publicacion publicacion,
            Optional<FirmasDeLaVersion> firmas,
            Optional<InconsistenciasAbiertas> inconsistencias) {
        this.publicacion = Objects.requireNonNull(publicacion, "publicacion");
        this.firmas = Objects.requireNonNull(firmas, "firmas");
        this.inconsistencias = Objects.requireNonNull(inconsistencias, "inconsistencias");
        this.motivos = List.copyOf(evaluar());
    }

    /**
     * @param publicacion la versión publicada vigente
     * @param firmas la cadena de su documento; vacío si la versión no tiene documento
     * @param inconsistencias las abiertas registradas al congelarla; vacío si no se registraron
     * @return las condiciones, con sus motivos ya evaluados
     */
    public static CondicionesDeExportacion de(
            Publicacion publicacion,
            Optional<FirmasDeLaVersion> firmas,
            Optional<InconsistenciasAbiertas> inconsistencias) {
        return new CondicionesDeExportacion(publicacion, firmas, inconsistencias);
    }

    private List<MotivoDeRechazo> evaluar() {
        List<MotivoDeRechazo> hallados = new ArrayList<>();
        if (firmas.isEmpty()) {
            hallados.add(MotivoDeRechazo.SIN_DOCUMENTO_FIRMADO);
        } else {
            FirmasDeLaVersion cadena = firmas.get();
            if (!cadena.hashContenido().equals(publicacion.hash())) {
                hallados.add(MotivoDeRechazo.DOCUMENTO_DE_OTRA_VERSION);
            }
            if (cadena.anulada() || cadena.rechazada()) {
                hallados.add(MotivoDeRechazo.FIRMAS_ANULADAS_O_RECHAZADAS);
            }
            if (cadena.total() == 0 || cadena.firmados() < cadena.total()) {
                hallados.add(MotivoDeRechazo.FIRMAS_INCOMPLETAS);
            }
        }
        if (inconsistencias.isEmpty()) {
            hallados.add(MotivoDeRechazo.INCONSISTENCIAS_SIN_REGISTRO);
        } else if (!inconsistencias.get().ninguna()) {
            hallados.add(MotivoDeRechazo.INCONSISTENCIAS_ABIERTAS);
        }
        return hallados;
    }

    /** Lo que impide exportar, en el orden de {@link MotivoDeRechazo}; vacío si se puede. */
    public List<MotivoDeRechazo> motivos() {
        return motivos;
    }

    /** La cadena que informó Firma, para los números del mensaje. */
    public Optional<FirmasDeLaVersion> firmas() {
        return firmas;
    }

    /** Las inconsistencias registradas al congelar, para los números del mensaje. */
    public Optional<InconsistenciasAbiertas> inconsistencias() {
        return inconsistencias;
    }

    /**
     * @throws ExportacionRechazada si hay algún motivo
     */
    public void exigir() {
        if (!motivos.isEmpty()) {
            throw new ExportacionRechazada(publicacion.periodo(), publicacion.version(), this);
        }
    }
}
