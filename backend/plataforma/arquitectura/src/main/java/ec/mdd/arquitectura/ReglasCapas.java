package ec.mdd.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas ArchUnit de las cinco capas de un servicio (principios I y V, ADR-009, ADR-012).
 *
 * <p>Cada servicio las aplica desde su {@code ArquitecturaTest}, que analiza solo las clases de
 * producción de su paquete raíz {@code ec.mdd.<servicio>}. Las capas se reconocen por el nombre de
 * su paquete: {@code dominio}, {@code puertos}, {@code aplicacion}, {@code adaptadores} e {@code
 * infraestructura}.
 *
 * <p>Ninguna regla falla por una capa ausente ni por un servicio que todavía no tiene clases: un
 * servicio recién creado o de soporte puede no tener todas las capas.
 */
public final class ReglasCapas {

    /**
     * Las dependencias apuntan hacia el dominio y nunca desde el dominio hacia los detalles.
     *
     * <p>El dominio lo pueden usar las otras cuatro capas. Los puertos, solo la aplicación, los
     * adaptadores y la infraestructura. La aplicación, solo los adaptadores y la infraestructura.
     * Los adaptadores, solo la infraestructura. Ninguna capa usa la infraestructura. Solo se
     * consideran las dependencias entre clases de las capas.
     */
    public static final ArchRule SENTIDO_DE_LAS_CAPAS =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .withOptionalLayers(true)
                    .layer("Dominio")
                    .definedBy("..dominio..")
                    .layer("Puertos")
                    .definedBy("..puertos..")
                    .layer("Aplicacion")
                    .definedBy("..aplicacion..")
                    .layer("Adaptadores")
                    .definedBy("..adaptadores..")
                    .layer("Infraestructura")
                    .definedBy("..infraestructura..")
                    .whereLayer("Dominio")
                    .mayOnlyBeAccessedByLayers(
                            "Puertos", "Aplicacion", "Adaptadores", "Infraestructura")
                    .whereLayer("Puertos")
                    .mayOnlyBeAccessedByLayers("Aplicacion", "Adaptadores", "Infraestructura")
                    .whereLayer("Aplicacion")
                    .mayOnlyBeAccessedByLayers("Adaptadores", "Infraestructura")
                    .whereLayer("Adaptadores")
                    .mayOnlyBeAccessedByLayers("Infraestructura")
                    .whereLayer("Infraestructura")
                    .mayNotBeAccessedByAnyLayer()
                    .because("las dependencias del código apuntan hacia el dominio (principio I)")
                    .allowEmptyShould(true);

    /**
     * El dominio no depende del framework web, del ORM, de HTTP ni de librerías de PDF.
     *
     * <p>Así el dominio sigue siendo Java puro y se prueba sin infraestructura (principios I y
     * VIII). La lista de paquetes es la de la constitución; agregar uno es un cambio de la regla,
     * no de cada servicio.
     */
    public static final ArchRule DOMINIO_SIN_DETALLES =
            noClasses()
                    .that()
                    .resideInAPackage("..dominio..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            // Spring
                            "org.springframework..",
                            // JPA y ORM
                            "jakarta.persistence..",
                            "org.hibernate..",
                            // HTTP
                            "jakarta.servlet..",
                            "java.net.http..",
                            "org.apache.hc..",
                            "okhttp3..",
                            // PDF
                            "org.apache.pdfbox..",
                            "com.openhtmltopdf..")
                    .because(
                            "el dominio no conoce el framework, la persistencia, HTTP ni PDF (principio I)")
                    .allowEmptyShould(true);

    private ReglasCapas() {}
}
