package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@ExtendWith(OutputCaptureExtension.class)
class FallosAsincronosTest {

    private final FallosAsincronos fallos = new FallosAsincronos();

    @Test
    void dadoUnaEntregaFallida_cuandoLlegaAlManejador_entoncesNoRepiteSuTrazaEnError(
            CapturedOutput salida) throws Exception {
        fallos.handleUncaughtException(new EntregaFallida("el receptor respondió 503"), metodo());

        assertThat(salida.getAll()).doesNotContain("ERROR").doesNotContain("EntregaFallida");
    }

    @Test
    void dadoOtraExcepcion_cuandoLlegaAlManejador_entoncesSeRegistraEnErrorConSuTraza(
            CapturedOutput salida) throws Exception {
        fallos.handleUncaughtException(new IllegalStateException("otro fallo"), metodo());

        assertThat(salida.getAll()).contains("ERROR").contains("IllegalStateException");
    }

    @Test
    void dadoLaAutoconfiguracion_cuandoDaElManejoAsincrono_entoncesUsaEsteManejadorYElEjecutor() {
        DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
        AsyncConfigurer configuracion =
                new EventosAutoConfiguracion().configuracionAsincronaDeEventos(fabrica);
        assertThat(configuracion.getAsyncUncaughtExceptionHandler())
                .isInstanceOf(FallosAsincronos.class);
        assertThat(configuracion.getAsyncExecutor()).isNull();

        Executor ejecutor = Runnable::run;
        fabrica.registerSingleton(EventosAutoConfiguracion.EJECUTOR_DE_SPRING_BOOT, ejecutor);

        assertThat(configuracion.getAsyncExecutor()).isSameAs(ejecutor);
    }

    private static Method metodo() throws NoSuchMethodException {
        return OyenteEntreProcesos.class.getMethod("entregar", EventoEntreProcesos.class);
    }
}
