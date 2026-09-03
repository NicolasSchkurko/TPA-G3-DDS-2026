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
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.camiones",
        entityManagerFactoryRef = "camionesEntityManagerFactory",
        transactionManagerRef = "camionesTransactionManager"
)

@Configuration
public class CamionesDataSourceConfig {

    @Bean
    public DataSource camionesDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/camiones")
                .username("valentin")
                .password("10032001")
                .build();
    }

    @Bean(name = "camionesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("camionesDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion"
        );
        emf.setPersistenceUnitName("camiones");
        emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        return emf;
    }

    @Bean(name = "camionesTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("camionesEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}