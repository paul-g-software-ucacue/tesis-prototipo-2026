package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.adaptadores.entrada.api.FilaCargaHoraria;
import ec.mdd.normativa.adaptadores.entrada.api.FilaEditada;
import ec.mdd.normativa.adaptadores.entrada.api.Parametro;
import ec.mdd.normativa.adaptadores.entrada.api.PeriodoNormativo;
import ec.mdd.normativa.adaptadores.entrada.api.PeriodosApi;
import ec.mdd.normativa.adaptadores.entrada.api.Regla;
import ec.mdd.normativa.adaptadores.entrada.api.ReglaEditada;
import ec.mdd.normativa.adaptadores.entrada.api.Severidad;
import ec.mdd.normativa.adaptadores.entrada.api.TipoIst;
import ec.mdd.normativa.adaptadores.entrada.api.VersionDelPeriodo;
import ec.mdd.normativa.adaptadores.entrada.api.VersionEditada;
import ec.mdd.normativa.adaptadores.entrada.api.VersionNormativa;
import ec.mdd.normativa.adaptadores.entrada.api.VersionNueva;
import ec.mdd.normativa.adaptadores.entrada.api.VersionResumen;
import ec.mdd.normativa.adaptadores.entrada.api.VersionesApi;
import ec.mdd.normativa.dominio.CambioNoPermitido;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.FilaInvalida;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las versiones de normativa ({@code contratos/normativa/openapi.yaml}, RF-19): consultarlas
 * (F04-H01), crearlas, editarlas y asociarlas a un periodo (F04-H02). Leer exige «Parámetros y
 * normativa: lectura» y escribir, «Parámetros y normativa: escritura», como dice su {@code
 * x-mdd-permiso} (ADR-021, D-66, D-67); {@code PermisosNormativaTest} lo comprueba.
 *
 * <p>Una versión que no existe responde 404 con el código común. Una fila que no cumple sus
 * invariantes, o una edición que agrega o quita filas, reglas o parámetros, responde 400 con un
 * detalle por campo, en español (research §5). Las reglas del catálogo responden su 409.
 */
@RestController
class NormativaControlador implements VersionesApi, PeriodosApi {

    private static final String LECTURA = "hasAuthority('PERMISO_PARAMETROS:LECTURA')";
    private static final String ESCRITURA = "hasAuthority('PERMISO_PARAMETROS:ESCRITURA')";
    private static final String TODOS = "TODOS";

    private final ConsultarNormativa consultar;
    private final CrearVersion crear;
    private final EditarVersion editar;
    private final AsociarVersionAPeriodo asociar;

    NormativaControlador(
            ConsultarNormativa consultar,
            CrearVersion crear,
            EditarVersion editar,
            AsociarVersionAPeriodo asociar) {
        this.consultar = consultar;
        this.crear = crear;
        this.editar = editar;
        this.asociar = asociar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<VersionResumen>> listarVersiones() {
        return ResponseEntity.ok(
                consultar.versiones().stream()
                        .map(
                                version ->
                                        new VersionResumen(
                                                        version.numero(),
                                                        version.codigo(),
                                                        version.origen(),
                                                        version.vigenteDesde(),
                                                        TipoIst.fromValue(version.tipoIst().name()),
                                                        version.periodos())
                                                .copiadaDe(version.copiadaDe()))
                        .toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<VersionNormativa> consultarVersion(Integer numero) {
        ec.mdd.normativa.dominio.VersionNormativa version;
        try {
            version = consultar.version(numero);
        } catch (VersionNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(respuesta(version));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<VersionNormativa> crearVersion(VersionNueva versionNueva) {
        DatosDeVersion datos =
                datos(
                        versionNueva.getCodigo(),
                        versionNueva.getOrigen(),
                        versionNueva.getVigenteDesde());
        ec.mdd.normativa.dominio.VersionNormativa creada;
        try {
            creada = crear.crear(versionNueva.getCopiarDe(), datos, quien());
        } catch (VersionNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(creada));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<VersionNormativa> editarVersion(
            Integer numero, VersionEditada versionEditada) {
        CambiosDeVersion cambios =
                new CambiosDeVersion(
                        datos(
                                versionEditada.getCodigo(),
                                versionEditada.getOrigen(),
                                versionEditada.getVigenteDesde()),
                        versionEditada.getTablaCargaHoraria().stream()
                                .map(NormativaControlador::horas)
                                .toList(),
                        parametros(versionEditada.getReglas()));
        try {
            return ResponseEntity.ok(respuesta(editar.editar(numero, cambios, quien())));
        } catch (VersionNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (FilaInvalida error) {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    HorasDeFila.campo(error.perfil(), error.aplicaA()),
                                    error.motivo())));
        } catch (CambioNoPermitido error) {
            throw new SolicitudInvalida(
                    List.of(new DetalleRespuesta(error.campo(), error.motivo())));
        }
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoNormativo> asociarPeriodo(
            String periodo, VersionDelPeriodo versionDelPeriodo) {
        try {
            return ResponseEntity.ok(
                    respuesta(asociar.asociar(periodo, versionDelPeriodo.getVersion(), quien())));
        } catch (VersionNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<PeriodoNormativo> consultarPeriodo(String periodo) {
        try {
            return ResponseEntity.ok(respuesta(consultar.periodo(periodo)));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static PeriodoNormativo respuesta(AsignacionDelPeriodo asignacion) {
        return new PeriodoNormativo(
                asignacion.periodo(),
                asignacion.version(),
                asignacion.asociadaEn().atOffset(ZoneOffset.UTC));
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // El contrato exige que no estén vacíos; en blanco tampoco valen.
    private static DatosDeVersion datos(String codigo, String origen, LocalDate vigenteDesde) {
        List<DetalleRespuesta> detalles = new ArrayList<>();
        if (codigo.isBlank()) {
            detalles.add(new DetalleRespuesta("codigo", "El código no puede estar en blanco"));
        }
        if (origen.isBlank()) {
            detalles.add(new DetalleRespuesta("origen", "El origen no puede estar en blanco"));
        }
        if (!detalles.isEmpty()) {
            throw new SolicitudInvalida(detalles);
        }
        return new DatosDeVersion(codigo.strip(), origen.strip(), vigenteDesde);
    }

    private static HorasDeFila horas(FilaEditada fila) {
        String aplicaA = fila.getAplicaA().getValue();
        return new HorasDeFila(
                TODOS.equals(aplicaA) ? null : ec.mdd.normativa.dominio.TipoIst.valueOf(aplicaA),
                fila.getPerfil(),
                fila.getDedicacionTotal(),
                fila.getHorasClase(),
                fila.getHorasClaseMinimo(),
                fila.getHorasClaseMaximo(),
                fila.getHorasOtrasDocencia(),
                fila.getHorasGestion(),
                fila.getHorasInvestigacionVinculacionMaximo());
    }

    // Una regla o un parámetro repetido en la solicitud es un error de entrada, no de la versión.
    private static Map<String, Map<String, BigDecimal>> parametros(List<ReglaEditada> reglas) {
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        for (ReglaEditada regla : reglas) {
            String campo = "reglas[" + regla.getIdentificador() + "]";
            Map<String, BigDecimal> valores = new LinkedHashMap<>();
            for (Parametro parametro : regla.getParametros()) {
                if (valores.put(parametro.getNombre(), parametro.getValor()) != null) {
                    throw repetido(campo + "." + parametro.getNombre(), "El parámetro");
                }
            }
            if (parametros.put(regla.getIdentificador(), valores) != null) {
                throw repetido(campo, "La regla");
            }
        }
        return parametros;
    }

    private static SolicitudInvalida repetido(String campo, String que) {
        return new SolicitudInvalida(List.of(new DetalleRespuesta(campo, que + " está repetido")));
    }

    private static VersionNormativa respuesta(ec.mdd.normativa.dominio.VersionNormativa version) {
        return new VersionNormativa(
                        version.numero(),
                        version.codigo(),
                        version.origen(),
                        version.vigenteDesde(),
                        TipoIst.fromValue(version.tipoIst().name()),
                        version.periodos(),
                        version.reglas().stream().map(NormativaControlador::regla).toList(),
                        version.tabla().filas().stream().map(NormativaControlador::fila).toList())
                .copiadaDe(version.copiadaDe());
    }

    private static Regla regla(ec.mdd.normativa.dominio.Regla regla) {
        Regla respuesta =
                new Regla(
                        regla.identificador(),
                        regla.parametros().entrySet().stream()
                                .map(
                                        parametro ->
                                                new Parametro(
                                                        parametro.getKey(),
                                                        sinCerosALaDerecha(parametro.getValue())))
                                .toList());
        if (regla.severidad() != null) {
            respuesta.setSeveridad(Severidad.fromValue(regla.severidad().name()));
        }
        return respuesta;
    }

    private static FilaCargaHoraria fila(ec.mdd.normativa.dominio.FilaCargaHoraria fila) {
        FilaCargaHoraria respuesta =
                new FilaCargaHoraria(
                        FilaCargaHoraria.AplicaAEnum.fromValue(
                                fila.aplicaATodos() ? "TODOS" : fila.aplicaA().name()),
                        fila.perfil(),
                        fila.dedicacionTotal(),
                        fila.horasClase(),
                        fila.horasClaseMinimo(),
                        fila.horasClaseMaximo(),
                        Severidad.fromValue(fila.severidad().name()),
                        fila.referencia());
        // Lo que la política no da queda en null y se omite en la respuesta (contrato).
        respuesta.setHorasOtrasDocencia(fila.horasOtrasDocencia());
        respuesta.setHorasGestion(fila.horasGestion());
        respuesta.setHorasInvestigacionVinculacionMaximo(
                fila.horasInvestigacionVinculacionMaximo());
        respuesta.setObservacion(fila.observacion());
        return respuesta;
    }

    // El 40.00 de la base se muestra como 40, sin notación científica.
    private static BigDecimal sinCerosALaDerecha(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }
}
