package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.dominio.EstructuraOrganica;
import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.ConsultarEstructura;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.academico.puertos.entrada.UnidadRepetida;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Doble de los casos de uso de la estructura escrito a mano (principio VIII): las unidades en
 * memoria, cambiadas con las reglas reales del dominio. Empieza con la sede matriz {@code MATRIZ},
 * el Rectorado {@code RECT} (G01), la gestión {@code G-CAL} (G10) y la comisión permanente {@code
 * COM-EVAL} debajo de ella. El catálogo tiene las 21 gestiones.
 */
final class EstructuraEnMemoria implements ConsultarEstructura, MantenerEstructura {

    static final List<GestionAutorizada> CATALOGO =
            IntStream.rangeClosed(1, 21)
                    .mapToObj(numero -> String.format("G%02d", numero))
                    .map(codigo -> new GestionAutorizada(codigo, "Gestión " + codigo))
                    .toList();

    private EstructuraOrganica estructura;
    private String ultimoUsuario;

    EstructuraEnMemoria() {
        reiniciar();
    }

    /** Vuelve a las cuatro unidades de inicio, para que cada prueba empiece igual. */
    void reiniciar() {
        estructura =
                new EstructuraOrganica(
                        List.of(
                                UnidadOrganica.nueva(
                                        "MATRIZ",
                                        "Sede matriz",
                                        TipoDeUnidad.SEDE_MATRIZ,
                                        null,
                                        null,
                                        null),
                                UnidadOrganica.nueva(
                                        "RECT",
                                        "Rectorado",
                                        TipoDeUnidad.GOBIERNO,
                                        "MATRIZ",
                                        "G01",
                                        null),
                                UnidadOrganica.nueva(
                                        "G-CAL",
                                        "Coordinación de Aseguramiento de la Calidad",
                                        TipoDeUnidad.GESTION,
                                        "RECT",
                                        "G10",
                                        null),
                                UnidadOrganica.nueva(
                                        "COM-EVAL",
                                        "Comisión de Evaluación Interna",
                                        TipoDeUnidad.COMISION,
                                        "G-CAL",
                                        null,
                                        true)),
                        CATALOGO.stream().map(GestionAutorizada::codigo).toList());
        ultimoUsuario = null;
    }

    /** El usuario del último cambio. */
    String ultimoUsuario() {
        return ultimoUsuario;
    }

    @Override
    public List<UnidadOrganica> unidades() {
        return estructura.unidades().stream()
                .sorted(Comparator.comparing(UnidadOrganica::codigo))
                .toList();
    }

    @Override
    public UnidadOrganica unidad(String codigo) {
        return estructura.unidad(codigo).orElseThrow(() -> new UnidadNoEncontrada(codigo));
    }

    @Override
    public List<GestionAutorizada> gestionesAutorizadas() {
        return CATALOGO;
    }

    @Override
    public UnidadOrganica registrar(
            String codigo,
            String nombre,
            TipoDeUnidad tipo,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien) {
        UnidadOrganica nueva =
                UnidadOrganica.nueva(
                        codigo, nombre, tipo, padre, gestionAutorizada, comisionPermanente);
        if (estructura.unidad(codigo).isPresent()) {
            throw new UnidadRepetida(codigo);
        }
        estructura = estructura.con(estructura.registrar(nueva));
        ultimoUsuario = quien;
        return nueva;
    }

    @Override
    public UnidadOrganica corregir(
            String codigo,
            String nombre,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien) {
        UnidadOrganica corregida =
                estructura
                        .corregir(
                                unidad(codigo),
                                nombre,
                                padre,
                                gestionAutorizada,
                                comisionPermanente)
                        .unidad();
        estructura = estructura.con(corregida);
        ultimoUsuario = quien;
        return corregida;
    }

    @Override
    public UnidadOrganica cambiarEstado(String codigo, boolean activa, String quien) {
        UnidadOrganica nueva = estructura.cambiarEstado(unidad(codigo), activa);
        estructura = estructura.con(nueva);
        ultimoUsuario = quien;
        return nueva;
    }
}
