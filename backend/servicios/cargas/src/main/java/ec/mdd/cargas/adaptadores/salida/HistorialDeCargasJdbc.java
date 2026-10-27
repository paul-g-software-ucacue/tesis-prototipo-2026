package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** El historial de cargas en {@code cargas.historial_carga} (V2), con {@link JdbcClient}. */
public final class HistorialDeCargasJdbc implements HistorialDeCargas {

    private static final String COLUMNAS =
            "carga, usuario, momento, archivo, hash, resultado, nuevos, cambiados,"
                    + " bajas_sin_aplicar, motivo";

    private final JdbcClient jdbc;

    /** Recibe el cliente de la base del proceso. */
    public HistorialDeCargasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(EntradaDeHistorial entrada) {
        jdbc.sql(
                        "INSERT INTO cargas.historial_carga ("
                                + COLUMNAS
                                + ") VALUES (:carga, :usuario, :momento, :archivo, :hash,"
                                + " :resultado, :nuevos, :cambiados, :bajas, :motivo)")
                .param("carga", entrada.carga())
                .param("usuario", entrada.usuario())
                .param("momento", Timestamp.from(entrada.momento()))
                .param("archivo", entrada.archivo())
                .param("hash", entrada.hash())
                .param("resultado", entrada.resultado().name())
                .param("nuevos", entrada.nuevos())
                .param("cambiados", entrada.cambiados())
                .param("bajas", entrada.bajasSinAplicar())
                .param("motivo", entrada.motivo().map(Enum::name).orElse(null))
                .update();
    }

    @Override
    public List<EntradaDeHistorial> deLaCarga(String carga) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM cargas.historial_carga WHERE carga = :carga"
                                + " ORDER BY momento DESC, id DESC")
                .param("carga", carga)
                .query((fila, numero) -> entrada(fila))
                .list();
    }

    @Override
    public Optional<EntradaDeHistorial> aplicada(String carga, String hash) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM cargas.historial_carga WHERE carga = :carga AND hash ="
                                + " :hash AND resultado = 'APLICADA' ORDER BY momento, id LIMIT 1")
                .param("carga", carga)
                .param("hash", hash)
                .query((fila, numero) -> entrada(fila))
                .optional();
    }

    private static EntradaDeHistorial entrada(ResultSet fila) throws SQLException {
        String motivo = fila.getString("motivo");
        return new EntradaDeHistorial(
                fila.getString("carga"),
                fila.getString("usuario"),
                fila.getTimestamp("momento").toInstant(),
                fila.getString("archivo"),
                fila.getString("hash"),
                ResultadoDeHistorial.valueOf(fila.getString("resultado")),
                fila.getInt("nuevos"),
                fila.getInt("cambiados"),
                fila.getInt("bajas_sin_aplicar"),
                Optional.ofNullable(motivo).map(MotivoDeRechazo::valueOf));
    }
}
