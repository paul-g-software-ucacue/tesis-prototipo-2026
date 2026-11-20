package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.AdvertenciaDeRemision;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.PlazosDeEtapas;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Revision;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.NormativaDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carga una configuración: primero lo que no cuesta nada, después lo que pregunta a otros procesos,
 * y solo guarda si no hay ningún problema. Un rechazo no cambia lo que rige (FR-016). La carga y su
 * entrada de bitácora van en la misma transacción (F17-H01). Desde F12-H03 calcula la advertencia
 * de RN-23 de una configuración válida y la guarda con la carga, sin rechazarla (D-114).
 */
public final class CargarConfiguracionServicio implements CargarConfiguracion {

    private static final Logger LOG = LoggerFactory.getLogger(CargarConfiguracionServicio.class);

    private static final String MARCA_DE_ORDEN = "﻿";

    private final DatosDelPeriodo datos;
    private final NormativaDelPeriodo normativa;
    private final LectorDeConfiguracion lector;
    private final RepositorioConfiguracion repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos y el reloj. */
    public CargarConfiguracionServicio(
            DatosDelPeriodo datos,
            NormativaDelPeriodo normativa,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.datos = datos;
        this.normativa = normativa;
        this.lector = lector;
        this.repositorio = repositorio;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ConfiguracionVigente cargar(String periodo, byte[] archivo, String quien) {
        if (archivo.length > ConfiguracionFlujo.TAMANO_MAXIMO) {
            throw new ArchivoDemasiadoGrande();
        }
        EstadoDelPeriodo estado = comprobarPeriodo(periodo);
        String texto = texto(archivo);
        Revision revision =
                ConfiguracionFlujo.revisar(lector.leer(texto), datos.firmantes(periodo));
        ConfiguracionFlujo configuracion =
                revision.configuracion()
                        .orElseThrow(() -> new ConfiguracionInvalida(revision.problemas()));
        Instant ahora = reloj.instant();
        Optional<AdvertenciaDeRemision> advertencia =
                advertencia(periodo, configuracion, estado, ahora);
        CargaGuardada vigente =
                transaccion.ejecutar(
                        () -> {
                            Optional<CargaGuardada> anterior = repositorio.vigente(periodo);
                            repositorio.guardar(periodo, texto, quien, ahora, advertencia);
                            CargaGuardada nueva = repositorio.vigente(periodo).orElseThrow();
                            registro.registrar(
                                    new CambioDeFlujo(
                                            ahora,
                                            quien,
                                            CambioDeFlujo.Accion.CONFIGURACION_FLUJO_CARGADA,
                                            Map.of("periodo", periodo),
                                            anterior.<Map<String, Object>>map(
                                                            una -> Map.of("version", una.numero()))
                                                    .orElse(Map.of()),
                                            nuevo(nueva, advertencia)));
                            return nueva;
                        });
        return new ConfiguracionVigente(periodo, configuracion, Optional.of(vigente), true);
    }

    private EstadoDelPeriodo comprobarPeriodo(String periodo) {
        EstadoDelPeriodo estado = datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new);
        if (!estado.enPlanificacion()) {
            throw new PeriodoSinPlanificacion();
        }
        return estado;
    }

    // F12-H03 (D-114): la advertencia de RN-23 de una configuración válida, con el día de la carga
    // en Ecuador. No rechaza nada. Sin el inicio del periodo (un proceso datos anterior, D-55), no
    // se calcula. Si Normativa no responde, la carga falla con el error común, como con Académico.
    private Optional<AdvertenciaDeRemision> advertencia(
            String periodo,
            ConfiguracionFlujo configuracion,
            EstadoDelPeriodo estado,
            Instant ahora) {
        if (estado.inicio().isEmpty()) {
            LOG.warn("Académico no dio el inicio de {}: la carga no comprueba RN-23", periodo);
            return Optional.empty();
        }
        return AdvertenciaDeRemision.de(
                LocalDate.ofInstant(ahora, ProcesoAprobacion.ZONA),
                PlazosDeEtapas.de(configuracion.etapas()),
                estado.inicio().get(),
                normativa.diasDeRemision(periodo));
    }

    private static Map<String, Object> nuevo(
            CargaGuardada nueva, Optional<AdvertenciaDeRemision> advertencia) {
        return advertencia
                .<Map<String, Object>>map(
                        una ->
                                Map.of(
                                        "version",
                                        nueva.numero(),
                                        "advertencia",
                                        una.codigo().name()))
                .orElseGet(() -> Map.of("version", nueva.numero()));
    }

    // Estricto: un archivo que no es UTF-8 se rechaza, no se guarda con caracteres cambiados. Los
    // editores de Windows agregan la marca de orden, y esa no es un error de quien lo escribió.
    private static String texto(byte[] archivo) {
        try {
            String texto =
                    StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(archivo)).toString();
            return texto.startsWith(MARCA_DE_ORDEN)
                    ? texto.substring(MARCA_DE_ORDEN.length())
                    : texto;
        } catch (CharacterCodingException error) {
            throw new ArchivoIlegible();
        }
    }
}
