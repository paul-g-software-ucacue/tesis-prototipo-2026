package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.aplicacion.ConsultarDocentesServicio;
import ec.mdd.docentes.aplicacion.MantenerDocentesServicio;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Un doble en memoria de los puertos de salida de Docentes, para las pruebas de contrato: los
 * servicios son los reales, con las reglas del dominio, y las referencias son las de las cargas 02
 * y 04 de prueba.
 */
final class DocentesEnMemoria implements RepositorioDocentes, RegistroDeCambios, Transaccion {

    static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    private final Map<String, Docente> docentes = new LinkedHashMap<>();
    private final List<CambioRegistrado> cambios = new ArrayList<>();

    void reiniciar() {
        docentes.clear();
        cambios.clear();
    }

    List<CambioRegistrado> cambios() {
        return List.copyOf(cambios);
    }

    ConsultarDocentes consultar() {
        return new ConsultarDocentesServicio(this, DocentesDePrueba::referencias);
    }

    MantenerDocentes mantener() {
        return new MantenerDocentesServicio(
                this,
                DocentesDePrueba::referencias,
                this,
                this,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Override
    public List<Docente> todos() {
        return docentes.values().stream()
                .sorted(Comparator.comparing(docente -> docente.datos().nombreCompleto()))
                .toList();
    }

    @Override
    public Optional<Docente> buscar(String identificacion) {
        return Optional.ofNullable(docentes.get(identificacion));
    }

    @Override
    public boolean existeCorreo(String correo, String salvo) {
        return docentes.values().stream()
                .filter(docente -> !docente.identificacion().numero().equals(salvo))
                .anyMatch(
                        docente ->
                                docente.datos().correo().equals(correo.toLowerCase(Locale.ROOT)));
    }

    @Override
    public void bloquear() {}

    @Override
    public void registrar(Docente docente, String quien, Instant momento) {
        docentes.put(docente.identificacion().numero(), docente);
    }

    @Override
    public void guardar(Docente docente) {
        docentes.put(docente.identificacion().numero(), docente);
    }

    @Override
    public void registrar(CambioRegistrado cambio) {
        cambios.add(cambio);
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return trabajo.get();
    }
}
