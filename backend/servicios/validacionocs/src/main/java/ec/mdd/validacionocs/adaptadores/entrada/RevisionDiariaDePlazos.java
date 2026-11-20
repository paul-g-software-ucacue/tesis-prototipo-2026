package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.validacionocs.puertos.entrada.RevisarPlazos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Corre la revisión de plazos (F12-H03, research §3, D-114) cada día, a la hora del cron {@code
 * mdd.validacionocs.plazos.revision} en hora de Ecuador (por defecto, a las 07:00), y al arrancar
 * el proceso, para no perder un día en que estuvo detenido (FR-004). La programación ya está
 * habilitada en el proceso por {@code plataforma/eventos}. La revisión es idempotente: correr las
 * dos veces el mismo día no repite ningún aviso.
 */
public class RevisionDiariaDePlazos {

    private static final Logger LOG = LoggerFactory.getLogger(RevisionDiariaDePlazos.class);

    private final RevisarPlazos revisar;

    /** Recibe el caso de uso. */
    public RevisionDiariaDePlazos(RevisarPlazos revisar) {
        this.revisar = revisar;
    }

    /** La revisión de cada día. */
    @Scheduled(
            cron = "${mdd.validacionocs.plazos.revision:0 0 7 * * *}",
            zone = "America/Guayaquil")
    public void cadaDia() {
        revisarYContar("diaria");
    }

    /**
     * La revisión al arrancar, en un hilo propio: no retrasa el arranque ni su healthcheck aunque
     * otro proceso tarde en responder (sin identidad, el token de proceso espera su plazo).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        Thread.ofVirtual().name("revision-de-plazos").start(() -> revisarYContar("al arrancar"));
    }

    private void revisarYContar(String cuando) {
        try {
            int vencimientos = revisar.revisar();
            LOG.info("Revisión de plazos {}: {} vencimientos nuevos", cuando, vencimientos);
        } catch (RuntimeException e) {
            // Sin la base, por ejemplo: la próxima revisión lo intenta de nuevo.
            LOG.warn("La revisión de plazos {} no terminó: {}", cuando, e.getMessage());
        }
    }
}
