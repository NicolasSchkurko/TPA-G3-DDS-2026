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
        basePackages = "ar.edu.utn.frba.ddsi.logisticas.models.repositories.items",
        entityManagerFactoryRef = "itemsEntityManagerFactory",
        transactionManagerRef = "teimsTransactionManager"

)

@Configuration
public class ItemsDataSourceConfig {

    @Bean
    public DataSource itemsDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/items")
                .username("valentin")
                .password("10032001")
                .build();
    }

    @Bean(name = "itemsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("itemsDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega"
        );
        emf.setPersistenceUnitName("items");
        emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        return emf;
    }

    @Bean(name = "itemsTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("itemsEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}