package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;

public class Secuencia {

    private static MongoTemplate mongoTemplate;

    public Secuencia(MongoTemplate mongoTemplate) {
        Secuencia.mongoTemplate = mongoTemplate;
    }

    public static Long siguiente() {
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

        return secuencia.getValor();
    }
}