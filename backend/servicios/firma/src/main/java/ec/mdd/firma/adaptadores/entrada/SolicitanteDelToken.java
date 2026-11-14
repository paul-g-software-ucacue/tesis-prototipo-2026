package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.puertos.entrada.Solicitante;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quién pide y sus roles, del token de la petición en curso (F13-H02, F14-H01). */
final class SolicitanteDelToken {

    private static final String ROL = "ROLE_";

    private SolicitanteDelToken() {}

    static Solicitante actual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles =
                autenticacion.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(una -> una.startsWith(ROL))
                        .map(una -> una.substring(ROL.length()))
                        .collect(Collectors.toSet());
        return new Solicitante(autenticacion.getName(), roles);
    }
}
