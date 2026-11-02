package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.CarreraConsolidada;
import ec.mdd.generacion.adaptadores.entrada.api.Consolidacion;
import ec.mdd.generacion.adaptadores.entrada.api.ConsolidadoApi;
import ec.mdd.generacion.adaptadores.entrada.api.ConsolidadoDelPeriodo;
import ec.mdd.generacion.adaptadores.entrada.api.EstadoDelParcial;
import ec.mdd.generacion.adaptadores.entrada.api.InformeSustentado;
import ec.mdd.generacion.adaptadores.entrada.api.TotalesDelPanel;
import ec.mdd.generacion.puertos.entrada.Consolidar;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.GestionarInforme;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El consolidado y el informe sustentado (RF-32). Lo leen Consolidación y el Vicerrectorado, y
 * escribe Consolidación (D-87). El nombre recibido solo se muestra: la descarga lleva uno fijo.
 */
@RestController
class ConsolidadoControlador implements ConsolidadoApi {

    private static final String LEER =
            "hasAnyAuthority('PERMISO_CONSOLIDACION:LECTURA', 'PERMISO_DISTRIBUTIVO:ESCRITURA')";
    private static final String ESCRIBIR = "hasAuthority('PERMISO_CONSOLIDACION:ESCRITURA')";

    private final ConsultarConsolidado consultar;
    private final Consolidar consolidar;
    private final GestionarInforme informe;

    ConsolidadoControlador(
            ConsultarConsolidado consultar, Consolidar consolidar, GestionarInforme informe) {
        this.consultar = consultar;
        this.consolidar = consolidar;
        this.informe = informe;
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<ConsolidadoDelPeriodo> consultarConsolidado(String periodo) {
        return ResponseEntity.ok(respuesta(consultar.consolidado(periodo)));
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<ConsolidadoDelPeriodo> consolidar(String periodo) {
        return ResponseEntity.ok(respuesta(consolidar.consolidar(periodo, quien())));
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<InformeSustentado> adjuntarInformeSustentado(
            String periodo, MultipartFile archivo) {
        try {
            return ResponseEntity.ok(
                    respuesta(
                            informe.adjuntar(
                                    periodo,
                                    archivo.getOriginalFilename(),
                                    archivo.getBytes(),
                                    quien())));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<Resource> descargarInformeSustentado(String periodo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("informe-sustentado-" + periodo + ".pdf")
                                .build()
                                .toString())
                .body(new ByteArrayResource(informe.descargar(periodo)));
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static ConsolidadoDelPeriodo respuesta(
            ec.mdd.generacion.dominio.ConsolidadoDelPeriodo consolidado) {
        ConsolidadoDelPeriodo respuesta =
                new ConsolidadoDelPeriodo(
                        consolidado.periodo(),
                        consolidado.creado(),
                        consolidado.modificable(),
                        consolidado.desactualizado(),
                        consolidado.carreras().stream()
                                .map(ConsolidadoControlador::respuesta)
                                .toList(),
                        CargaHorariaControlador.totales(consolidado.totales()),
                        new TotalesDelPanel(
                                Math.toIntExact(consolidado.duras()),
                                Math.toIntExact(consolidado.blandas())));
        if (consolidado.creado()) {
            respuesta.setVersion(consolidado.numero());
        }
        consolidado
                .consolidacion()
                .ifPresent(
                        una ->
                                respuesta.setConsolidacion(
                                        new Consolidacion(
                                                una.por(), una.en().atOffset(ZoneOffset.UTC))));
        consolidado
                .informe()
                .map(ConsolidadoControlador::respuesta)
                .ifPresent(respuesta::setInforme);
        return respuesta;
    }

    private static CarreraConsolidada respuesta(
            ec.mdd.generacion.dominio.CarreraConsolidada carrera) {
        return new CarreraConsolidada(
                carrera.carrera(),
                EstadoDelParcial.fromValue(carrera.estado().name()),
                carrera.avance().ofertadas(),
                carrera.avance().conPropuesta(),
                carrera.avance().completo(),
                CargaHorariaControlador.totales(carrera.horas()));
    }

    private static InformeSustentado respuesta(
            ec.mdd.generacion.dominio.InformeSustentado informe) {
        return new InformeSustentado(
                informe.nombre(),
                informe.tamano(),
                informe.huella(),
                informe.por(),
                informe.en().atOffset(ZoneOffset.UTC));
    }
}
