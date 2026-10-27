package ec.mdd.docentes.aplicacion.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import ec.mdd.plataforma.cargas.CambioDeCampo;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Los traductores de las cargas 06, 08 y 09 (F01-H04, FR-012), con Mockito: los 47 docentes, los 21
 * cargos y los 11 proyectos del archivo de prueba son iguales a los que construyen las pruebas de
 * Docentes desde el mismo archivo, así que la traducción es fiel.
 */
class TraductoresDocentesTest {

    private static final String HASH = "a".repeat(64);

    private final RepositorioDocentes docentes = mock(RepositorioDocentes.class);
    private final MantenerDocentes mantenerDocentes = mock(MantenerDocentes.class);
    private final RepositorioCargos cargos = mock(RepositorioCargos.class);
    private final RepositorioProyectos proyectos = mock(RepositorioProyectos.class);
    private final TraductorDocentesContratados traductorDocentes =
            new TraductorDocentesContratados(docentes, mantenerDocentes);

    static List<FilaDelLote> filas(String archivo) {
        try (InputStream entrada =
                        TraductoresDocentesTest.class.getResourceAsStream(
                                "/docentes/datos-de-prueba/" + archivo);
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().toList();
            String[] encabezados = lineas.getFirst().replace("﻿", "").split(",", -1);
            List<FilaDelLote> filas = new ArrayList<>();
            for (int i = 1; i < lineas.size(); i++) {
                if (lineas.get(i).isBlank()) {
                    continue;
                }
                String[] valores = lineas.get(i).split(",", -1);
                Map<String, String> fila = new HashMap<>();
                for (int c = 0; c < encabezados.length; c++) {
                    fila.put(encabezados[c].strip(), valores[c].strip());
                }
                filas.add(new FilaDelLote(i + 1, fila));
            }
            return filas;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static LoteDeCarga lote(String carga, List<FilaDelLote> filas) {
        return new LoteDeCarga(carga, HASH, filas);
    }

    @Test
    void los47DocentesDelArchivo_sonIgualesALosRegistrados() {
        when(docentes.todos()).thenReturn(DocentesDePrueba.cargados());

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        traductorDocentes, lote("docentes", filas("06_docentes.csv")));

        assertThat(vista.registros())
                .as(
                        () ->
                                vista.registros().stream()
                                        .filter(r -> r.tipo() != TipoDeRegistro.IGUAL)
                                        .toList()
                                        .toString())
                .allMatch(registro -> registro.tipo() == TipoDeRegistro.IGUAL)
                .hasSize(47);
    }

    @Test
    void unCambioDeDedicacionCuenta_yElEstadoSeAplicaConCambiarEstado() {
        when(docentes.todos()).thenReturn(DocentesDePrueba.cargados());
        FilaDelLote primera = filas("06_docentes.csv").getFirst();
        Map<String, String> valores = new HashMap<>(primera.valores());
        valores.put("estado", "LICENCIA");
        FilaDelLote enLicencia = new FilaDelLote(primera.fila(), valores);

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        traductorDocentes, lote("docentes", List.of(enLicencia)));

        assertThat(vista.registros())
                .filteredOn(registro -> registro.tipo() == TipoDeRegistro.CAMBIA)
                .singleElement()
                .satisfies(
                        registro ->
                                assertThat(registro.cambios())
                                        .containsExactly(
                                                new CambioDeCampo("estado", "ACTIVO", "LICENCIA")));
        traductorDocentes.corregir(
                enLicencia, DocentesDePrueba.cargados().getFirst(), "datos.demo");
        verify(mantenerDocentes, never()).corregir(anyString(), any(), anyString());
        verify(mantenerDocentes)
                .cambiarEstado(
                        primera.valor("numero_identificacion"),
                        EstadoLaboral.LICENCIA,
                        "datos.demo");
    }

    @Test
    void elTipoDeIdentificacionNoCambia() {
        FilaDelLote primera = filas("06_docentes.csv").getFirst();
        Map<String, String> valores = new HashMap<>(primera.valores());
        valores.put("tipo_identificacion", "PASAPORTE");

        assertThatThrownBy(
                        () ->
                                traductorDocentes.corregir(
                                        new FilaDelLote(2, valores),
                                        DocentesDePrueba.cargados().getFirst(),
                                        "datos.demo"))
                .isInstanceOfSatisfying(
                        DatoInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("tipo_identificacion"));
        assertThat(
                        traductorDocentes.rechazo(
                                new FilaDelLote(2, valores), new DatoInvalido("correo", "x")))
                .hasValueSatisfying(rechazo -> assertThat(rechazo.campo()).contains("correo"));
    }

    @Test
    void los21CargosY11ProyectosDelArchivo_sonIgualesALosRegistrados() {
        when(cargos.todos()).thenReturn(CargasDePrueba.cargos());
        when(proyectos.todos()).thenReturn(CargasDePrueba.proyectos());
        TraductorCargos traductorCargos = new TraductorCargos(cargos, mock(MantenerCargos.class));
        TraductorProyectos traductorProyectos =
                new TraductorProyectos(proyectos, mock(MantenerProyectos.class));

        VistaPreviaDelLote deCargos =
                MotorDelLote.previsualizar(
                        traductorCargos,
                        lote("cargos_autoridades", filas("08_cargos_autoridades.csv")));
        VistaPreviaDelLote deProyectos =
                MotorDelLote.previsualizar(
                        traductorProyectos,
                        lote("proyectos_practicas", filas("09_proyectos_practicas.csv")));

        assertThat(deCargos.cuantos(TipoDeRegistro.IGUAL)).isEqualTo(21);
        assertThat(deCargos.registros()).hasSize(21);
        assertThat(deProyectos.cuantos(TipoDeRegistro.IGUAL)).isEqualTo(11);
        assertThat(deProyectos.registros()).hasSize(11);
        assertThat(deCargos.registros().getFirst().clave())
                .matches("\\d{10} · [A-Z0-9_]+ · \\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void unCargoNuevoSeRegistraConSuCasoDeUso() {
        when(cargos.todos()).thenReturn(List.of());
        MantenerCargos mantener = mock(MantenerCargos.class);
        TraductorCargos traductorCargos = new TraductorCargos(cargos, mantener);
        FilaDelLote primera = filas("08_cargos_autoridades.csv").getFirst();

        traductorCargos.registrar(primera, "datos.demo");

        verify(mantener).registrar(eq(CargasDePrueba.cargos().getFirst()), eq("datos.demo"));
    }
}
