package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.validacionocs.adaptadores.entrada.api.CargaDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.ConfiguracionDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.EtapaDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.FlujoApi;
import ec.mdd.validacionocs.adaptadores.entrada.api.PasoDeFirma;
import ec.mdd.validacionocs.adaptadores.entrada.api.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.Etapa;
import ec.mdd.validacionocs.dominio.PasoFirma;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * La configuración del flujo y del árbol de firmas (RF-34, RF-58). La carga y la lee quien tiene
 * FLUJO_FIRMAS, hoy el Administrador; la pantalla la muestra, sin editarla (ADR-007).
 */
@RestController
class ConfiguracionControlador implements FlujoApi {

    private static final String LEER = "hasAuthority('PERMISO_FLUJO_FIRMAS:LECTURA')";
    private static final String ESCRIBIR = "hasAuthority('PERMISO_FLUJO_FIRMAS:ESCRITURA')";
    private static final String ARCHIVO_PLANTILLA = "flujo-por-defecto.yaml";
    private static final MediaType YAML = new MediaType("text", "yaml", StandardCharsets.UTF_8);

    private final CargarConfiguracion cargar;
    private final ConsultarConfiguracion consultar;

    ConfiguracionControlador(CargarConfiguracion cargar, ConsultarConfiguracion consultar) {
        this.cargar = cargar;
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<ConfiguracionDelFlujo> cargarConfiguracionDelFlujo(
            String periodo, MultipartFile archivo) {
        try {
            return ResponseEntity.ok(
                    respuesta(cargar.cargar(periodo, archivo.getBytes(), quien())));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<ConfiguracionDelFlujo> consultarConfiguracionDelFlujo(String periodo) {
        return ResponseEntity.ok(respuesta(consultar.dePeriodo(periodo)));
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<String> descargarPlantillaDelFlujo() {
        return ResponseEntity.ok()
                .contentType(YAML)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(ARCHIVO_PLANTILLA)
                                .build()
                                .toString())
                .body(consultar.plantilla());
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static ConfiguracionDelFlujo respuesta(ConfiguracionVigente vigente) {
        ConfiguracionDelFlujo respuesta =
                new ConfiguracionDelFlujo(
                        vigente.periodo(),
                        vigente.carga().isEmpty(),
                        vigente.modificable(),
                        vigente.configuracion().etapas().stream()
                                .map(ConfiguracionControlador::etapa)
                                .toList(),
                        vigente.configuracion().firmas().stream()
                                .map(ConfiguracionControlador::firma)
                                .toList());
        vigente.carga()
                .ifPresent(
                        carga -> {
                            respuesta.setVersion(carga.numero());
                            respuesta.setCargada(cargada(carga));
                        });
        return respuesta;
    }

    private static CargaDelFlujo cargada(CargaGuardada carga) {
        return new CargaDelFlujo(carga.por(), carga.en().atOffset(ZoneOffset.UTC));
    }

    private static EtapaDelFlujo etapa(Etapa etapa) {
        EtapaDelFlujo respuesta =
                new EtapaDelFlujo(
                        TipoDeEtapa.valueOf(etapa.tipo().name()),
                        etapa.tipo().esEsqueleto(),
                        etapa.activa(),
                        etapa.nombre(),
                        etapa.rol(),
                        etapa.admiteDevolucion());
        etapa.plazoDias().ifPresent(respuesta::setPlazoDias);
        return respuesta;
    }

    private static PasoDeFirma firma(PasoFirma paso) {
        return new PasoDeFirma(
                paso.orden(),
                paso.etiqueta(),
                paso.rol(),
                TipoDeEtapa.valueOf(paso.etapa().name()),
                PasoDeFirma.DocumentoEnum.valueOf(paso.documento().name()));
    }
}
