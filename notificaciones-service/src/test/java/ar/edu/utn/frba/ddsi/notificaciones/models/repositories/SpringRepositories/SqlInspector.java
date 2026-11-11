package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Captura el SQL que emite Hibernate para poder afirmar qué sentencias se ejecutan. */
public class SqlInspector implements StatementInspector {

    public static final List<String> SENTENCIAS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        SENTENCIAS.add(sql);
        return sql;
    }
}
