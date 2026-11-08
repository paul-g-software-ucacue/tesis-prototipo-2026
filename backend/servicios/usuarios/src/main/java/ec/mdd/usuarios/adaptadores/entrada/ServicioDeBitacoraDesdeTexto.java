package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.ServicioDeBitacora;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * El parámetro {@code servicio} de la consulta de la bitácora con el valor del contrato ({@code
 * generacion}), no con el nombre del enumerado generado ({@code GENERACION}), que es lo que Spring
 * convierte por defecto (F17-H02). Un valor fuera de la lista responde {@code
 * COMUN_SOLICITUD_INVALIDA}, como cualquier parámetro que no se puede convertir.
 */
@Component
class ServicioDeBitacoraDesdeTexto implements Converter<String, ServicioDeBitacora> {

    @Override
    public ServicioDeBitacora convert(String valor) {
        return ServicioDeBitacora.fromValue(valor);
    }
}
