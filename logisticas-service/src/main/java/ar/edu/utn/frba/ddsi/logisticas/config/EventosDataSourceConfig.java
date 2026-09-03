package ar.edu.utn.frba.ddsi.logisticas.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

@EnableJpaRepositories(
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.eventos"
)

@Configuration
public class EventosDataSourceConfig {

    @Bean
    public DataSource eventosDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/eventos")
                .username("valentin")
                .password("10032001")
                .build();
    }

    @Bean(name = "eventosEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("eventosDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica"
        );
        emf.setPersistenceUnitName("eventos");
        emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        return emf;
    }

    @Bean(name = "eventosTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("eventosEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}
