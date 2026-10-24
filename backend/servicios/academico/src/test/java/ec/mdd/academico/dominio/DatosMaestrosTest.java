package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.CarreraTest.invalido;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.asignatura;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.carrera;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.vacios;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Las reglas entre carreras, mallas, asignaturas y la estructura orgánica (research §4), sin
 * dobles, con las 7 carreras, 12 mallas y 300 asignaturas de los datos de prueba.
 */
class DatosMaestrosTest {

    @Test
    void dadaUnaSedeInexistenteInactivaODeOtroTipo_cuandoSeRegistraLaCarrera_entoncesEnSede() {
        for (String sede : List.of("OTRA", "NORTE", "RECT")) {
            Carrera conSede = carrera("C1", "DS");
            Carrera otra =
                    new Carrera(
                            conSede.codigo(),
                            conSede.nombre(),
                            conSede.siglas(),
                            sede,
                            conSede.nivelFormacion(),
                            conSede.modalidad(),
                            conSede.jornadas(),
                            conSede.campo(),
                            conSede.campoDetallado(),
                            conSede.numeroCiclos(),
                            conSede.estado());
            invalido(() -> vacios().registrarCarrera(otra), "sede");
        }
        assertThat(vacios().registrarCarrera(carrera("C1", "DS")).sede()).isEqualTo("MATRIZ");
    }

    @Test
    void dadoUnCampoAmplioQueNoEsDeLaCine_cuandoSeRegistra_entoncesEnCampoAmplio() {
        Carrera base = carrera("C1", "DS");
        Carrera rara =
                new Carrera(
                        base.codigo(),
                        base.nombre(),
                        base.siglas(),
                        base.sede(),
                        base.nivelFormacion(),
                        base.modalidad(),
                        base.jornadas(),
                        new CampoConocimiento("11", "111"),
                        null,
                        base.numeroCiclos(),
                        base.estado());
        invalido(() -> vacios().registrarCarrera(rara), "campoAmplio");
    }

    @Test
    void dadaUnaCarreraNoVigente_cuandoSeRegistraSuMallaOUnaAsignatura_entoncesEnCarrera() {
        DatosMaestros datos =
                vacios().con(carrera("C1", "DS").conEstado(EstadoDeCarrera.EN_CIERRE))
                        .con(Malla.nueva("M1", "C1"));

        invalido(() -> datos.registrarMalla(Malla.nueva("M2", "C1")), "carrera");
        invalido(() -> datos.registrarAsignatura(asignatura("A1", "C1", "M1", 1)), "carrera");
        invalido(() -> vacios().registrarAsignatura(asignatura("A1", "C9", "M1", 1)), "carrera");
    }

    @Test
    void dadaUnaMallaDerogadaODeOtraCarrera_cuandoSeRegistraUnaAsignatura_entoncesEnMalla() {
        DatosMaestros datos =
                vacios().con(carrera("C1", "DS"))
                        .con(carrera("C2", "CO"))
                        .con(Malla.nueva("M1", "C1").conEstado(EstadoDeMalla.DEROGADA))
                        .con(Malla.nueva("M2", "C2"));

        invalido(() -> datos.registrarAsignatura(asignatura("A1", "C1", "M1", 1)), "malla");
        invalido(() -> datos.registrarAsignatura(asignatura("A1", "C1", "M2", 1)), "malla");
        invalido(() -> datos.registrarAsignatura(asignatura("A1", "C1", "M9", 1)), "malla");
    }

    @Test
    void dadoUnCicloMayorQueLosDeSuCarrera_cuandoSeRegistraOCorrige_entoncesEnCiclo() {
        DatosMaestros datos = vacios().con(carrera("C1", "DS")).con(Malla.nueva("M1", "C1"));

        invalido(() -> datos.registrarAsignatura(asignatura("A1", "C1", "M1", 6)), "ciclo");
        Asignatura enCiclo5 = datos.registrarAsignatura(asignatura("A1", "C1", "M1", 5));
        invalido(
                () ->
                        datos.con(enCiclo5)
                                .corregirAsignatura(
                                        enCiclo5,
                                        "N",
                                        7,
                                        UnidadDeOrganizacionCurricular.BASICA,
                                        BigDecimal.ONE,
                                        null,
                                        null,
                                        false,
                                        new CampoConocimiento("05", "054"),
                                        List.of("a", "b")),
                "ciclo");
    }

    @Test
    void
            dadaUnaAsignaturaActivaEnElCiclo5_cuandoSeBajanLosCiclosDeSuCarrera_entoncesEnNumeroCiclos() {
        Carrera c1 = carrera("C1", "DS");
        DatosMaestros datos =
                vacios().con(c1).con(Malla.nueva("M1", "C1")).con(asignatura("A1", "C1", "M1", 5));

        invalido(
                () ->
                        datos.corregirCarrera(
                                c1,
                                c1.nombre(),
                                c1.siglas(),
                                c1.sede(),
                                c1.nivelFormacion(),
                                c1.modalidad(),
                                c1.jornadas(),
                                c1.campo(),
                                c1.campoDetallado(),
                                4),
                "numeroCiclos");
        DatosMaestros conInactiva =
                vacios().con(c1)
                        .con(Malla.nueva("M1", "C1"))
                        .con(asignatura("A1", "C1", "M1", 5).conEstado(false));
        assertThat(
                        conInactiva
                                .corregirCarrera(
                                        c1,
                                        c1.nombre(),
                                        c1.siglas(),
                                        c1.sede(),
                                        c1.nivelFormacion(),
                                        c1.modalidad(),
                                        c1.jornadas(),
                                        c1.campo(),
                                        c1.campoDetallado(),
                                        4)
                                .carrera()
                                .numeroCiclos())
                .isEqualTo(4);
    }

    @Test
    void dadaUnaAsignaturaDeUnaMallaDerogada_cuandoSeReactiva_entoncesEnMalla() {
        Asignatura inactiva = asignatura("A1", "C1", "M1", 1).conEstado(false);
        DatosMaestros datos =
                vacios().con(carrera("C1", "DS"))
                        .con(Malla.nueva("M1", "C1").conEstado(EstadoDeMalla.DEROGADA))
                        .con(inactiva);

        invalido(() -> datos.cambiarEstadoAsignatura(inactiva, true), "malla");
        assertThat(datos.cambiarEstadoAsignatura(inactiva, false).activa()).isFalse();
    }

    @Test
    void dadosLosDatosDePrueba_cuandoSeRegistranEnOrden_entoncesEntranTodosYCadaMallaSuma100() {
        DatosMaestros datos = vacios();
        for (Carrera carrera : DatosMaestrosDePrueba.carreras()) {
            datos = datos.con(datos.registrarCarrera(carrera));
        }
        List<Asignatura> asignaturas = DatosMaestrosDePrueba.asignaturas();
        for (String malla : asignaturas.stream().map(Asignatura::malla).distinct().toList()) {
            String carrera =
                    asignaturas.stream()
                            .filter(a -> a.malla().equals(malla))
                            .findFirst()
                            .orElseThrow()
                            .carrera();
            datos = datos.con(datos.registrarMalla(Malla.nueva(malla, carrera)));
        }
        for (Asignatura asignatura : asignaturas) {
            datos = datos.con(datos.registrarAsignatura(asignatura));
        }

        DatosMaestros registrados = datos;
        assertThat(registrados.carreras()).hasSize(7);
        assertThat(registrados.mallas()).hasSize(12);
        assertThat(registrados.asignaturas()).hasSize(300);
        Map<String, BigDecimal> horas =
                registrados.mallas().stream()
                        .collect(
                                Collectors.toMap(
                                        Malla::codigo,
                                        malla -> registrados.horasDe(malla.codigo())));
        assertThat(horas.values())
                .allSatisfy(total -> assertThat(total).isEqualByComparingTo("100"));
    }
}
