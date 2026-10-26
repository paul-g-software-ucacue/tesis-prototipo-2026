package ec.mdd.docentes.aplicacion.cargas;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La carga 08, los cargos y autoridades (F01-H04): sus columnas, traducidas a los cargos de
 * Docentes (FR-012), con su clave compuesta: identificación, cargo y fecha de designación. Registra
 * y corrige con {@link MantenerCargos}, así que se aplican sus reglas, como un coordinador vigente
 * por carrera (RN-12) o el docente activo a tiempo completo (POL 5.4).
 */
final class TraductorCargos extends TraductorDeDocentes<Cargo> {

    private final RepositorioCargos repositorio;
    private final MantenerCargos mantener;

    TraductorCargos(RepositorioCargos repositorio, MantenerCargos mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "cargos_autoridades";
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Cargo> existentes(LoteDeCarga lote) {
        Map<String, Cargo> existentes = new LinkedHashMap<>();
        repositorio
                .todos()
                .forEach(
                        cargo ->
                                existentes.put(
                                        clave(
                                                cargo.docente(),
                                                cargo.tipo().name(),
                                                cargo.fechaDesignacion().toString()),
                                        cargo));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return clave(
                fila.valor("numero_identificacion"),
                fila.valor("codigo_cargo"),
                fila.valor("fecha_designacion"));
    }

    @Override
    public Map<String, String> campos(Cargo cargo) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("codigo_unidad", texto(cargo.unidad()));
        campos.put("codigo_carrera", texto(cargo.carrera()));
        campos.put("fecha_fin", texto(cargo.fechaFin()));
        campos.put("documento_designacion", cargo.documento());
        campos.put("estado", cargo.estado().name());
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (String campo :
                java.util.List.of(
                        "codigo_unidad",
                        "codigo_carrera",
                        "fecha_fin",
                        "documento_designacion",
                        "estado")) {
            campos.put(campo, fila.valor(campo));
        }
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        mantener.registrar(cargo(fila), quien);
    }

    @Override
    public void corregir(FilaDelLote fila, Cargo actual, String quien) {
        mantener.corregir(cargo(fila), quien);
    }

    private static Cargo cargo(FilaDelLote fila) {
        return new Cargo(
                fila.valor("numero_identificacion"),
                TipoDeCargo.valueOf(fila.valor("codigo_cargo")),
                LocalDate.parse(fila.valor("fecha_designacion")),
                nulo(fila.valor("codigo_unidad")),
                nulo(fila.valor("codigo_carrera")),
                fecha(fila.valor("fecha_fin")),
                fila.valor("documento_designacion"),
                EstadoDeCargo.valueOf(fila.valor("estado")));
    }

    private static String clave(String docente, String cargo, String fecha) {
        return docente + " · " + cargo + " · " + fecha;
    }
}
