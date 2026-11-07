package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import ec.mdd.usuarios.adaptadores.entrada.api.AccionDeBitacora;
import ec.mdd.usuarios.adaptadores.entrada.api.BitacoraApi;
import ec.mdd.usuarios.adaptadores.entrada.api.EntradaConsultada;
import ec.mdd.usuarios.adaptadores.entrada.api.PaginaDeBitacora;
import ec.mdd.usuarios.adaptadores.entrada.api.ServicioDeBitacora;
import ec.mdd.usuarios.dominio.FiltroDeBitacoraInvalido;
import ec.mdd.usuarios.puertos.entrada.ConsultarBitacora;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consulta de la bitácora ({@code contratos/usuarios/openapi.yaml}, F17-H02). Solo lee. Las dos
 * operaciones exigen «Bitácora: lectura», como dice su {@code x-mdd-permiso} (ADR-021): sin él, el
 * 403 común, que no dice qué permiso falta.
 *
 * <p>Un filtro que no cumple su regla responde {@code COMUN_SOLICITUD_INVALIDA} con el detalle de
 * su campo, igual que la validación del contrato (FR-010).
 */
@RestController
class BitacoraControlador implements BitacoraApi {

    private final ConsultarBitacora consultar;

    BitacoraControlador(ConsultarBitacora consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_BITACORA:LECTURA')")
    public ResponseEntity<PaginaDeBitacora> consultarBitacora(
            LocalDate desde,
            LocalDate hasta,
            String usuario,
            ServicioDeBitacora servicio,
            String accion,
            String identificador,
            String periodo,
            Integer pagina,
            Integer tamano,
            OffsetDateTime corte) {
        PaginaDeEntradas resultado;
        try {
            resultado =
                    consultar.consultar(
                            new ConsultarBitacora.Filtro(
                                    desde,
                                    hasta,
                                    usuario,
                                    servicio == null ? null : servicio.getValue(),
                                    accion,
                                    identificador,
                                    periodo),
                            pagina,
                            tamano,
                            corte == null ? null : corte.toInstant());
        } catch (FiltroDeBitacoraInvalido error) {
            throw new SolicitudInvalida(
                    List.of(new DetalleRespuesta(error.campo(), error.motivo())));
        }
        return ResponseEntity.ok(
                new PaginaDeBitacora(
                        resultado.entradas().stream().map(BitacoraControlador::entrada).toList(),
                        resultado.total(),
                        resultado.pagina(),
                        resultado.tamano(),
                        resultado.corte().atOffset(ZoneOffset.UTC)));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_BITACORA:LECTURA')")
    public ResponseEntity<List<AccionDeBitacora>> listarAccionesDeBitacora() {
        return ResponseEntity.ok(
                consultar.acciones().stream()
                        .map(
                                una ->
                                        new AccionDeBitacora(
                                                ServicioDeBitacora.fromValue(una.servicio()),
                                                una.accion()))
                        .toList());
    }

    private static EntradaConsultada entrada(
            ec.mdd.usuarios.puertos.entrada.EntradaConsultada una) {
        EntradaConsultada entrada =
                new EntradaConsultada(
                        una.id(),
                        ServicioDeBitacora.fromValue(una.servicio()),
                        una.usuario(),
                        una.accion(),
                        una.momento().atOffset(ZoneOffset.UTC),
                        una.objeto());
        una.periodo().ifPresent(entrada::periodo);
        una.anterior().ifPresent(entrada::anterior);
        una.nuevo().ifPresent(entrada::nuevo);
        return entrada;
    }
}
