package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Supplier;

/** Datos y dobles comunes de las pruebas de la aplicación de Académico. */
final class DatosDePrueba {

    static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    static final String QUIEN = "vicerrectorado.demo";
    static final String CODIGO = "2026-2";
    static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    static final LocalDate FIN = LocalDate.of(2027, 2, 28);

    private DatosDePrueba() {}

    static PeriodoAcademico periodo(EstadoPeriodo estado) {
        return new PeriodoAcademico(CODIGO, "Periodo 2026-2", INICIO, FIN, estado);
    }

    /**
     * Una transacción que ejecuta el trabajo en el acto y sabe si está dentro de él, para comprobar
     * que la llamada a Normativa queda fuera (research §3).
     */
    static final class TransaccionDePrueba implements Transaccion {

        private boolean dentro;

        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            dentro = true;
            try {
                return trabajo.get();
            } finally {
                dentro = false;
            }
        }

        boolean dentro() {
            return dentro;
        }
    }
}
