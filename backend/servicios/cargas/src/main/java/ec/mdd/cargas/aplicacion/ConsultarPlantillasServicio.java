package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;

/**
 * Consultar las plantillas de carga. El diccionario se lee una vez, al construir el servicio: no
 * cambia mientras el proceso corre, y una definición incompleta detiene el arranque.
 */
public final class ConsultarPlantillasServicio implements ConsultarPlantillas {

    private final DiccionarioDeDatos diccionario;

    /** Lee el diccionario de la fuente. */
    public ConsultarPlantillasServicio(FuenteDelDiccionario fuente) {
        this.diccionario = fuente.leer();
    }

    @Override
    public DiccionarioDeDatos diccionario() {
        return diccionario;
    }

    @Override
    public Carga carga(String codigo) {
        return diccionario.carga(codigo).orElseThrow(() -> new CargaNoEncontrada(codigo));
    }
}
