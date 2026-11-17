package ec.mdd.validacionocs;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import ec.mdd.arquitectura.ReglasCapas;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;

/** Aplica al servicio las reglas de las cinco capas de plataforma:arquitectura (FR-004, FR-005). */
@AnalyzeClasses(
        packagesOf = ArquitecturaTest.class,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest static final ArchRule sentidoDeLasCapas = ReglasCapas.SENTIDO_DE_LAS_CAPAS;

    @ArchTest static final ArchRule dominioSinDetalles = ReglasCapas.DOMINIO_SIN_DETALLES;

    /**
     * Quien guarda transiciones también las notifica (F16-H01, D-108): así una operación nueva no
     * se olvida de avisar a quien le toca.
     */
    @ArchTest
    static final ArchRule quienGuardaTransicionesNotifica =
            classes()
                    .that()
                    .resideInAPackage("..aplicacion..")
                    .should(
                            new ArchCondition<JavaClass>(
                                    "llamar a NotificacionDeTransiciones.notificar si llama a"
                                            + " RepositorioProceso.guardar") {
                                @Override
                                public void check(JavaClass clase, ConditionEvents eventos) {
                                    boolean guarda =
                                            llama(clase, RepositorioProceso.class, "guardar");
                                    boolean notifica =
                                            llama(
                                                    clase,
                                                    NotificacionDeTransiciones.class,
                                                    "notificar");
                                    if (guarda && !notifica) {
                                        eventos.add(
                                                SimpleConditionEvent.violated(
                                                        clase,
                                                        clase.getName()
                                                                + " guarda transiciones sin"
                                                                + " notificarlas"));
                                    }
                                }
                            });

    private static boolean llama(JavaClass clase, Class<?> destino, String metodo) {
        return clase.getMethodCallsFromSelf().stream()
                .anyMatch(
                        llamada ->
                                llamada.getTargetOwner().isEquivalentTo(destino)
                                        && llamada.getName().equals(metodo));
    }
}
