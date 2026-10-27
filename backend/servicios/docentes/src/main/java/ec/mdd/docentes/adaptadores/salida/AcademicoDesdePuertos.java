package ec.mdd.docentes.adaptadores.salida;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Las referencias de Académico, pedidas por su interfaz publicada dentro del proceso: solo {@code
 * ec.mdd.academico.puertos.entrada}, como Académico con Normativa; {@code ModulosTest} del proceso
 * {@code datos} lo comprueba (principio V, research §4). El puerto refleja la operación {@code
 * consultarReferencias} del contrato de Académico. La autorización es la de la operación de
 * Docentes.
 */
public final class AcademicoDesdePuertos implements ReferenciasAcademicas {

    private final ConsultarReferencias consultar;

    /** Recibe el puerto publicado de Académico. */
    public AcademicoDesdePuertos(ConsultarReferencias consultar) {
        this.consultar = consultar;
    }

    @Override
    public Referencias referencias() {
        // Dentro de una transacción, como la de una carga por archivo (F01-H04), las referencias se
        // leen una vez y valen hasta que termina: confirmar 11 000 docentes no las lee 11 000
        // veces. Fuera de una transacción se leen cada vez, como antes.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return leer();
        }
        Referencias enLaTransaccion =
                (Referencias) TransactionSynchronizationManager.getResource(this);
        if (enLaTransaccion == null) {
            enLaTransaccion = leer();
            TransactionSynchronizationManager.bindResource(this, enLaTransaccion);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int estado) {
                            TransactionSynchronizationManager.unbindResourceIfPossible(
                                    AcademicoDesdePuertos.this);
                        }
                    });
        }
        return enLaTransaccion;
    }

    private Referencias leer() {
        ReferenciasDeAcademico todas = consultar.referencias();
        return new Referencias(
                todas.carreras().stream()
                        .collect(
                                Collectors.toMap(
                                        CarreraDeReferencia::codigo,
                                        carrera ->
                                                new Referencias.Carrera(
                                                        carrera.nombre(), carrera.siglas()))),
                todas.asignaturas().stream()
                        .collect(
                                Collectors.toMap(
                                        AsignaturaDeReferencia::codigo,
                                        asignatura ->
                                                new Referencias.Asignatura(
                                                        asignatura.nombre(),
                                                        asignatura.carrera()))),
                todas.camposAmplios().stream()
                        .collect(
                                Collectors.toMap(
                                        CampoAmplioDeReferencia::codigo,
                                        CampoAmplioDeReferencia::nombre)),
                todas.unidades().stream()
                        .collect(
                                Collectors.toMap(
                                        UnidadDeReferencia::codigo,
                                        unidad ->
                                                new Referencias.Unidad(
                                                        unidad.nombre(),
                                                        unidad.tipo(),
                                                        unidad.gestionAutorizada(),
                                                        unidad.activa()))),
                todas.periodos().stream()
                        .collect(
                                Collectors.toMap(
                                        PeriodoDeReferencia::codigo,
                                        periodo ->
                                                new Referencias.Periodo(
                                                        periodo.nombre(),
                                                        periodo.inicio(),
                                                        periodo.fin()))));
    }
}
