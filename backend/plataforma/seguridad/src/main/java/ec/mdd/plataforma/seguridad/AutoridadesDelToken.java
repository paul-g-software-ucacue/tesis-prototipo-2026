package ec.mdd.plataforma.seguridad;

import ec.mdd.plataforma.errores.nucleo.AlcanceDeCarrera;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Las autoridades de un token: {@code ROLE_<rol>} por cada rol del claim {@code roles} y {@code
 * PERMISO_<FUNCIONALIDAD>:<TIPO>} por cada permiso del claim {@code permisos} (ADR-021).
 *
 * <p>El alcance ({@code :PROPIO}) no forma parte de la autoridad: decide qué datos ve el usuario,
 * no si puede usar la operación (D-64). Por eso va en otra autoridad, junto con la carrera del
 * claim {@code carrera}: las leen los servicios con {@code AlcanceDeCarrera}, sin conocer el token.
 * Un token sin {@code permisos}, como el de proceso, solo da sus roles.
 */
public final class AutoridadesDelToken implements Converter<Jwt, Collection<GrantedAuthority>> {

    /** Prefijo de las autoridades que vienen de un permiso. */
    public static final String PREFIJO_PERMISO = "PERMISO_";

    private static final String PROPIO = ":PROPIO";

    @Override
    public Collection<GrantedAuthority> convert(Jwt token) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (String rol : lista(token, "roles")) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol));
        }
        for (String permiso : lista(token, "permisos")) {
            String sinAlcance =
                    permiso.endsWith(PROPIO)
                            ? permiso.substring(0, permiso.length() - PROPIO.length())
                            : permiso;
            autoridades.add(new SimpleGrantedAuthority(PREFIJO_PERMISO + sinAlcance));
            if (permiso.endsWith(PROPIO)) {
                autoridades.add(
                        new SimpleGrantedAuthority(AlcanceDeCarrera.PREFIJO_PROPIO + sinAlcance));
            }
        }
        String carrera = token.getClaimAsString("carrera");
        if (carrera != null) {
            autoridades.add(new SimpleGrantedAuthority(AlcanceDeCarrera.PREFIJO_CARRERA + carrera));
        }
        return autoridades;
    }

    private static List<String> lista(Jwt token, String claim) {
        List<String> valores = token.getClaimAsStringList(claim);
        return valores == null ? List.of() : valores;
    }
}
