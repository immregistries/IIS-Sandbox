package org.immregistries.iis.kernal.persisted;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
// @PropertySource({"application.properties"})
@EntityScan("org.immregistries.iis.kernal.persisted.model")
@EnableJpaRepositories(basePackages = "org.immregistries.iis.kernal.persisted.model", entityManagerFactoryRef = "iisLocalEntityManager", transactionManagerRef = "iisLocalTransactionManager")
public class SecondDatabaseConfiguration {
	@Autowired
	private Environment env;

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private static SessionFactory sessionFactory = null;

	@Bean
	@ConfigurationProperties(prefix = "spring.second-datasource")
	public DataSource iisLocalDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean iisLocalEntityManager(
			@Qualifier("iisLocalDataSource") DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan(
				new String[] { "org.immregistries.iis.kernal.model.persisted" });

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2ddl.auto",
				env.getProperty("hibernate.hbm2ddl.auto"));
		properties.put("hibernate.dialect",
				env.getProperty("hibernate.dialect"));
		em.setJpaPropertyMap(properties);

		return em;
	}

	@Bean
	public PlatformTransactionManager iisLocalTransactionManager(
			@Qualifier("iisLocalEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(
				entityManagerFactory.getObject());
		return transactionManager;
	}

	@Bean
	public SessionFactory sessionFactory(
			@Qualifier("iisLocalEntityManager") EntityManagerFactory entityManagerFactory) {
		// The LCEFBean produces an EntityManagerFactory.
		// If the provider is Hibernate, this object is also a SessionFactory.

		if (entityManagerFactory.unwrap(SessionFactory.class) == null) {
			throw new IllegalStateException("The JPA EntityManagerFactory is not a Hibernate SessionFactory!");
		}

		// This is the cleanest and most reliable way to get the native Hibernate
		// object.
		SessionFactory unwrap = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory = unwrap;
		return unwrap;
	}

}