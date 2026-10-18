package ec.mdd.roles.puertos.salida;

import ec.mdd.roles.dominio.CatalogoDeFuncionalidades;
import ec.mdd.roles.dominio.Rol;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Las funcionalidades y los roles con sus permisos (data-model, tablas del esquema roles). */
public interface RepositorioRoles {

    /** Las funcionalidades, en su orden, con los tipos que admite cada una. */
    CatalogoDeFuncionalidades funcionalidades();

    /** Todos los roles, ordenados por nombre. */
    List<Rol> todos();

    /** El rol con ese identificador, si existe. */
    Optional<Rol> buscar(String identificador);

    /**
     * Crea el rol o, si su identificador ya existe, reemplaza su nombre, su descripción y sus
     * permisos, todo junto.
     *
     * @param momento fecha de creación o de la última edición
     */
    void guardar(Rol rol, Instant momento);

    /** Borra el rol y sus permisos. No comprueba las reglas: eso es de la aplicación. */
    void borrar(String identificador);
}
