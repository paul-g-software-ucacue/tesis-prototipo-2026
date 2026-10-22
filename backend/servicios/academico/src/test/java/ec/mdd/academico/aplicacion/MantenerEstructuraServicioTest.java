package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.SedeMatrizExistente;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadConDependientes;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.academico.puertos.entrada.UnidadRepetida;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Mantener la estructura orgánica (FR-002, FR-003, FR-007, FR-009): cada cambio se valida con toda
 * la estructura bloqueada, se guarda y se registra en la misma transacción. Con Mockito.
 */
class MantenerEstructuraServicioTest {

    private static final String QUIEN = "admin.demo";

    private static final UnidadOrganica MATRIZ =
            UnidadOrganica.nueva(
                    "MATRIZ", "Sede matriz", TipoDeUnidad.SEDE_MATRIZ, null, null, null);
    private static final UnidadOrganica RECT =
            UnidadOrganica.nueva("RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null);
    private static final UnidadOrganica COMISION =
            UnidadOrganica.nueva(
                    "COM-EVAL",
                    "Comisión de Evaluación",
                    TipoDeUnidad.COMISION,
                    "RECT",
                    null,
                    true);

    private final RepositorioEstructura repositorio = mock(RepositorioEstructura.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final MantenerEstructuraServicio servicio =
            new MantenerEstructuraServicio(repositorio, registro, transaccion, RELOJ);

    @BeforeEach
    void estructura() {
        when(repositorio.todas()).thenReturn(List.of(COMISION, MATRIZ, RECT));
        when(repositorio.catalogo())
                .thenReturn(
                        List.of(
                                new GestionAutorizada("G01", "Rector"),
                                new GestionAutorizada("G07", "Investigación")));
        when(repositorio.registrar(any(), anyString(), any())).thenReturn(true);
    }

    @Test
    void dadaUnaGestionValida_cuandoSeRegistra_entoncesBloqueaValidaGuardaYRegistra() {
        UnidadOrganica registrada =
                servicio.registrar(
                        "G-INV",
                        "Gestión de Investigación",
                        TipoDeUnidad.GESTION,
                        "RECT",
                        "G07",
                        null,
                        QUIEN);

        UnidadOrganica esperada =
                UnidadOrganica.nueva(
                        "G-INV",
                        "Gestión de Investigación",
                        TipoDeUnidad.GESTION,
                        "RECT",
                        "G07",
                        null);
        assertThat(registrada).isEqualTo(esperada);
        InOrder orden = inOrder(repositorio, registro);
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).todas();
        orden.verify(repositorio).registrar(esperada, QUIEN, AHORA);
        orden.verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.UNIDAD_REGISTRADA,
                                Map.of(
                                        "codigo", "G-INV",
                                        "nombre", "Gestión de Investigación",
                                        "tipo", "GESTION",
                                        "padre", "RECT",
                                        "gestionAutorizada", "G07")));
    }

    @Test
    void dadaUnaComision_cuandoSeRegistra_entoncesElRegistroLlevaSiEsPermanente() {
        servicio.registrar(
                "COM-INV",
                "Comisión de Investigación",
                TipoDeUnidad.COMISION,
                "RECT",
                null,
                false,
                QUIEN);

        verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.UNIDAD_REGISTRADA,
                                Map.of(
                                        "codigo", "COM-INV",
                                        "nombre", "Comisión de Investigación",
                                        "tipo", "COMISION",
                                        "padre", "RECT",
                                        "comisionPermanente", false)));
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entoncesUnidadRepetidaSinGuardarNiRegistrar() {
        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "RECT",
                                        "Otro",
                                        TipoDeUnidad.GOBIERNO,
                                        "MATRIZ",
                                        null,
                                        null,
                                        QUIEN))
                .isInstanceOf(UnidadRepetida.class);

        verify(repositorio, never()).registrar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoQueLaBaseRechazaElCodigo_cuandoSeRegistra_entoncesUnidadRepetidaSinRegistrar() {
        when(repositorio.registrar(any(), anyString(), any())).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "OCS",
                                        "Consejo",
                                        TipoDeUnidad.GOBIERNO,
                                        "MATRIZ",
                                        null,
                                        null,
                                        QUIEN))
                .isInstanceOf(UnidadRepetida.class);

        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaSegundaSedeMatriz_cuandoSeRegistra_entoncesSedeMatrizExistente() {
        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "MATRIZ2",
                                        "Otra",
                                        TipoDeUnidad.SEDE_MATRIZ,
                                        null,
                                        null,
                                        null,
                                        QUIEN))
                .isInstanceOf(SedeMatrizExistente.class);

        verify(repositorio, never()).registrar(any(), anyString(), any());
    }

    @Test
    void dadaUnaGestionFueraDelCatalogo_cuandoSeRegistra_entoncesDatoInvalidoSinGuardar() {
        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "G-X",
                                        "Otra",
                                        TipoDeUnidad.GESTION,
                                        "RECT",
                                        "G99",
                                        null,
                                        QUIEN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("gestionAutorizada");

        verify(repositorio, never()).registrar(any(), anyString(), any());
    }

    @Test
    void dadaUnaUnidad_cuandoSeCorrige_entoncesGuardaYRegistraCadaCambio() {
        UnidadOrganica corregida =
                servicio.corregir(
                        "COM-EVAL", "Comisión de Evaluación Interna", "MATRIZ", null, true, QUIEN);

        UnidadOrganica esperada =
                UnidadOrganica.nueva(
                        "COM-EVAL",
                        "Comisión de Evaluación Interna",
                        TipoDeUnidad.COMISION,
                        "MATRIZ",
                        null,
                        true);
        assertThat(corregida).isEqualTo(esperada);
        verify(repositorio).bloquear();
        verify(repositorio).guardar(esperada);
        verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.UNIDAD_CORREGIDA,
                                Map.of(
                                        "codigo",
                                        "COM-EVAL",
                                        "cambios",
                                        List.of(
                                                Map.of(
                                                        "campo", "nombre",
                                                        "anterior", "Comisión de Evaluación",
                                                        "nuevo", "Comisión de Evaluación Interna"),
                                                Map.of(
                                                        "campo", "padre",
                                                        "anterior", "RECT",
                                                        "nuevo", "MATRIZ")))));
    }

    @Test
    void dadosLosMismosDatos_cuandoSeCorrige_entoncesNoGuardaNiRegistra() {
        assertThat(servicio.corregir("RECT", "Rectorado", "MATRIZ", "G01", null, QUIEN))
                .isEqualTo(RECT);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaUnidadQueNoExiste_cuandoSeCorrigeOCambiaDeEstado_entoncesUnidadNoEncontrada() {
        assertThatThrownBy(() -> servicio.corregir("NADA", "Nada", "RECT", null, null, QUIEN))
                .isInstanceOf(UnidadNoEncontrada.class);
        assertThatThrownBy(() -> servicio.cambiarEstado("NADA", false, QUIEN))
                .isInstanceOf(UnidadNoEncontrada.class);
    }

    @Test
    void dadaUnaUnidadSinDependientes_cuandoSeInactiva_entoncesGuardaYRegistra() {
        UnidadOrganica inactiva = servicio.cambiarEstado("COM-EVAL", false, QUIEN);

        assertThat(inactiva).isEqualTo(COMISION.conEstado(false));
        verify(repositorio).bloquear();
        verify(repositorio).guardar(COMISION.conEstado(false));
        verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.UNIDAD_INACTIVADA,
                                Map.of("codigo", "COM-EVAL")));
    }

    @Test
    void dadaUnaUnidadInactiva_cuandoSeReactiva_entoncesGuardaYRegistra() {
        when(repositorio.todas()).thenReturn(List.of(COMISION.conEstado(false), MATRIZ, RECT));

        assertThat(servicio.cambiarEstado("COM-EVAL", true, QUIEN)).isEqualTo(COMISION);

        verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.UNIDAD_REACTIVADA,
                                Map.of("codigo", "COM-EVAL")));
    }

    @Test
    void dadoElMismoEstado_cuandoCambia_entoncesNoGuardaNiRegistra() {
        assertThat(servicio.cambiarEstado("RECT", true, QUIEN)).isEqualTo(RECT);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaUnidadConDependientesActivos_cuandoSeInactiva_entoncesUnidadConDependientes() {
        assertThatThrownBy(() -> servicio.cambiarEstado("RECT", false, QUIEN))
                .isInstanceOf(UnidadConDependientes.class);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }
}
