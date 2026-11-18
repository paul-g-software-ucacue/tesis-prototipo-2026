package ec.mdd.firma;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import ec.mdd.arquitectura.ReglasCapas;

/** Aplica al servicio las reglas de las cinco capas de plataforma:arquitectura (FR-004, FR-005). */
@AnalyzeClasses(
        packagesOf = ArquitecturaTest.class,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest static final ArchRule sentidoDeLasCapas = ReglasCapas.SENTIDO_DE_LAS_CAPAS;

    @ArchTest static final ArchRule dominioSinDetalles = ReglasCapas.DOMINIO_SIN_DETALLES;

    /**
     * PDFBox y Bouncy Castle solo en los adaptadores y en la configuración (F14-H02, D-103): el
     * dominio decide qué se acepta y en qué orden, sin depender de las bibliotecas de PDF ni de
     * criptografía.
     */
    @ArchTest
    static final ArchRule bibliotecasDeFirmaSoloEnAdaptadores =
            ArchRuleDefinition.noClasses()
                    .that()
                    .resideInAnyPackage("..dominio..", "..puertos..", "..aplicacion..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.bouncycastle..", "org.apache.pdfbox..");

    /**
     * Firma no conoce el flujo ni a Notificaciones (MD-05, D-110): avisa a Aprobación y pide
     * notificaciones solo con eventos, por sus puertos de salida.
     */
    @ArchTest
    static final ArchRule sinAprobacionNiNotificaciones =
            ArchRuleDefinition.noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("ec.mdd.validacionocs..", "ec.mdd.notificaciones..");
}
