package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Doble de los casos de uso de Normativa escrito a mano (principio VIII): las versiones en memoria,
 * cambiadas con las operaciones reales del agregado. Empieza con la versión 1 y una versión 2 sin
 * periodo copiada de ella.
 */
final class NormativaEnMemoria
        implements ConsultarNormativa, CrearVersion, EditarVersion, AsociarVersionAPeriodo {

    static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");

    private final Map<Integer, VersionNormativa> versiones = new TreeMap<>();
    private String ultimoUsuario;

    NormativaEnMemoria() {
        reiniciar();
    }

    /** Vuelve a la versión 1 y la 2, para que cada prueba empiece igual. */
    void reiniciar() {
        versiones.clear();
        versiones.put(1, NormativaContratoTest.VERSION_1);
        versiones.put(
                2,
                VersionNormativa.copiaDe(
                                NormativaContratoTest.VERSION_1,
                                2,
                                new DatosDeVersion(
                                        "SENESCYT-2025-01",
                                        "Reforma de la política 2025",
                                        LocalDate.of(2025, 1, 15)),
                                AHORA)
                        .version());
        ultimoUsuario = null;
    }

    /** El usuario que recibió la última operación de escritura. */
    String ultimoUsuario() {
        return ultimoUsuario;
    }

    /** Asocia la versión a un periodo, como lo hará Académico. */
    void asociar(int numero, String periodo) {
        versiones.put(numero, versiones.get(numero).asociadaA(periodo, AHORA));
    }

    @Override
    public List<VersionNormativa> versiones() {
        return List.copyOf(versiones.values());
    }

    @Override
    public VersionNormativa version(int numero) {
        VersionNormativa version = versiones.get(numero);
        if (version == null) {
            throw new VersionNoEncontrada(numero);
        }
        return version;
    }

    @Override
    public VersionNormativa crear(int copiarDe, DatosDeVersion datos, String quien) {
        ultimoUsuario = quien;
        VersionNormativa origen = version(copiarDe);
        if (versiones.values().stream().anyMatch(v -> v.codigo().equals(datos.codigo()))) {
            throw new CodigoRepetido(datos.codigo());
        }
        int numero = versiones.size() + 1;
        VersionNormativa nueva = VersionNormativa.copiaDe(origen, numero, datos, AHORA).version();
        versiones.put(numero, nueva);
        return nueva;
    }

    @Override
    public VersionNormativa editar(int numero, CambiosDeVersion cambios, String quien) {
        ultimoUsuario = quien;
        VersionNormativa editada = version(numero).editada(cambios).version();
        versiones.put(numero, editada);
        return editada;
    }

    @Override
    public AsignacionDelPeriodo periodo(String periodo) {
        return versiones.values().stream()
                .filter(version -> version.periodos().contains(periodo))
                .findFirst()
                .map(version -> new AsignacionDelPeriodo(periodo, version.numero(), AHORA))
                .orElseThrow(() -> new PeriodoSinVersion(periodo));
    }

    @Override
    public AsignacionDelPeriodo asociar(String periodo, int version, String quien) {
        ultimoUsuario = quien;
        for (VersionNormativa otra : versiones.values()) {
            if (otra.periodos().contains(periodo) && otra.numero() != version) {
                throw new PeriodoConVersion(periodo, otra.numero());
            }
        }
        asociar(this.version(version).numero(), periodo);
        return periodo(periodo);
    }
}
