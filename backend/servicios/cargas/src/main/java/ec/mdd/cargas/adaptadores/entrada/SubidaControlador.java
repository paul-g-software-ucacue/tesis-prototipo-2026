package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.FormatoDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.LecturaDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.SubidaApi;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Subir el archivo de una carga y validar su estructura ({@code contratos/cargas/openapi.yaml},
 * RF-06, RF-07, F01-H02). Exige «Cargas: escritura», como dice su {@code x-mdd-permiso} (ADR-021,
 * D-71); {@code PermisosCargasTest} lo comprueba. El archivo no se guarda ni se registra; cada
 * rechazo responde su código de {@link ErroresCargas}, y una carga que no existe, 404.
 */
@RestController
class SubidaControlador implements SubidaApi {

    private final ValidarArchivo validar;

    SubidaControlador(ValidarArchivo validar) {
        this.validar = validar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CARGAS:ESCRITURA')")
    public ResponseEntity<LecturaDeArchivo> validarArchivo(String codigo, MultipartFile archivo) {
        ArchivoValidado validado;
        try {
            validado =
                    validar.validar(
                            codigo,
                            new ArchivoSubido(archivo.getOriginalFilename(), archivo.getBytes()));
        } catch (CargaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        return ResponseEntity.ok(
                new LecturaDeArchivo(
                        validado.carga(),
                        validado.archivo(),
                        FormatoDeArchivo.fromValue(validado.formato().name()),
                        validado.registros()));
    }
}
