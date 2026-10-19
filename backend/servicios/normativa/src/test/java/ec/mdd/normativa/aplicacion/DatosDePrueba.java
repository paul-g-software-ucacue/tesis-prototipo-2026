package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Una versión pequeña y una transacción que solo ejecuta, para las pruebas de la aplicación. */
final class DatosDePrueba {

    static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");
    static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    static final String QUIEN = "vicerrectorado.demo";
    static final DatosDeVersion REFORMA =
            new DatosDeVersion(
                    "SENESCYT-2025-01", "Reforma de la política 2025", LocalDate.of(2025, 1, 15));

    /** Ejecuta el trabajo sin base: la atomicidad se prueba con PostgreSQL. */
    static final Transaccion DIRECTA =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };

    static VersionNormativa politica() {
        return new VersionNormativa(
                1,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                new TablaCargaHoraria(
                        List.of(
                                new FilaCargaHoraria(
                                        null,
                                        "DOCENTE_MT",
                                        20,
                                        12,
                                        8,
                                        12,
                                        8,
                                        0,
                                        null,
                                        Severidad.DURA,
                                        "POL 6",
                                        null))),
                List.of(
                        new Regla(
                                "RN-02",
                                Severidad.DURA,
                                Map.of("horas_maximas_docente", BigDecimal.valueOf(40)))));
    }

    static VersionNormativa dos() {
        return VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();
    }

    static CambiosDeVersion conClase(VersionNormativa version, DatosDeVersion datos, int clase) {
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas()
                .forEach(regla -> parametros.put(regla.identificador(), regla.parametros()));
        return new CambiosDeVersion(
                datos,
                version.tabla().filas().stream()
                        .map(fila -> HorasDeFila.de(fila).conHorasClase(clase))
                        .toList(),
                parametros);
    }

    private DatosDePrueba() {}
}
