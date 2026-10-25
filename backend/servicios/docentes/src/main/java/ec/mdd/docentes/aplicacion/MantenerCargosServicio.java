package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.puertos.entrada.CargoConsultado;
import ec.mdd.docentes.puertos.entrada.CargoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.CargoRepetido;
import ec.mdd.docentes.puertos.entrada.CoordinadorVigente;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mantener los cargos (F03-H05). Las referencias de Académico se piden antes de la transacción,
 * como con los docentes. Dentro de ella se bloquean los cargos, se leen el docente, la clave y el
 * coordinador vigente de la carrera (RN-12), y se guarda y se registra. La respuesta trae las
 * advertencias de hoy, como la de RN-20.
 */
public final class MantenerCargosServicio implements MantenerCargos {

    private final RepositorioCargos repositorio;
    private final RepositorioDocentes docentes;
    private final ReferenciasAcademicas referencias;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerCargosServicio(
            RepositorioCargos repositorio,
            RepositorioDocentes docentes,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.docentes = docentes;
        this.referencias = referencias;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public CargoConsultado registrar(Cargo cargo, String quien) {
        referencias.referencias().verificar(cargo);
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    Docente suDocente = docente(cargo.docente());
                    if (repositorio
                            .buscar(cargo.docente(), cargo.tipo(), cargo.fechaDesignacion())
                            .isPresent()) {
                        throw new CargoRepetido();
                    }
                    cargo.verificarDocente(suDocente);
                    verificarCoordinador(cargo, null);
                    Instant ahora = reloj.instant();
                    repositorio.registrar(cargo, quien, ahora);
                    Map<String, Object> detalle = clave(cargo);
                    if (cargo.unidad() != null) {
                        detalle.put("unidad", cargo.unidad());
                    }
                    if (cargo.carrera() != null) {
                        detalle.put("carrera", cargo.carrera());
                    }
                    detalle.put("estado", cargo.estado().name());
                    registrar(ahora, quien, Accion.CARGO_REGISTRADO, detalle);
                    return consultado(cargo, suDocente);
                });
    }

    @Override
    public CargoConsultado corregir(Cargo cargo, String quien) {
        // Un cargo que se finaliza no se vuelve a comparar con Académico: su unidad pudo
        // inactivarse mientras duraba.
        if (cargo.estado() == EstadoDeCargo.VIGENTE) {
            referencias.referencias().verificar(cargo);
        }
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    Cargo actual =
                            repositorio
                                    .buscar(cargo.docente(), cargo.tipo(), cargo.fechaDesignacion())
                                    .orElseThrow(CargoNoEncontrado::new);
                    Docente suDocente = docente(cargo.docente());
                    Cargo.Correccion correccion = actual.corregido(cargo);
                    if (correccion.cambios().isEmpty()) {
                        return consultado(actual, suDocente);
                    }
                    // Finalizar no pide nada: así se cierra el cargo de un docente que dejó de
                    // estar activo a tiempo completo.
                    if (cargo.estado() == EstadoDeCargo.VIGENTE) {
                        cargo.verificarDocente(suDocente);
                        verificarCoordinador(cargo, actual);
                    }
                    repositorio.guardar(cargo);
                    Map<String, Object> detalle = clave(cargo);
                    detalle.put(
                            "cambios", correccion.cambios().stream().map(Detalles::de).toList());
                    registrar(reloj.instant(), quien, Accion.CARGO_CORREGIDO, detalle);
                    return consultado(cargo, suDocente);
                });
    }

    private void verificarCoordinador(Cargo cargo, Cargo salvo) {
        if (cargo.esCoordinadorVigente()
                && repositorio.hayCoordinadorVigente(cargo.carrera(), salvo)) {
            throw new CoordinadorVigente();
        }
    }

    private Docente docente(String identificacion) {
        return docentes.buscar(identificacion)
                .orElseThrow(() -> new DatoInvalido("docente", "El docente no está registrado"));
    }

    private CargoConsultado consultado(Cargo cargo, Docente suDocente) {
        return new CargoConsultado(
                cargo,
                suDocente.datos().nombreCompleto(),
                cargo.advertencias(suDocente, LocalDate.now(reloj)));
    }

    private static Map<String, Object> clave(Cargo cargo) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("docente", cargo.docente());
        detalle.put("tipo", cargo.tipo().name());
        detalle.put("fechaDesignacion", cargo.fechaDesignacion().toString());
        return detalle;
    }

    private void registrar(
            Instant momento, String quien, Accion accion, Map<String, Object> detalle) {
        registro.registrar(new CambioRegistrado(momento, quien, accion, detalle));
    }
}
