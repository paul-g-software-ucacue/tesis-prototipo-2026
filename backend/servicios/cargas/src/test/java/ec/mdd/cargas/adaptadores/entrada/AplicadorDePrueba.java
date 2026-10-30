package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.AplicadorPorTraductores;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TraductorDeCarga;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Un dueño en memoria de las 10 cargas, escrito a mano para las pruebas de Cargas (principio VIII):
 * la clave de una fila es su número, y sus campos son todos sus valores. Puede rechazar una fila
 * con la excepción que se le diga. Dice que tiene datos de todas las cargas, salvo las que se le
 * digan, para que el orden de las cargas (F01-H05) no estorbe a las pruebas que no son del orden.
 */
public final class AplicadorDePrueba implements AplicadorDeCargas {

    /** Las 10 cargas del diccionario. */
    static final Set<String> CARGAS =
            Set.of(
                    "estructura_organica",
                    "carreras",
                    "ciclos_academicos",
                    "asignaturas_malla",
                    "oferta_periodo",
                    "docentes",
                    "politica_carga_horaria",
                    "cargos_autoridades",
                    "proyectos_practicas",
                    "matriculados_carrera");

    private final Map<String, Map<String, Map<String, String>>> datos = new HashMap<>();
    private final AplicadorDeCargas aplicador;
    private final Set<String> sinDatos = new HashSet<>();
    private RuntimeException rechazo;

    /** Un dueño sin datos. */
    public AplicadorDePrueba() {
        List<TraductorDeCarga<?>> traductores =
                CARGAS.stream().<TraductorDeCarga<?>>map(EnMemoria::new).toList();
        aplicador = new AplicadorPorTraductores(traductores, trabajo -> trabajo.get());
    }

    /** Desde ahora, la primera fila que se aplique falla con esta excepción. */
    public void rechazarCon(RuntimeException excepcion) {
        rechazo = excepcion;
    }

    /** Desde ahora, no tiene datos de estas cargas. */
    public void sinDatosDe(String... cargas) {
        sinDatos.addAll(List.of(cargas));
    }

    /** Vacía los datos, deja de rechazar y vuelve a tener datos de todas las cargas. */
    public void reiniciar() {
        datos.clear();
        sinDatos.clear();
        rechazo = null;
    }

    @Override
    public Set<String> cargas() {
        return aplicador.cargas();
    }

    @Override
    public boolean tieneDatos(String carga) {
        return aplicador.tieneDatos(carga);
    }

    @Override
    public boolean tieneDatosEn(String carga, String periodo) {
        return aplicador.tieneDatosEn(carga, periodo);
    }

    @Override
    public VistaPreviaDelLote previsualizar(LoteDeCarga lote) {
        return aplicador.previsualizar(lote);
    }

    @Override
    public ResultadoDelLote aplicar(LoteDeCarga lote, String huella, String quien) {
        return aplicador.aplicar(lote, huella, quien);
    }

    /** La carga en memoria: la fila es la clave. */
    private final class EnMemoria implements TraductorDeCarga<Map<String, String>> {

        private final String carga;

        EnMemoria(String carga) {
            this.carga = carga;
        }

        @Override
        public String carga() {
            return carga;
        }

        @Override
        public boolean tieneDatos() {
            return !sinDatos.contains(carga);
        }

        @Override
        public void bloquear() {}

        @Override
        public Map<String, Map<String, String>> existentes(LoteDeCarga lote) {
            return datos.getOrDefault(carga, Map.of());
        }

        @Override
        public String clave(FilaDelLote fila) {
            return String.format("%05d", fila.fila());
        }

        @Override
        public Map<String, String> campos(Map<String, String> existente) {
            return existente;
        }

        @Override
        public Map<String, String> campos(FilaDelLote fila) {
            return new TreeMap<>(fila.valores());
        }

        @Override
        public void registrar(FilaDelLote fila, String quien) {
            if (rechazo != null) {
                throw rechazo;
            }
            datos.computeIfAbsent(carga, c -> new HashMap<>()).put(clave(fila), campos(fila));
        }

        @Override
        public void corregir(FilaDelLote fila, Map<String, String> actual, String quien) {
            registrar(fila, quien);
        }
    }
}
