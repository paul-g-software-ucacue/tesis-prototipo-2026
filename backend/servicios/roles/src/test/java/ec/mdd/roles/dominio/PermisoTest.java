package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tipos de permiso, alcances, funcionalidades y permisos válidos (RF-02, D-64, D-65). */
class PermisoTest {

    private static final CatalogoDeFuncionalidades CATALOGO =
            new CatalogoDeFuncionalidades(
                    List.of(
                            new Funcionalidad(
                                    "CARGAS",
                                    "Cargas de datos",
                                    EnumSet.of(TipoDePermiso.LECTURA, TipoDePermiso.ESCRITURA)),
                            new Funcionalidad(
                                    "VOTACION_OCS",
                                    "Votación del OCS",
                                    EnumSet.of(TipoDePermiso.APROBACION))));

    @Test
    void dadoLosTiposDePermiso_cuandoSeListan_entoncesSonLosCuatroDeRf02EnEspanol() {
        assertThat(TipoDePermiso.values())
                .containsExactly(
                        TipoDePermiso.LECTURA,
                        TipoDePermiso.ESCRITURA,
                        TipoDePermiso.APROBACION,
                        TipoDePermiso.FIRMA);
        assertThat(TipoDePermiso.APROBACION.nombre()).isEqualTo("aprobación");
    }

    @Test
    void dadoDosAlcances_cuandoSeCombinan_entoncesGanaElMasAmplio() {
        assertThat(Alcance.PROPIO.masAmplio(Alcance.TODO)).isEqualTo(Alcance.TODO);
        assertThat(Alcance.TODO.masAmplio(Alcance.PROPIO)).isEqualTo(Alcance.TODO);
        assertThat(Alcance.PROPIO.masAmplio(Alcance.PROPIO)).isEqualTo(Alcance.PROPIO);
    }

    @Test
    void dadoUnPermiso_cuandoSeEscribe_entoncesUsaLaFormaDelToken() {
        assertThat(new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO).texto())
                .isEqualTo("CARGAS:LECTURA");
        assertThat(new Permiso("OFERTA", TipoDePermiso.ESCRITURA, Alcance.PROPIO).texto())
                .isEqualTo("OFERTA:ESCRITURA:PROPIO");
    }

    @Test
    void dadoPermisosQueSuFuncionalidadAdmite_cuandoSeValidan_entoncesPasan() {
        CATALOGO.validar(
                List.of(
                        new Permiso("CARGAS", TipoDePermiso.ESCRITURA, Alcance.TODO),
                        new Permiso("VOTACION_OCS", TipoDePermiso.APROBACION, Alcance.TODO)));
    }

    @Test
    void dadoUnTipoQueLaFuncionalidadNoAdmite_cuandoSeValida_entoncesSeRechazaEnEspanol() {
        assertThatThrownBy(
                        () ->
                                CATALOGO.validar(
                                        List.of(
                                                new Permiso(
                                                        "CARGAS",
                                                        TipoDePermiso.APROBACION,
                                                        Alcance.TODO))))
                .isInstanceOf(PermisosInvalidos.class)
                .hasMessage("«Cargas de datos» no admite el permiso de aprobación.");
    }

    @Test
    void dadoUnaFuncionalidadQueNoExiste_cuandoSeValida_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                CATALOGO.validar(
                                        List.of(
                                                new Permiso(
                                                        "INVENTADA",
                                                        TipoDePermiso.LECTURA,
                                                        Alcance.TODO))))
                .isInstanceOf(PermisosInvalidos.class)
                .hasMessage("La funcionalidad INVENTADA no existe.");
    }

    @Test
    void dadoElMismoPermisoDosVeces_cuandoSeValida_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                CATALOGO.validar(
                                        List.of(
                                                new Permiso(
                                                        "CARGAS",
                                                        TipoDePermiso.LECTURA,
                                                        Alcance.TODO),
                                                new Permiso(
                                                        "CARGAS",
                                                        TipoDePermiso.LECTURA,
                                                        Alcance.PROPIO))))
                .isInstanceOf(PermisosInvalidos.class)
                .hasMessage("El permiso de lectura de «Cargas de datos» está repetido.");
    }

    @Test
    void dadoElCatalogo_cuandoSeListan_entoncesConservaElOrden() {
        assertThat(CATALOGO.funcionalidades())
                .extracting(Funcionalidad::codigo)
                .containsExactly("CARGAS", "VOTACION_OCS");
        assertThat(CATALOGO.funcionalidades().getFirst().admite(TipoDePermiso.FIRMA)).isFalse();
    }
}
