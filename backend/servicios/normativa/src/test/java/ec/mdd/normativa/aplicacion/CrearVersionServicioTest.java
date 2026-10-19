package ec.mdd.normativa.aplicacion;

import static ec.mdd.normativa.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.DIRECTA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.REFORMA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.politica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Crear una versión nueva a partir de otra (FR-001, FR-009), con Mockito en los puertos. */
class CrearVersionServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final CrearVersionServicio servicio =
            new CrearVersionServicio(repositorio, registro, DIRECTA, RELOJ);

    @Test
    void dadoLaVersionUno_cuandoSeCreaUnaNueva_entoncesSeNumeraSeGuardaYSeRegistra() {
        when(repositorio.siguienteNumero()).thenReturn(2);
        when(repositorio.buscar(1)).thenReturn(Optional.of(politica()));

        VersionNormativa creada = servicio.crear(1, REFORMA, QUIEN);

        assertThat(creada.numero()).isEqualTo(2);
        assertThat(creada.copiadaDe()).isEqualTo(1);
        assertThat(creada.tabla()).isEqualTo(politica().tabla());
        InOrder orden = inOrder(repositorio, registro);
        // El número se pide antes de comprobar el código: su candado ordena las creaciones.
        orden.verify(repositorio).siguienteNumero();
        orden.verify(repositorio).existeCodigo("SENESCYT-2025-01", 0);
        orden.verify(repositorio).guardar(creada, QUIEN, AHORA);
        orden.verify(registro)
                .registrar(
                        new CambioDeNormativa(
                                AHORA,
                                QUIEN,
                                Accion.VERSION_CREADA,
                                Map.of("numero", 2, "codigo", "SENESCYT-2025-01", "copiadaDe", 1)));
    }

    @Test
    void dadoUnaVersionDeOrigenQueNoExiste_cuandoSeCrea_entoncesVersionNoEncontrada() {
        when(repositorio.siguienteNumero()).thenReturn(2);
        when(repositorio.buscar(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(7, REFORMA, QUIEN))
                .isInstanceOf(VersionNoEncontrada.class);
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnCodigoQueYaUsaOtraVersion_cuandoSeCrea_entoncesCodigoRepetidoSinGuardar() {
        when(repositorio.siguienteNumero()).thenReturn(2);
        when(repositorio.buscar(1)).thenReturn(Optional.of(politica()));
        when(repositorio.existeCodigo(anyString(), anyInt())).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(1, REFORMA, QUIEN))
                .isInstanceOf(CodigoRepetido.class)
                .hasMessageContaining("SENESCYT-2025-01");
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }
}
