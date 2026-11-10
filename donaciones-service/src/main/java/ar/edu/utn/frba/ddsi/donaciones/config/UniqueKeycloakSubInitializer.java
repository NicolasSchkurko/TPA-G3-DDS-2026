package ar.edu.utn.frba.ddsi.donaciones.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Crea los índices únicos sobre {@code keycloak_sub} si todavía no existen.
 *
 * <p>{@code ddl-auto=update} no genera la constraint en la configuración actual del dialecto,
 * así que la unicidad se aplica explícitamente acá. Es idempotente: sólo crea lo que falta.</p>
 */
@Component
@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
public class UniqueKeycloakSubInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UniqueKeycloakSubInitializer.class);

    private final DataSource dataSource;

    public UniqueKeycloakSubInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            for (String tabla : new String[]{"donante", "entidad_beneficiaria", "administrador"}) {
                asegurar(connection, tabla, "keycloak_sub");
            }
        } catch (SQLException e) {
            // No bloqueamos el arranque: la lógica de la app sigue protegiendo contra duplicados.
            log.error("No se pudieron verificar los índices únicos de keycloak_sub: {}", e.getMessage());
        }
    }

    private void asegurar(Connection connection, String tabla, String columna) throws SQLException {
        if (indexUnicoExiste(connection, tabla, columna)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE " + tabla + " ADD CONSTRAINT uq_" + tabla + "_keycloak_sub UNIQUE (" + columna + ")")) {
            statement.execute();
            log.info("Índice único creado sobre {}.{}", tabla, columna);
        } catch (SQLException e) {
            // Si ya existe o no se puede crear, lo logueamos y seguimos.
            log.warn("No se pudo crear el índice único sobre {}.{}: {}", tabla, columna, e.getMessage());
        }
    }

    private boolean indexUnicoExiste(Connection connection, String tabla, String columna) throws SQLException {
        String sql =
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? AND NON_UNIQUE = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tabla);
            statement.setString(2, columna);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
