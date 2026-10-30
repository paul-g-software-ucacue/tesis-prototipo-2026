package ec.mdd.cargas;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import ec.mdd.arquitectura.ReglasCapas;

/**
 * Aplica al servicio las reglas de las cinco capas de plataforma:arquitectura (FR-004, FR-005) y la
 * de la fuente de datos intercambiable (RF-57, F01-H05).
 */
@AnalyzeClasses(
        packagesOf = ArquitecturaTest.class,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest static final ArchRule sentidoDeLasCapas = ReglasCapas.SENTIDO_DE_LAS_CAPAS;

    @ArchTest static final ArchRule dominioSinDetalles = ReglasCapas.DOMINIO_SIN_DETALLES;

    /**
     * Las reglas de las filas, la vista previa y el orden de las cargas no dependen del formato ni
     * del origen de los datos: solo ven la tabla que entrega el puerto {@code FuenteDeDatos}.
     */
    @ArchTest
    static final ArchRule fuenteDeDatosIntercambiable =
            noClasses()
                    .that()
                    .resideInAnyPackage("..cargas.dominio..", "..cargas.aplicacion..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.apache.poi..", "..cargas.adaptadores..")
                    .because(
                            "las cargas no dependen del formato ni del origen de los datos"
                                    + " (RF-57): otro adaptador de la fuente de datos no cambia"
                                    + " las reglas");
}
