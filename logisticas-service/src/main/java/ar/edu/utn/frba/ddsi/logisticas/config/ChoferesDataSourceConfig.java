package ar.edu.utn.frba.ddsi.logisticas.config;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableJpaRepositories(
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.choferes",
        entityManagerFactoryRef = "choferesEntityManagerFactory",
        transactionManagerRef = "choferesTransactionManager"
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

    @Bean(name = "choferesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("choferesDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer"
        );
        emf.setPersistenceUnitName("choferes");
        emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        return emf;
    }

    @Bean(name = "choferesTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("choferesEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}