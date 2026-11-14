package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.Firmante;
import ec.mdd.firma.dominio.PasoDeFirma;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Contenidos sintéticos del documento firmable para las pruebas del generador (F13-H02). */
final class ContenidosDePrueba {

    static final String HASH = "0123456789abcdef".repeat(4);

    private ContenidosDePrueba() {}

    static List<Firmante> firmantes() {
        return Firmante.de(
                List.of(
                        new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                        new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                        new PasoDeFirma(3, "Aprobado", "RECTOR")),
                Map.of(
                        "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                        "VICERRECTOR", "LÓPEZ LUIS",
                        "RECTOR", "MORA EVA"));
    }

    /** Dos carreras y dos docentes, uno con un nombre que hay que escapar. */
    static ContenidoDelDocumento pequeno() {
        return new ContenidoDelDocumento(
                "2026-I",
                1,
                HASH,
                List.of(
                        new ContenidoDelDocumento.Carrera(
                                "DESARROLLO DE SOFTWARE", "ENVIADO", 2, 2, horas(8, 4)),
                        new ContenidoDelDocumento.Carrera(
                                "CONTABILIDAD", "ENVIADO", 1, 1, horas(4, 0))),
                horas(12, 4),
                List.of(
                        new ContenidoDelDocumento.Docente(
                                "0102030405",
                                "ZAMBRANO <LUIS> & \"HIJOS\"",
                                List.of(
                                        new ContenidoDelDocumento.Asignatura(
                                                "POO-204",
                                                "Programación orientada a objetos",
                                                "A",
                                                "DESARROLLO DE SOFTWARE",
                                                new BigDecimal("4"))),
                                List.of(
                                        new ContenidoDelDocumento.Actividad(
                                                "Tutorías", "OTRAS_DOCENCIA", new BigDecimal("4"))),
                                horas(4, 4)),
                        new ContenidoDelDocumento.Docente(
                                "0909090909",
                                "ARIAS ANA",
                                List.of(
                                        new ContenidoDelDocumento.Asignatura(
                                                "CON-101",
                                                "Contabilidad general",
                                                "A",
                                                "CONTABILIDAD",
                                                new BigDecimal("4"))),
                                List.of(),
                                horas(4, 0))),
                firmantes());
    }

    /** El tamaño del distributivo de la prueba: 165 paralelos en 7 carreras, 3 por docente. */
    static ContenidoDelDocumento completo() {
        List<ContenidoDelDocumento.Carrera> carreras = new ArrayList<>();
        for (int c = 1; c <= 7; c++) {
            carreras.add(
                    new ContenidoDelDocumento.Carrera(
                            "CARRERA DE PRUEBA NÚMERO " + c, "ENVIADO", 24, 24, horas(96, 12)));
        }
        List<ContenidoDelDocumento.Docente> docentes = new ArrayList<>();
        int paralelo = 0;
        for (int d = 1; paralelo < 165; d++) {
            List<ContenidoDelDocumento.Asignatura> asignaturas = new ArrayList<>();
            for (int a = 0; a < 3 && paralelo < 165; a++, paralelo++) {
                asignaturas.add(
                        new ContenidoDelDocumento.Asignatura(
                                "ASIG-" + paralelo,
                                "Asignatura de prueba con un nombre largo " + paralelo,
                                "A",
                                "CARRERA DE PRUEBA NÚMERO " + (paralelo % 7 + 1),
                                new BigDecimal("4")));
            }
            docentes.add(
                    new ContenidoDelDocumento.Docente(
                            String.format("01%08d", d),
                            String.format("DOCENTE %03d APELLIDO NOMBRE", d),
                            asignaturas,
                            List.of(
                                    new ContenidoDelDocumento.Actividad(
                                            "Tutorías académicas",
                                            "OTRAS_DOCENCIA",
                                            new BigDecimal("2"))),
                            horas(4 * asignaturas.size(), 2)));
        }
        return new ContenidoDelDocumento(
                "2026-I", 1, HASH, carreras, horas(660, 110), docentes, firmantes());
    }

    static ContenidoDelDocumento.Horas horas(int clase, int otras) {
        return new ContenidoDelDocumento.Horas(
                new BigDecimal(clase),
                new BigDecimal(otras),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
