package ec.mdd.validacionocs.dominio;

import ec.mdd.validacionocs.dominio.Borrador.EtapaEscrita;
import ec.mdd.validacionocs.dominio.Borrador.FirmaEscrita;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Las etapas y el árbol de firmas de un periodo (RF-34, RF-58, ADR-007). Solo existe si cumple sus
 * reglas: {@link #revisar(Borrador)} la crea, o dice todo lo que hay que corregir (FR-015).
 */
public final class ConfiguracionFlujo {

    /** El tamaño máximo del archivo: 1 MB (FR-010). */
    public static final int TAMANO_MAXIMO = 1_048_576;

    private static final int NOMBRE_MAXIMO = 80;
    private static final int ETIQUETA_MAXIMA = 40;
    private static final int TEXTO_REPETIDO_MAXIMO = 40;
    private static final int PROBLEMAS_MAXIMOS = 50;
    private static final String RESPONSABLE = "RESPONSABLE_CONSOLIDACION";

    // Los 10 roles fijos (D-63): así un rol mal escrito se dice sin llamar a otro proceso.
    private static final Set<String> ROLES =
            Set.of(
                    "ADMINISTRADOR",
                    "COORDINADOR_CARRERA",
                    "DOCENTE",
                    "GESTOR_UNIDAD",
                    "MIEMBRO_OCS",
                    "OPERADOR_DATOS",
                    "RECTOR",
                    RESPONSABLE,
                    "SECRETARIA_GENERAL",
                    "VICERRECTOR");

    // Hasta cuatro cifras: el plazo son días, y un número enorme es un error de escritura.
    private static final Pattern PLAZO = Pattern.compile("[1-9]\\d{0,3}");
    private static final Pattern ORDEN = Pattern.compile("[1-9]\\d{0,2}");

    private final List<Etapa> etapas;
    private final List<PasoFirma> firmas;

    private ConfiguracionFlujo(List<Etapa> etapas, List<PasoFirma> firmas) {
        this.etapas = List.copyOf(etapas);
        this.firmas = List.copyOf(firmas);
    }

    /**
     * Revisa un archivo sin mirar quién puede firmar: la configuración por defecto rige sin
     * cargarse, y una guardada ya se comprobó al cargarla.
     *
     * @param borrador lo que dice el archivo
     * @return la configuración, o sus problemas
     */
    public static Revision revisar(Borrador borrador) {
        return new Revisor(borrador).revisar(Optional.empty());
    }

    /**
     * Revisa un archivo que se está cargando: además, cada rol firmante necesita un usuario y el
     * Responsable tiene que estar designado (FR-013, FR-014).
     *
     * @param borrador lo que dice el archivo
     * @param firmantes quién puede firmar en el periodo
     * @return la configuración, o sus problemas
     */
    public static Revision revisar(Borrador borrador, Firmantes firmantes) {
        return new Revisor(borrador).revisar(Optional.of(firmantes));
    }

    /** Las 9 etapas, en el orden del flujo. */
    public List<Etapa> etapas() {
        return etapas;
    }

    /** Las firmas, por su orden. */
    public List<PasoFirma> firmas() {
        return firmas;
    }

    private static String corta(String texto) {
        return texto.length() <= TEXTO_REPETIDO_MAXIMO
                ? texto
                : texto.substring(0, TEXTO_REPETIDO_MAXIMO) + "…";
    }

    private static boolean falta(String texto) {
        return texto == null || texto.isBlank();
    }

    private static boolean esBooleano(String texto) {
        return "true".equals(texto) || "false".equals(texto);
    }

    /** Una revisión: junta los problemas de un archivo. */
    private static final class Revisor {

        private final Borrador borrador;
        private final List<String> problemas = new ArrayList<>();
        private final Map<TipoDeEtapa, Etapa> etapas = new EnumMap<>(TipoDeEtapa.class);
        private final List<PasoFirma> firmas = new ArrayList<>();

        Revisor(Borrador borrador) {
            this.borrador = borrador;
            problemas.addAll(borrador.problemas());
        }

        Revision revisar(Optional<Firmantes> firmantes) {
            revisarEtapas();
            revisarFirmas();
            firmantes.ifPresent(this::revisarFirmantes);
            if (!problemas.isEmpty()) {
                return new Revision(Optional.empty(), limitados());
            }
            firmas.sort(Comparator.comparingInt(PasoFirma::orden));
            return new Revision(
                    Optional.of(new ConfiguracionFlujo(new ArrayList<>(etapas.values()), firmas)),
                    List.of());
        }

        // Un archivo de 1 MB puede tener miles de errores: con los primeros basta para corregir.
        private List<String> limitados() {
            if (problemas.size() <= PROBLEMAS_MAXIMOS) {
                return problemas;
            }
            List<String> primeros = new ArrayList<>(problemas.subList(0, PROBLEMAS_MAXIMOS));
            primeros.add("Hay " + (problemas.size() - PROBLEMAS_MAXIMOS) + " problemas más.");
            return primeros;
        }

        private void revisarEtapas() {
            Set<TipoDeEtapa> vistas = new LinkedHashSet<>();
            for (EtapaEscrita escrita : borrador.etapas()) {
                Optional<TipoDeEtapa> tipo = tipoDe(escrita.tipo());
                if (tipo.isEmpty()) {
                    problemas.add(
                            "La etapa " + corta(String.valueOf(escrita.tipo())) + " no existe.");
                } else if (!vistas.add(tipo.get())) {
                    problemas.add("La etapa " + tipo.get() + " está repetida.");
                } else {
                    revisar(tipo.get(), escrita);
                }
            }
            for (TipoDeEtapa tipo : TipoDeEtapa.values()) {
                if (!vistas.contains(tipo)) {
                    problemas.add("Falta la etapa " + tipo + ".");
                }
            }
        }

        private void revisar(TipoDeEtapa tipo, EtapaEscrita escrita) {
            int antes = problemas.size();
            boolean activa = "true".equals(escrita.activa());
            if (!esBooleano(escrita.activa())) {
                problemas.add("En la etapa " + tipo + ", activa debe ser true o false.");
            } else if (!activa && tipo.esEsqueleto()) {
                problemas.add("La etapa " + tipo + " no se puede desactivar.");
            }
            if (!esBooleano(escrita.admiteDevolucion())) {
                problemas.add("En la etapa " + tipo + ", admite_devolucion debe ser true o false.");
            }
            revisarNombre(tipo, escrita.nombre());
            revisarRol(tipo, escrita.rol());
            OptionalInt plazo = plazoDe(tipo, escrita.plazoDias(), activa);
            if (problemas.size() == antes) {
                etapas.put(
                        tipo,
                        new Etapa(
                                tipo,
                                activa,
                                escrita.nombre().strip(),
                                escrita.rol(),
                                plazo,
                                "true".equals(escrita.admiteDevolucion())));
            } else {
                // Sin datos válidos, sus firmas se revisan igual: la etapa existe.
                etapas.put(tipo, new Etapa(tipo, activa, "", "", plazo, false));
            }
        }

        private void revisarNombre(TipoDeEtapa tipo, String nombre) {
            if (falta(nombre)) {
                problemas.add("A la etapa " + tipo + " le falta el nombre.");
            } else if (nombre.strip().length() > NOMBRE_MAXIMO) {
                problemas.add(
                        "El nombre de la etapa "
                                + tipo
                                + " pasa de "
                                + NOMBRE_MAXIMO
                                + " caracteres.");
            }
        }

        private void revisarRol(TipoDeEtapa tipo, String rol) {
            if (falta(rol)) {
                problemas.add("A la etapa " + tipo + " le falta el rol.");
            } else if (!ROLES.contains(rol)) {
                problemas.add(
                        "La etapa " + tipo + " tiene el rol " + corta(rol) + ", que no existe.");
            }
        }

        private OptionalInt plazoDe(TipoDeEtapa tipo, String plazo, boolean activa) {
            if (falta(plazo)) {
                if (activa && tipo != TipoDeEtapa.PUBLICADO) {
                    problemas.add("A la etapa " + tipo + " le falta el plazo.");
                }
                return OptionalInt.empty();
            }
            if (!PLAZO.matcher(plazo).matches()) {
                problemas.add(
                        "El plazo de la etapa "
                                + tipo
                                + " debe ser un número entero de días, mayor que 0.");
                return OptionalInt.empty();
            }
            return OptionalInt.of(Integer.parseInt(plazo));
        }

        private void revisarFirmas() {
            if (borrador.firmas().isEmpty()) {
                problemas.add("Falta el árbol de firmas.");
                return;
            }
            Set<Integer> ordenes = new LinkedHashSet<>();
            boolean ordenesValidos = true;
            for (FirmaEscrita escrita : borrador.firmas()) {
                OptionalInt orden = ordenDe(escrita);
                ordenesValidos &= orden.isPresent();
                orden.ifPresent(
                        valor -> {
                            if (!ordenes.add(valor)) {
                                problemas.add(
                                        "El orden " + valor + " de las firmas está repetido.");
                            }
                        });
                revisar(escrita, orden);
            }
            if (ordenesValidos) {
                revisarHuecos(ordenes);
            }
        }

        private void revisarHuecos(Set<Integer> ordenes) {
            for (int orden = 1; orden <= ordenes.size(); orden++) {
                if (!ordenes.contains(orden)) {
                    problemas.add(
                            "Falta la firma de orden " + orden + ": van desde 1, sin huecos.");
                }
            }
        }

        private OptionalInt ordenDe(FirmaEscrita escrita) {
            if (escrita.orden() == null || !ORDEN.matcher(escrita.orden()).matches()) {
                problemas.add(
                        "El orden de la firma "
                                + nombreDe(escrita)
                                + " debe ser un número entero desde 1.");
                return OptionalInt.empty();
            }
            return OptionalInt.of(Integer.parseInt(escrita.orden()));
        }

        private static String nombreDe(FirmaEscrita escrita) {
            return falta(escrita.etiqueta())
                    ? "de orden " + corta(String.valueOf(escrita.orden()))
                    : corta(escrita.etiqueta().strip());
        }

        private void revisar(FirmaEscrita escrita, OptionalInt orden) {
            int antes = problemas.size();
            String firma = nombreDe(escrita);
            if (falta(escrita.etiqueta())) {
                problemas.add("A la firma " + firma + " le falta la etiqueta.");
            } else if (escrita.etiqueta().strip().length() > ETIQUETA_MAXIMA) {
                problemas.add(
                        "La etiqueta de la firma de orden "
                                + corta(String.valueOf(escrita.orden()))
                                + " pasa de "
                                + ETIQUETA_MAXIMA
                                + " caracteres.");
            }
            if (falta(escrita.rol())) {
                problemas.add("A la firma " + firma + " le falta el rol.");
            } else if (!ROLES.contains(escrita.rol())) {
                problemas.add(
                        "La firma "
                                + firma
                                + " tiene el rol "
                                + corta(escrita.rol())
                                + ", que no existe.");
            }
            Optional<TipoDeEtapa> etapa = etapaDe(escrita, firma);
            Optional<DocumentoDeFirma> documento = documentoDe(escrita, firma);
            if (problemas.size() == antes
                    && orden.isPresent()
                    && etapa.isPresent()
                    && documento.isPresent()) {
                firmas.add(
                        new PasoFirma(
                                orden.getAsInt(),
                                escrita.etiqueta().strip(),
                                escrita.rol(),
                                etapa.get(),
                                documento.get()));
            }
        }

        private Optional<TipoDeEtapa> etapaDe(FirmaEscrita escrita, String firma) {
            if (falta(escrita.etapa())) {
                problemas.add("A la firma " + firma + " le falta la etapa.");
                return Optional.empty();
            }
            Optional<TipoDeEtapa> tipo = tipoDe(escrita.etapa());
            if (tipo.isEmpty()) {
                problemas.add(
                        "La firma "
                                + firma
                                + " está en "
                                + corta(escrita.etapa())
                                + ", que no existe.");
            } else if (etapas.containsKey(tipo.get()) && !etapas.get(tipo.get()).activa()) {
                problemas.add(
                        "La firma " + firma + " está en " + tipo.get() + ", que no está activa.");
            }
            return tipo;
        }

        private Optional<DocumentoDeFirma> documentoDe(FirmaEscrita escrita, String firma) {
            if (falta(escrita.documento())) {
                problemas.add("A la firma " + firma + " le falta el documento.");
                return Optional.empty();
            }
            Optional<DocumentoDeFirma> documento =
                    Arrays.stream(DocumentoDeFirma.values())
                            .filter(uno -> uno.name().equals(escrita.documento()))
                            .findFirst();
            if (documento.isEmpty()) {
                problemas.add(
                        "La firma "
                                + firma
                                + " es sobre "
                                + corta(escrita.documento())
                                + ", un documento que no existe.");
            }
            return documento;
        }

        private void revisarFirmantes(Firmantes firmantes) {
            Set<String> roles = new LinkedHashSet<>();
            for (FirmaEscrita escrita : borrador.firmas()) {
                if (escrita.rol() != null && ROLES.contains(escrita.rol())) {
                    roles.add(escrita.rol());
                }
            }
            for (String rol : roles) {
                if (RESPONSABLE.equals(rol)) {
                    if (!firmantes.responsableDesignado()) {
                        problemas.add(
                                "Falta designar al Responsable de consolidación del periodo.");
                    }
                } else if (!firmantes.rolesConUsuario().contains(rol)) {
                    problemas.add("El rol " + rol + " no tiene usuarios.");
                }
            }
        }

        private static Optional<TipoDeEtapa> tipoDe(String texto) {
            return Arrays.stream(TipoDeEtapa.values())
                    .filter(tipo -> tipo.name().equals(texto))
                    .findFirst();
        }
    }
}
