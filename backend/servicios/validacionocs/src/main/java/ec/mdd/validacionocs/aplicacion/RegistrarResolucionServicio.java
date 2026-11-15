package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ArchivoDeResolucion;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteResolucion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.ResolucionOcs;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.RegistrarResolucion;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.AlmacenDeResoluciones;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Registra la resolución del OCS y aprueba el distributivo (F15-H01, RF-37, D-107, research §1).
 *
 * <p>En este orden:
 *
 * <ol>
 *   <li>el periodo abierto y el distributivo, como aprobar y avanzar;
 *   <li>el PDF y el número, y el agregado sin bloquear ni cambiar nada (la etapa, el rol y la
 *       fecha): un rechazo no deja ni el archivo;
 *   <li>el PDF en el almacén, por su huella, fuera de la transacción: la escritura es idempotente,
 *       y un archivo cuyo registro falla queda sin referencia;
 *   <li>con el proceso bloqueado, el agregado vuelve a comprobar y registra el paso a APROBADO con
 *       la resolución, y su cambio para la bitácora, en la misma transacción. Dos registros a la
 *       vez: el segundo espera el bloqueo y encuentra el distributivo en APROBADO.
 * </ol>
 *
 * <p>Las firmas del PDF no se abren ni se validan (D-19).
 */
public final class RegistrarResolucionServicio implements RegistrarResolucion {

    private final RepositorioProceso procesos;
    private final Distributivo distributivo;
    private final DatosDelPeriodo datos;
    private final AlmacenDeResoluciones almacen;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final ConsultarFlujo flujo;
    private final Clock reloj;

    /**
     * @param procesos los procesos de aprobación
     * @param distributivo Generación, para saber si el periodo tiene distributivo
     * @param datos el estado del periodo, de Académico
     * @param almacen los PDF de las resoluciones
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción del registro
     * @param flujo el estado del flujo que se responde
     * @param reloj el momento del registro
     */
    public RegistrarResolucionServicio(
            RepositorioProceso procesos,
            Distributivo distributivo,
            DatosDelPeriodo datos,
            AlmacenDeResoluciones almacen,
            RegistroDeCambios registro,
            Transaccion transaccion,
            ConsultarFlujo flujo,
            Clock reloj) {
        this.procesos = procesos;
        this.distributivo = distributivo;
        this.datos = datos;
        this.almacen = almacen;
        this.registro = registro;
        this.transaccion = transaccion;
        this.flujo = flujo;
        this.reloj = reloj;
    }

    @Override
    public FlujoDelDistributivo registrar(
            String periodo,
            String numero,
            LocalDate fecha,
            String nombre,
            byte[] archivo,
            Solicitante quien) {
        if (datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new).cerrado()) {
            throw new PeriodoCerrado();
        }
        distributivo.actual(periodo).orElseThrow(SinDistributivo::new);
        ArchivoDeResolucion pdf = ArchivoDeResolucion.de(nombre, archivo);
        ResolucionOcs resolucion =
                new ResolucionOcs(numero, fecha, pdf.sha256(), pdf.nombre(), pdf.tamano());
        Instant ahora = reloj.instant();
        // Sin proceso todavía, el distributivo no llegó a EN_OCS.
        procesos.de(periodo)
                .orElseThrow(EtapaNoAdmiteResolucion::new)
                .validarResolucion(quien, resolucion, ahora);
        almacen.guardar(pdf.sha256(), pdf.contenido());
        transaccion.ejecutar(
                () -> {
                    ProcesoAprobacion bloqueado =
                            procesos.bloquear(periodo).orElseThrow(EtapaNoAdmiteResolucion::new);
                    List<Transicion> nuevas =
                            bloqueado.registrarResolucion(quien, resolucion, ahora);
                    procesos.guardar(bloqueado, nuevas);
                    nuevas.forEach(
                            una ->
                                    registro.registrar(
                                            FlujoDelDistributivoServicio.cambio(
                                                    periodo, una, Optional.empty())));
                    return null;
                });
        return flujo.flujo(periodo, quien);
    }
}
