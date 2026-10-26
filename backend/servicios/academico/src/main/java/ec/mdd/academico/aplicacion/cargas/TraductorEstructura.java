package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La carga 01, la estructura orgánica (F01-H04): sus columnas, traducidas a las unidades de
 * Académico (FR-012). Registra y corrige con {@link MantenerEstructura}, así que se aplican sus
 * reglas, como una sola sede matriz o el padre que existe. Los padres se aplican antes que sus
 * hijos (research §4). El tipo de una unidad no cambia.
 */
final class TraductorEstructura extends TraductorAcademico<UnidadOrganica> {

    private final RepositorioEstructura repositorio;
    private final MantenerEstructura mantener;

    TraductorEstructura(RepositorioEstructura repositorio, MantenerEstructura mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "estructura_organica";
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, UnidadOrganica> existentes(LoteDeCarga lote) {
        return repositorio.todas().stream()
                .collect(
                        Collectors.toMap(
                                UnidadOrganica::codigo,
                                Function.identity(),
                                (una, otra) -> una,
                                LinkedHashMap::new));
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_unidad");
    }

    @Override
    public Map<String, String> campos(UnidadOrganica unidad) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", unidad.nombre());
        campos.put("tipo_unidad", unidad.tipo().name());
        campos.put("codigo_unidad_padre", texto(unidad.padre()));
        campos.put("codigo_gestion_autorizada", texto(unidad.gestionAutorizada()));
        campos.put("es_comision_permanente", sn(unidad.comisionPermanente()));
        campos.put("estado", unidad.activa() ? "ACTIVO" : "INACTIVO");
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (String campo :
                List.of(
                        "nombre",
                        "tipo_unidad",
                        "codigo_unidad_padre",
                        "codigo_gestion_autorizada",
                        "es_comision_permanente",
                        "estado")) {
            campos.put(campo, fila.valor(campo));
        }
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        String codigo = clave(fila);
        mantener.registrar(
                codigo,
                fila.valor("nombre"),
                TipoDeUnidad.valueOf(fila.valor("tipo_unidad")),
                nulo(fila.valor("codigo_unidad_padre")),
                nulo(fila.valor("codigo_gestion_autorizada")),
                comision(fila),
                quien);
        if (!activa(fila)) {
            mantener.cambiarEstado(codigo, false, quien);
        }
    }

    @Override
    public void corregir(FilaDelLote fila, UnidadOrganica actual, String quien) {
        if (!actual.tipo().name().equals(fila.valor("tipo_unidad"))) {
            throw new DatoInvalido(
                    "tipo_unidad",
                    "El tipo de una unidad no cambia: regístrala con otro código o corrige el"
                            + " archivo.");
        }
        Map<String, String> ahora = campos(actual);
        Map<String, String> archivo = campos(fila);
        ahora.remove("estado");
        archivo.remove("estado");
        String codigo = clave(fila);
        if (!ahora.equals(archivo)) {
            mantener.corregir(
                    codigo,
                    fila.valor("nombre"),
                    nulo(fila.valor("codigo_unidad_padre")),
                    nulo(fila.valor("codigo_gestion_autorizada")),
                    comision(fila),
                    quien);
        }
        if (actual.activa() != activa(fila)) {
            mantener.cambiarEstado(codigo, activa(fila), quien);
        }
    }

    /** Los padres antes que sus hijos, en el orden del archivo cuando no dependen entre sí. */
    @Override
    public List<FilaDelLote> enOrden(List<FilaDelLote> filas) {
        Set<String> delArchivo = new HashSet<>();
        filas.forEach(fila -> delArchivo.add(clave(fila)));
        List<FilaDelLote> pendientes = new ArrayList<>(filas);
        List<FilaDelLote> enOrden = new ArrayList<>();
        Set<String> aplicadas = new HashSet<>();
        boolean avanzo = true;
        while (!pendientes.isEmpty() && avanzo) {
            avanzo = false;
            for (FilaDelLote fila : List.copyOf(pendientes)) {
                String padre = fila.valor("codigo_unidad_padre");
                if (padre.isEmpty() || !delArchivo.contains(padre) || aplicadas.contains(padre)) {
                    enOrden.add(fila);
                    aplicadas.add(clave(fila));
                    pendientes.remove(fila);
                    avanzo = true;
                }
            }
        }
        // Un ciclo entre padres queda en el orden del archivo: la estructura lo rechaza.
        enOrden.addAll(pendientes);
        return enOrden;
    }

    private static boolean activa(FilaDelLote fila) {
        return !"INACTIVO".equals(fila.valor("estado"));
    }

    private static Boolean comision(FilaDelLote fila) {
        String valor = fila.valor("es_comision_permanente");
        return valor.isEmpty() ? null : "S".equals(valor);
    }

    private static String sn(Boolean valor) {
        return valor == null ? "" : valor ? "S" : "N";
    }
}
