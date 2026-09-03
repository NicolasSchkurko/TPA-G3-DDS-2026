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
import java.util.HashMap;
import java.util.Map;

@EnableJpaRepositories(
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.rutas"
)

@Configuration
public class RutasDataSourceConfig {

    @Bean
    public DataSource rutasDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/rutas")
                .username("valentin")
                .password("10032001")
                .build();
    }

    @Bean(name = "rutasEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("rutasDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta"
        );
        emf.setPersistenceUnitName("rutas");
        emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        return emf;
    }

    @Bean(name = "rutasTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("rutasEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}