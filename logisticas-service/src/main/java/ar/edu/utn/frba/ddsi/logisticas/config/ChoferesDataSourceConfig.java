package ar.edu.utn.frba.ddsi.logisticas.config;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableJpaRepositories(
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.choferes"
)

@Configuration
public class ChoferesDataSourceConfig {

    @Bean
    public DataSource choferesDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/choferes")
                .username("valentin")
                .password("10032001")
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("choferesDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer")
                .persistenceUnit("choferes")
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}