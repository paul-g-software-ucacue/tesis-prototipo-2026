package ec.mdd.demobeta.adaptadores;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Violación de demostración: arma una consulta SQL concatenando un parámetro (escenario 5). */
public class ConsultaInsegura {
    public boolean buscar(Connection conexion, String parametro) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            return sentencia.execute("SELECT * FROM t WHERE c = '" + parametro + "'");
        }
    }
}
