package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.Carga;
import ec.mdd.cargas.adaptadores.entrada.api.ColumnaDeCarga;
import ec.mdd.cargas.adaptadores.entrada.api.Convencion;
import ec.mdd.cargas.adaptadores.entrada.api.DiccionarioDeDatos;
import ec.mdd.cargas.adaptadores.entrada.api.Obligatoriedad;
import ec.mdd.cargas.adaptadores.entrada.api.PlantillasApi;
import ec.mdd.cargas.adaptadores.entrada.api.ValorDeCatalogo;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las plantillas de las 10 cargas y su diccionario de datos ({@code contratos/cargas/openapi.yaml},
 * RF-05, F01-H01). Las descarga quien tiene «Cargas: lectura» o «Distributivo del periodo:
 * lectura», como dice su {@code x-mdd-permiso} (ADR-021, D-70); {@code PermisosCargasTest} lo
 * comprueba. Una carga que no existe responde 404 con el código común.
 *
 * <p>Los modelos del contrato se llaman como los del dominio, con los términos del glosario: los
 * del dominio van con su nombre completo (research §8).
 */
@RestController
class PlantillasControlador implements PlantillasApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_CARGAS:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA')";

    /** Las columnas del CSV del diccionario, una fila por columna de la plantilla (research §6). */
    private static final List<String> ENCABEZADO_DEL_DICCIONARIO =
            List.of(
                    "numero",
                    "campo",
                    "descripcion",
                    "tipo",
                    "longitud_maxima",
                    "obligatorio",
                    "formato_o_valores",
                    "clave",
                    "validacion",
                    "ejemplo");

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ConsultarPlantillas consultar;

    PlantillasControlador(ConsultarPlantillas consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<DiccionarioDeDatos> listarCargas() {
        ec.mdd.cargas.dominio.DiccionarioDeDatos diccionario = consultar.diccionario();
        return ResponseEntity.ok(
                new DiccionarioDeDatos(
                        diccionario.convenciones().stream()
                                .map(
                                        convencion ->
                                                new Convencion(
                                                        convencion.tema(), convencion.regla()))
                                .toList(),
                        diccionario.cargas().stream()
                                .map(carga -> respuesta(diccionario, carga))
                                .toList()));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<String> descargarPlantilla(String codigo) {
        ec.mdd.cargas.dominio.PlantillaCarga plantilla = existente(codigo).plantilla();
        return archivo(plantilla.archivo(), EscritorCsv.escribir(List.of(plantilla.encabezados())));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<String> descargarDiccionario(String codigo) {
        ec.mdd.cargas.dominio.Carga carga = existente(codigo);
        ec.mdd.cargas.dominio.DiccionarioDeDatos diccionario = consultar.diccionario();
        List<List<String>> filas = new ArrayList<>();
        filas.add(ENCABEZADO_DEL_DICCIONARIO);
        int numero = 1;
        for (ec.mdd.cargas.dominio.ColumnaDeCarga columna : carga.columnas()) {
            filas.add(
                    List.of(
                            String.valueOf(numero++),
                            columna.campo(),
                            columna.descripcion(),
                            columna.tipo(),
                            columna.longitud() == null ? "" : String.valueOf(columna.longitud()),
                            columna.obligatorio(),
                            formatoOValores(columna, diccionario.valoresDe(columna)),
                            columna.clave(),
                            columna.validacion(),
                            columna.ejemplo()));
        }
        filas.add(List.of());
        filas.add(List.of("tema", "regla"));
        for (ec.mdd.cargas.dominio.Convencion convencion : diccionario.convenciones()) {
            filas.add(List.of(convencion.tema(), convencion.regla()));
        }
        return archivo(carga.archivoDelDiccionario(), EscritorCsv.escribir(filas));
    }

    /**
     * El formato de la columna y, si toma los valores de un catálogo, esos valores: «G01 Rector;
     * G02 Vicerrector; …» (research §5).
     */
    private static String formatoOValores(
            ec.mdd.cargas.dominio.ColumnaDeCarga columna,
            List<ec.mdd.cargas.dominio.ValorDeCatalogo> valores) {
        if (valores.isEmpty()) {
            return columna.formato();
        }
        String lista =
                valores.stream()
                        .map(
                                valor ->
                                        valor.descripcion().isEmpty()
                                                ? valor.codigo()
                                                : valor.codigo() + " " + valor.descripcion())
                        .collect(Collectors.joining("; "));
        return columna.formato().isEmpty() ? lista : columna.formato() + ": " + lista;
    }

    private ec.mdd.cargas.dominio.Carga existente(String codigo) {
        try {
            return consultar.carga(codigo);
        } catch (CargaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static ResponseEntity<String> archivo(String nombre, String csv) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .body(csv);
    }

    private static Carga respuesta(
            ec.mdd.cargas.dominio.DiccionarioDeDatos diccionario,
            ec.mdd.cargas.dominio.Carga carga) {
        return new Carga(
                carga.numero(),
                carga.codigo(),
                carga.nombre(),
                carga.archivo(),
                carga.dependeDe(),
                Obligatoriedad.fromValue(carga.obligatoriedad().name()),
                carga.columnas().stream().map(columna -> respuesta(diccionario, columna)).toList());
    }

    private static ColumnaDeCarga respuesta(
            ec.mdd.cargas.dominio.DiccionarioDeDatos diccionario,
            ec.mdd.cargas.dominio.ColumnaDeCarga columna) {
        ColumnaDeCarga respuesta =
                new ColumnaDeCarga(
                                columna.campo(),
                                columna.descripcion(),
                                columna.tipo(),
                                columna.obligatorio(),
                                columna.ejemplo())
                        .longitud(columna.longitud())
                        .formato(vacioANulo(columna.formato()))
                        .clave(vacioANulo(columna.clave()))
                        .validacion(vacioANulo(columna.validacion()));
        List<ValorDeCatalogo> valores =
                diccionario.valoresDe(columna).stream()
                        .map(valor -> new ValorDeCatalogo(valor.codigo(), valor.descripcion()))
                        .toList();
        return valores.isEmpty() ? respuesta : respuesta.valores(valores);
    }

    private static String vacioANulo(String texto) {
        return texto.isEmpty() ? null : texto;
    }
}
