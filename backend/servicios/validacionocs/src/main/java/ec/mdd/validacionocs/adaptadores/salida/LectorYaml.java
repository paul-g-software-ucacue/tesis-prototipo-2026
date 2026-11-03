package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.Borrador;
import ec.mdd.validacionocs.dominio.Borrador.EtapaEscrita;
import ec.mdd.validacionocs.dominio.Borrador.FirmaEscrita;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Lee el archivo en dos pasos, y ninguno construye objetos (FR-009). Primero recorre sus eventos y
 * rechaza anclas, alias y etiquetas: un archivo de configuración no los necesita, y así no hay
 * bomba de alias posible. Después arma solo el árbol de nodos y toma cada valor como el texto que
 * se escribió, para que la validación diga qué está mal.
 */
public final class LectorYaml implements LectorDeConfiguracion {

    private static final String ETAPAS = "etapas";
    private static final String FIRMAS = "firmas";
    private static final String ACTIVA = "activa";
    private static final String NOMBRE = "nombre";
    private static final String ROL = "rol";
    private static final String PLAZO = "plazo_dias";
    private static final String DEVOLUCION = "admite_devolucion";
    private static final String ORDEN = "orden";
    private static final String ETIQUETA = "etiqueta";
    private static final String ETAPA = "etapa";
    private static final String DOCUMENTO = "documento";

    private static final Set<String> CLAVES_RAIZ = Set.of(ETAPAS, FIRMAS);
    private static final Set<String> CLAVES_ETAPA = Set.of(ACTIVA, NOMBRE, ROL, PLAZO, DEVOLUCION);
    private static final Set<String> CLAVES_FIRMA = Set.of(ORDEN, ETIQUETA, ROL, ETAPA, DOCUMENTO);

    private static final int PROFUNDIDAD_MAXIMA = 8;
    private static final int CLAVE_MAXIMA = 40;

    private static LoaderOptions opciones() {
        LoaderOptions opciones = new LoaderOptions();
        opciones.setAllowDuplicateKeys(false);
        opciones.setAllowRecursiveKeys(false);
        opciones.setMaxAliasesForCollections(0);
        opciones.setNestingDepthLimit(PROFUNDIDAD_MAXIMA);
        opciones.setCodePointLimit(ConfiguracionFlujo.TAMANO_MAXIMO);
        return opciones;
    }

    @Override
    public Borrador leer(String texto) {
        Yaml yaml = new Yaml(new SafeConstructor(opciones()));
        try {
            rechazarLoQueNoHaceFalta(yaml, texto);
            if (yaml.compose(new StringReader(texto)) instanceof MappingNode raiz) {
                return new Lectura().de(raiz);
            }
            throw new ArchivoIlegible();
        } catch (MarkedYAMLException error) {
            throw new ArchivoIlegible(linea(error.getProblemMark()));
        } catch (YAMLException error) {
            throw new ArchivoIlegible();
        }
    }

    private static void rechazarLoQueNoHaceFalta(Yaml yaml, String texto) {
        for (Event evento : yaml.parse(new StringReader(texto))) {
            boolean conAncla = evento instanceof NodeEvent nodo && nodo.getAnchor() != null;
            boolean conEtiqueta =
                    evento instanceof ScalarEvent valor && valor.getTag() != null
                            || evento instanceof CollectionStartEvent grupo
                                    && grupo.getTag() != null;
            if (conAncla || conEtiqueta) {
                throw new ArchivoIlegible(linea(evento.getStartMark()));
            }
        }
    }

    private static int linea(Mark marca) {
        return marca == null ? 0 : marca.getLine() + 1;
    }

    private static int linea(Node nodo) {
        return linea(nodo.getStartMark());
    }

    private static String corta(String texto) {
        return texto.length() <= CLAVE_MAXIMA ? texto : texto.substring(0, CLAVE_MAXIMA) + "…";
    }

    /** Una lectura: junta lo que dice el archivo y lo que no se entiende. */
    private static final class Lectura {

        private final List<EtapaEscrita> etapas = new ArrayList<>();
        private final List<FirmaEscrita> firmas = new ArrayList<>();
        private final List<String> problemas = new ArrayList<>();

        Borrador de(MappingNode raiz) {
            Map<String, Node> campos = campos(raiz, CLAVES_RAIZ);
            if (campos.containsKey(ETAPAS)) {
                leerEtapas(campos.get(ETAPAS));
            }
            if (campos.containsKey(FIRMAS)) {
                leerFirmas(campos.get(FIRMAS));
            }
            return new Borrador(etapas, firmas, problemas);
        }

        private void leerEtapas(Node nodo) {
            if (!(nodo instanceof MappingNode mapa)) {
                problemas.add("Línea " + linea(nodo) + ": etapas debe ser un mapa con cada etapa.");
                return;
            }
            // Una por una, sin campos(): la etapa repetida la dice el dominio, en sus palabras.
            for (NodeTuple entrada : mapa.getValue()) {
                if (entrada.getKeyNode() instanceof ScalarNode clave) {
                    leerEtapa(clave.getValue(), entrada.getValueNode());
                } else {
                    claveQueNoEsTexto(entrada.getKeyNode());
                }
            }
        }

        private void leerEtapa(String tipo, Node nodo) {
            if (!(nodo instanceof MappingNode datos)) {
                problemas.add(
                        "Línea "
                                + linea(nodo)
                                + ": la etapa "
                                + corta(tipo)
                                + " debe tener sus datos.");
                return;
            }
            Map<String, Node> campos = campos(datos, CLAVES_ETAPA);
            etapas.add(
                    new EtapaEscrita(
                            tipo,
                            valor(campos, ACTIVA),
                            valor(campos, NOMBRE),
                            valor(campos, ROL),
                            valor(campos, PLAZO),
                            valor(campos, DEVOLUCION)));
        }

        private void leerFirmas(Node nodo) {
            if (!(nodo instanceof SequenceNode lista)) {
                problemas.add("Línea " + linea(nodo) + ": firmas debe ser una lista.");
                return;
            }
            for (Node firma : lista.getValue()) {
                if (firma instanceof MappingNode datos) {
                    Map<String, Node> campos = campos(datos, CLAVES_FIRMA);
                    firmas.add(
                            new FirmaEscrita(
                                    valor(campos, ORDEN),
                                    valor(campos, ETIQUETA),
                                    valor(campos, ROL),
                                    valor(campos, ETAPA),
                                    valor(campos, DOCUMENTO)));
                } else {
                    problemas.add("Línea " + linea(firma) + ": cada firma debe tener sus datos.");
                }
            }
        }

        private Map<String, Node> campos(MappingNode mapa, Set<String> conocidas) {
            Map<String, Node> campos = new LinkedHashMap<>();
            for (NodeTuple entrada : mapa.getValue()) {
                if (!(entrada.getKeyNode() instanceof ScalarNode clave)) {
                    claveQueNoEsTexto(entrada.getKeyNode());
                } else if (!conocidas.contains(clave.getValue())) {
                    problemas.add(
                            "Línea "
                                    + linea(clave)
                                    + ": la clave "
                                    + corta(clave.getValue())
                                    + " no existe.");
                } else if (campos.putIfAbsent(clave.getValue(), entrada.getValueNode()) != null) {
                    problemas.add(
                            "Línea "
                                    + linea(clave)
                                    + ": la clave "
                                    + clave.getValue()
                                    + " está repetida.");
                }
            }
            return campos;
        }

        private void claveQueNoEsTexto(Node clave) {
            problemas.add("Línea " + linea(clave) + ": una clave debe ser un texto.");
        }

        private String valor(Map<String, Node> campos, String clave) {
            Node nodo = campos.get(clave);
            if (nodo == null) {
                return null;
            }
            if (!(nodo instanceof ScalarNode valor)) {
                problemas.add("Línea " + linea(nodo) + ": " + clave + " debe ser un solo valor.");
                return null;
            }
            return Tag.NULL.equals(valor.getTag()) ? null : valor.getValue();
        }
    }
}
