package ec.mdd.docentes.dominio;

import static ec.mdd.docentes.dominio.DocentesDePrueba.ANIO;
import static ec.mdd.docentes.dominio.DocentesDePrueba.con;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Las reglas de los datos del docente (RF-16, research §3), sin dobles. */
class DocenteTest {

    @Test
    void dadosDatosValidos_cuandoSeRegistra_entoncesQuedaActivoConNombresEnMayusculas() {
        DatosDelDocente datos =
                con(
                        v -> {
                            v.primerApellido = "  peralta ";
                            v.segundoNombre = "";
                            v.correo = "Juan.Peralta@Instituto-Prueba.EDU.ec";
                            v.telefono = " ";
                            return v;
                        });

        Docente docente =
                Docente.nuevo(
                        new Identificacion(TipoDeIdentificacion.CEDULA, DocentesDePrueba.CEDULA),
                        datos,
                        ANIO);

        assertThat(docente.estado()).isEqualTo(EstadoLaboral.ACTIVO);
        assertThat(docente.datos().primerApellido()).isEqualTo("PERALTA");
        assertThat(docente.datos().segundoNombre()).isNull();
        assertThat(docente.datos().telefono()).isNull();
        assertThat(docente.datos().correo()).isEqualTo("juan.peralta@instituto-prueba.edu.ec");
        assertThat(docente.datos().nombreCompleto()).isEqualTo("PERALTA MALDONADO JUAN");
        assertThat(docente.datos().condicionEspecial()).isEqualTo(CondicionEspecial.NINGUNA);
    }

    @Test
    void dadosNombresInvalidos_cuandoSeGuardan_entoncesDatoInvalidoEnSuCampo() {
        invalido(v -> conPrimerApellido(v, " "), "primerApellido");
        invalido(v -> conPrimerApellido(v, "A".repeat(61)), "primerApellido");
        invalido(v -> conPrimerApellido(v, "PERALTA2"), "primerApellido");
        invalido(
                v -> {
                    v.primerNombre = null;
                    return v;
                },
                "primerNombre");
        invalido(
                v -> {
                    v.segundoApellido = "B".repeat(61);
                    return v;
                },
                "segundoApellido");
        assertThat(con(v -> conPrimerApellido(v, "o'neil-de la cruz")).primerApellido())
                .isEqualTo("O'NEIL-DE LA CRUZ");
    }

    @Test
    void dadosUnCorreoOUnTelefonoInvalidos_cuandoSeGuardan_entoncesDatoInvalidoSinRepetirElValor() {
        for (String correo : new String[] {"sinarroba.edu.ec", "a@b", "x".repeat(95) + "@a.edu"}) {
            assertThatThrownBy(
                            () ->
                                    con(
                                            v -> {
                                                v.correo = correo;
                                                return v;
                                            }))
                    .isInstanceOfSatisfying(
                            DatoInvalido.class,
                            error -> {
                                assertThat(error.campo()).isEqualTo("correoInstitucional");
                                assertThat(error.getMessage()).doesNotContain(correo);
                            });
        }
        for (String telefono : new String[] {"099abc0001", "099000", "09900000011"}) {
            assertThatThrownBy(
                            () ->
                                    con(
                                            v -> {
                                                v.telefono = telefono;
                                                return v;
                                            }))
                    .isInstanceOfSatisfying(
                            DatoInvalido.class,
                            error -> {
                                assertThat(error.campo()).isEqualTo("telefono");
                                assertThat(error.getMessage()).doesNotContain(telefono);
                            });
        }
    }

    @Test
    void dadaUnaCategoriaQueNoCorrespondeAlTipoDePersonal_cuandoSeGuarda_entoncesDatoInvalido() {
        invalido(
                v -> {
                    v.tipoPersonal = TipoPersonal.TITULAR;
                    v.categoria = CategoriaDeEscalafon.NO_APLICA;
                    return v;
                },
                "categoriaEscalafon");
        invalido(
                v -> {
                    v.categoria = CategoriaDeEscalafon.AGREGADO_1;
                    return v;
                },
                "categoriaEscalafon");
        assertThat(
                        con(v -> {
                                    v.tipoPersonal = TipoPersonal.TITULAR;
                                    v.categoria = CategoriaDeEscalafon.PRINCIPAL_2;
                                    v.fin = null;
                                    return v;
                                })
                                .categoria())
                .isEqualTo(CategoriaDeEscalafon.PRINCIPAL_2);
    }

    @Test
    void dadasHorasContratadasFueraDe1A40_cuandoSeGuardan_entoncesDatoInvalido() {
        for (int horas : new int[] {0, 41}) {
            invalido(
                    v -> {
                        v.horas = horas;
                        return v;
                    },
                    "horasContratadas");
        }
    }

    @Test
    void
            dadoTiempoParcialCon40Horas_cuandoSeGuarda_entoncesSeAceptaPorqueLaCoherenciaEsDeLaNormativa() {
        DatosDelDocente datos =
                con(
                        v -> {
                            v.dedicacion = Dedicacion.TP;
                            v.horas = 40;
                            return v;
                        });

        assertThat(datos.horasContratadas()).isEqualTo(40);
    }

    @Test
    void dadasFechasDeContratoInvalidas_cuandoSeGuardan_entoncesDatoInvalidoEnFinContrato() {
        invalido(
                v -> {
                    v.fin = null;
                    return v;
                },
                "finContrato");
        invalido(
                v -> {
                    v.fin = v.inicio;
                    return v;
                },
                "finContrato");
        invalido(
                v -> {
                    v.fin = LocalDate.of(2026, 3, 1);
                    return v;
                },
                "finContrato");
        invalido(
                v -> {
                    v.ingreso = null;
                    return v;
                },
                "fechaIngreso");
        invalido(
                v -> {
                    v.carrera = " ";
                    return v;
                },
                "carreraBase");
    }

    @Test
    void dadoUnDocente_cuandoSeCorrige_entoncesConservaIdentificacionYEstadoYDevuelveLosCambios() {
        Docente docente = DocentesDePrueba.docente().conEstado(EstadoLaboral.LICENCIA);
        DatosDelDocente nuevos =
                con(
                        v -> {
                            v.dedicacion = Dedicacion.MT;
                            v.horas = 20;
                            v.ficha =
                                    new FichaAcademica(
                                            v.ficha.tercerNivel(),
                                            v.ficha.cuartoNivel(),
                                            List.of(v.ficha.cursos().getFirst()),
                                            v.ficha.experiencia(),
                                            v.ficha.asignaturasAfines());
                            return v;
                        });

        Docente.Correccion correccion = docente.corregido(nuevos, ANIO);

        assertThat(correccion.docente().identificacion()).isEqualTo(docente.identificacion());
        assertThat(correccion.docente().estado()).isEqualTo(EstadoLaboral.LICENCIA);
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor("dedicacion", "TC", "MT"),
                        new CambioDeValor("horasContratadas", "40", "20"),
                        new CambioDeValor(
                                "ficha.cursos",
                                "PROGRAMACIÓN EN JAVA|061|40|2023; DOCENCIA UNIVERSITARIA|011|60|2021",
                                "PROGRAMACIÓN EN JAVA|061|40|2023"));
        assertThat(docente.corregido(docente.datos(), ANIO).cambios()).isEmpty();
    }

    @Test
    void dadoUnDocente_cuandoCambiaDeEstado_entoncesSoloCambiaElEstado() {
        Docente docente = DocentesDePrueba.docente();

        Docente inactivo = docente.conEstado(EstadoLaboral.INACTIVO);

        assertThat(inactivo.estado()).isEqualTo(EstadoLaboral.INACTIVO);
        assertThat(inactivo.datos()).isEqualTo(docente.datos());
        assertThat(inactivo.identificacion()).isEqualTo(docente.identificacion());
    }

    private static DocentesDePrueba.Valores conPrimerApellido(
            DocentesDePrueba.Valores v, String apellido) {
        v.primerApellido = apellido;
        return v;
    }

    private static void invalido(
            Function<DocentesDePrueba.Valores, DocentesDePrueba.Valores> cambio, String campo) {
        assertThatThrownBy(() -> con(cambio))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo(campo);
    }
}
