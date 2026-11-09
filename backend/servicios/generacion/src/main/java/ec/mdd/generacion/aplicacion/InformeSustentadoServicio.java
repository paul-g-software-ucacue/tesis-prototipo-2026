package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ArchivoPdf;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.puertos.entrada.ConsolidadoEnRevision;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.EnRevision;
import ec.mdd.generacion.puertos.entrada.GestionarInforme;
import ec.mdd.generacion.puertos.entrada.InformeIlegible;
import ec.mdd.generacion.puertos.entrada.InformeNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinConsolidar;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.AlmacenDeArchivos;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Adjunta y descarga el informe sustentado (RF-32, D-88). El archivo se guarda primero, fuera de la
 * transacción: si falla no queda fila, y si falla la fila queda un archivo que nadie ve. Al
 * descargar se comprueba la huella: nunca se entrega un archivo dañado.
 */
public final class InformeSustentadoServicio implements GestionarInforme {

    private final ConsultarConsolidado consultar;
    private final AlmacenDeArchivos almacen;
    private final RepositorioConsolidacion consolidacion;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final EnRevision enRevision;
    private final Clock reloj;

    /** Recibe la consulta y los puertos de salida. */
    public InformeSustentadoServicio(
            ConsultarConsolidado consultar,
            AlmacenDeArchivos almacen,
            RepositorioConsolidacion consolidacion,
            RegistroDeCambios registro,
            Transaccion transaccion,
            EnRevision enRevision,
            Clock reloj) {
        this.consultar = consultar;
        this.almacen = almacen;
        this.consolidacion = consolidacion;
        this.registro = registro;
        this.transaccion = transaccion;
        this.enRevision = enRevision;
        this.reloj = reloj;
    }

    @Override
    public InformeSustentado adjuntar(
            String periodo, String nombre, byte[] contenido, String quien) {
        ConsolidadoDelPeriodo actual = consultar.consolidado(periodo);
        UUID version = actual.version().orElseThrow(SinDistributivo::new);
        if (!actual.modificable()) {
            throw new PeriodoCerrado();
        }
        if (actual.consolidacion().isEmpty()) {
            throw new SinConsolidar();
        }
        // #215, D-99, research §2: antes de guardar el archivo, porque el almacén no borra; y otra
        // vez dentro de la transacción, por si el envío a revisión llega entre las dos.
        if (enRevision.enRevision(periodo)) {
            throw new ConsolidadoEnRevision();
        }
        ArchivoPdf archivo = ArchivoPdf.de(nombre, contenido);
        String clave = almacen.guardar(archivo.contenido());
        Instant ahora = reloj.instant();
        InformeSustentado informe =
                new InformeSustentado(
                        archivo.nombre(), contenido.length, archivo.huella(), clave, quien, ahora);
        return transaccion.ejecutar(
                () -> {
                    if (enRevision.enRevision(periodo)) {
                        throw new ConsolidadoEnRevision();
                    }
                    consolidacion.guardarInforme(version, informe);
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.INFORME_ADJUNTADO,
                                    Map.of(
                                            "periodo", periodo,
                                            "version", actual.numero(),
                                            "huella", informe.huella(),
                                            "tamano", informe.tamano()),
                                    actual.informe()
                                            .map(InformeSustentadoServicio::valores)
                                            .orElse(Map.of()),
                                    valores(informe)));
                    return informe;
                });
    }

    @Override
    public byte[] descargar(String periodo) {
        InformeSustentado informe =
                consultar.consolidado(periodo).informe().orElseThrow(InformeNoEncontrado::new);
        byte[] contenido = leer(informe);
        if (!ArchivoPdf.sha256(contenido).equals(informe.huella())) {
            throw new InformeIlegible();
        }
        return contenido;
    }

    private byte[] leer(InformeSustentado informe) {
        try {
            return almacen.leer(informe.clave());
        } catch (UncheckedIOException | IllegalArgumentException error) {
            throw new InformeIlegible();
        }
    }

    private static Map<String, Object> valores(InformeSustentado informe) {
        return Map.of("huella", informe.huella(), "tamano", informe.tamano());
    }
}
