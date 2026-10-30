package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class Secuencia {

    private static MongoTemplate mongoTemplate;
    private static final AtomicLong fallbackEnMemoria = new AtomicLong(1);

    private final MongoTemplate mongoTemplateInstance;

    public Secuencia(MongoTemplate mongoTemplate) {
        this.mongoTemplateInstance = mongoTemplate;
    }

    @PostConstruct
    public void inicializar() {
        Secuencia.mongoTemplate = mongoTemplateInstance;
    }

    public static void setMongoTemplate(MongoTemplate template) {
        Secuencia.mongoTemplate = template;
    }

    public static Long siguiente() {
        if (mongoTemplate == null) {
            return fallbackEnMemoria.getAndIncrement();
        }

        Query query = new Query(
                Criteria.where("_id").is("evento_logistica")
        );

        Update update = new Update().inc("valor", 1);

        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(true)
                .upsert(true);

        SecuenciaDocumento secuencia = mongoTemplate.findAndModify(
                query,
                update,
                options,
                SecuenciaDocumento.class
        );

        return (secuencia != null && secuencia.getValor() != null)
                ? secuencia.getValor()
                : fallbackEnMemoria.getAndIncrement();
    }
}