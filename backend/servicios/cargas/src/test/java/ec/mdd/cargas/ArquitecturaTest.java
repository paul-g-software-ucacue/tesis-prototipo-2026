package ec.mdd.cargas;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import ec.mdd.arquitectura.ReglasCapas;

/** Aplica al servicio las reglas de las cinco capas de plataforma:arquitectura (FR-004, FR-005). */
@AnalyzeClasses(
        packagesOf = ArquitecturaTest.class,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest static final ArchRule sentidoDeLasCapas = ReglasCapas.SENTIDO_DE_LAS_CAPAS;

    @ArchTest static final ArchRule dominioSinDetalles = ReglasCapas.DOMINIO_SIN_DETALLES;
}
