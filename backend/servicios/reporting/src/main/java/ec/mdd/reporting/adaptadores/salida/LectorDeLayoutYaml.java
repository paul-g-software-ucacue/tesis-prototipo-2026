package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.BorradorDeColumna;
import ec.mdd.reporting.dominio.BorradorDeLayout;
import ec.mdd.reporting.dominio.ProblemaDelLayout;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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
 * El archivo de mapeo en YAML, leído con las reglas de la configuración del flujo (F12-H01,
 * research §7, D-119), sin usar sus clases (principio V).
 *
 * <p>Primero recorre los eventos del lector: un ancla, un alias o una etiqueta explícita rechazan
 * el archivo, así que no hay bomba de alias posible ni tipos que interpretar. Después arma solo el
 * árbol de nodos y toma cada valor como el texto escrito: «01» sigue siendo «01». Lo que no
 * entiende queda como problema del borrador, para que salga junto con los de la validación.
 */
public final class LectorDeLayoutYaml implements LectorDeLayout {

    private static final String COLUMNAS = "columnas";
    private static final String NOMBRE = "nombre";
    private static final String CAMPO = "campo";
    private static final String TRADUCCION = "traduccion";
    private static final String ARCHIVO = "archivo";

    private static final Set<String> CLAVES_RAIZ = Set.of(COLUMNAS);
    private static final Set<String> CLAVES_COLUMNA = Set.of(NOMBRE, CAMPO, TRADUCCION);

    /** Columnas → lista → columna → traducción. */
    private static final int PROFUNDIDAD_MAXIMA = 4;

    /** 256 KB: el controlador ya rechazó lo que pasa de ahí. */
    private static final int TAMANO_MAXIMO = 262_144;

    private static final int CLAVE_MAXIMA = 40;

    @Override
    public LecturaDelLayout leer(byte[] archivo) {
        String texto = sinMarcaDeOrden(utf8(archivo));
        Yaml yaml = new Yaml(new SafeConstructor(opciones()));
        try {
            rechazarLoQueNoHaceFalta(yaml, texto);
            Node raiz = yaml.compose(new StringReader(texto));
            if (raiz == null) {
                return new LecturaDelLayout(texto, new BorradorDeLayout(List.of(), List.of()));
            }
            if (raiz instanceof MappingNode mapa) {
                return new LecturaDelLayout(texto, new Lectura().de(mapa));
            }
            throw new ArchivoIlegible(
                    "La raíz del archivo de mapeo no es un mapa.",
                    linea(raiz.getStartMark()),
                    null);
        } catch (MarkedYAMLException error) {
            throw new ArchivoIlegible(
                    "YAML inválido: " + error.getMessage(), linea(error.getProblemMark()), error);
        } catch (YAMLException error) {
            throw new ArchivoIlegible("YAML inválido: " + error.getMessage(), null, error);
        }
    }

    private static LoaderOptions opciones() {
        LoaderOptions opciones = new LoaderOptions();
        opciones.setAllowDuplicateKeys(false);
        opciones.setAllowRecursiveKeys(false);
        opciones.setMaxAliasesForCollections(0);
        opciones.setNestingDepthLimit(PROFUNDIDAD_MAXIMA);
        opciones.setCodePointLimit(TAMANO_MAXIMO);
        return opciones;
    }

    // UTF-8 estricto: un byte inválido no se reemplaza en silencio por «�».
    private static String utf8(byte[] archivo) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(archivo))
                    .toString();
        } catch (CharacterCodingException error) {
            throw new ArchivoIlegible("El archivo de mapeo no es UTF-8.", null, error);
        }
    }

    // Los editores de Windows agregan la marca; no es parte del archivo.
    private static String sinMarcaDeOrden(String texto) {
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    private static void rechazarLoQueNoHaceFalta(Yaml yaml, String texto) {
        for (Event evento : yaml.parse(new StringReader(texto))) {
            boolean conAncla = evento instanceof NodeEvent nodo && nodo.getAnchor() != null;
            boolean conEtiqueta =
                    evento instanceof ScalarEvent valor && valor.getTag() != null
                            || evento instanceof CollectionStartEvent grupo
                                    && grupo.getTag() != null;
            if (conAncla || conEtiqueta) {
                throw new ArchivoIlegible(
                        "El archivo de mapeo tiene un ancla, un alias o una etiqueta.",
                        linea(evento.getStartMark()),
                        null);
            }
        }
    }

    private static Integer linea(Mark marca) {
        return marca == null ? null : marca.getLine() + 1;
    }

    private static int linea(Node nodo) {
        return nodo.getStartMark().getLine() + 1;
    }

    private static String corta(String texto) {
        return texto.length() <= CLAVE_MAXIMA ? texto : texto.substring(0, CLAVE_MAXIMA) + "…";
    }

    /** Una lectura: junta las columnas escritas y lo que no se entiende. */
    private static final class Lectura {

        private final List<BorradorDeColumna> columnas = new ArrayList<>();
        private final List<ProblemaDelLayout> problemas = new ArrayList<>();

        BorradorDeLayout de(MappingNode raiz) {
            Map<String, Node> campos = campos(raiz, CLAVES_RAIZ, ARCHIVO, "«columnas»");
            Node lista = campos.get(COLUMNAS);
            if (lista instanceof SequenceNode secuencia) {
                for (Node nodo : secuencia.getValue()) {
                    leerColumna(nodo, "columnas[" + (columnas.size() + 1) + "]");
                }
            } else if (lista != null) {
                problemas.add(
                        new ProblemaDelLayout(
                                ARCHIVO,
                                "Línea " + linea(lista) + ": «columnas» debe ser una lista."));
            }
            return new BorradorDeLayout(columnas, problemas);
        }

        private void leerColumna(Node nodo, String donde) {
            if (!(nodo instanceof MappingNode datos)) {
                problemas.add(
                        new ProblemaDelLayout(
                                donde,
                                "Línea "
                                        + linea(nodo)
                                        + ": cada columna tiene «nombre» y «campo»."));
                columnas.add(new BorradorDeColumna(null, null, null));
                return;
            }
            Map<String, Node> campos =
                    campos(datos, CLAVES_COLUMNA, donde, "«nombre», «campo» y «traduccion»");
            columnas.add(
                    new BorradorDeColumna(
                            valor(campos, NOMBRE, donde),
                            valor(campos, CAMPO, donde),
                            traduccion(campos.get(TRADUCCION), donde)));
        }

        private Map<String, String> traduccion(Node nodo, String donde) {
            if (nodo == null) {
                return null;
            }
            if (!(nodo instanceof MappingNode mapa)) {
                problemas.add(
                        new ProblemaDelLayout(
                                donde,
                                "Línea "
                                        + linea(nodo)
                                        + ": «traduccion» debe tener un código y su valor por"
                                        + " línea."));
                return null;
            }
            Map<String, String> traduccion = new LinkedHashMap<>();
            for (NodeTuple par : mapa.getValue()) {
                if (par.getKeyNode() instanceof ScalarNode codigo
                        && par.getValueNode() instanceof ScalarNode valor) {
                    if (traduccion.putIfAbsent(codigo.getValue(), texto(valor)) != null) {
                        problemas.add(
                                new ProblemaDelLayout(
                                        donde,
                                        "Línea "
                                                + linea(codigo)
                                                + ": el código «"
                                                + corta(codigo.getValue())
                                                + "» está repetido."));
                    }
                } else {
                    problemas.add(
                            new ProblemaDelLayout(
                                    donde,
                                    "Línea "
                                            + linea(par.getKeyNode())
                                            + ": «traduccion» debe tener un código y su valor por"
                                            + " línea."));
                }
            }
            return traduccion;
        }

        private Map<String, Node> campos(
                MappingNode mapa, Set<String> conocidas, String donde, String cuales) {
            Map<String, Node> campos = new LinkedHashMap<>();
            for (NodeTuple entrada : mapa.getValue()) {
                if (!(entrada.getKeyNode() instanceof ScalarNode clave)) {
                    problemas.add(
                            new ProblemaDelLayout(
                                    donde,
                                    "Línea "
                                            + linea(entrada.getKeyNode())
                                            + ": una clave debe ser un texto."));
                } else if (!conocidas.contains(clave.getValue())) {
                    problemas.add(
                            new ProblemaDelLayout(
                                    donde,
                                    "Línea "
                                            + linea(clave)
                                            + ": la clave «"
                                            + corta(clave.getValue())
                                            + "» no existe. Usa solo "
                                            + cuales
                                            + "."));
                } else if (campos.putIfAbsent(clave.getValue(), entrada.getValueNode()) != null) {
                    problemas.add(
                            new ProblemaDelLayout(
                                    donde,
                                    "Línea "
                                            + linea(clave)
                                            + ": la clave «"
                                            + clave.getValue()
                                            + "» está repetida."));
                }
            }
            return campos;
        }

        private String valor(Map<String, Node> campos, String clave, String donde) {
            Node nodo = campos.get(clave);
            if (nodo == null) {
                return null;
            }
            if (!(nodo instanceof ScalarNode valor)) {
                problemas.add(
                        new ProblemaDelLayout(
                                donde,
                                "Línea "
                                        + linea(nodo)
                                        + ": «"
                                        + clave
                                        + "» debe ser un solo valor."));
                return null;
            }
            return texto(valor);
        }

        private static String texto(ScalarNode valor) {
            return Tag.NULL.equals(valor.getTag()) ? null : valor.getValue();
        }
    }
}
