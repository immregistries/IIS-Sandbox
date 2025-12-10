package org.immregistries.iis.kernal.model.persisted;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@PropertySource({"classpath:persistence-multiple-db.properties"})
@EnableJpaRepositories(
	basePackages = "org.immregistries.iis.kernal.model.persisted",
	entityManagerFactoryRef = "iisLocalEntityManager",
	transactionManagerRef = "iisLocalTransactionManager"
)
public class ProperDatabaseConfiguration {
	@Autowired
	private Environment env;

	@Bean
	public LocalContainerEntityManagerFactoryBean iisLocalEntityManager() {
		LocalContainerEntityManagerFactoryBean em
			= new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(iisLocalDataSource());
		em.setPackagesToScan(
			new String[]{"org.immregistries.iis.kernal.model.persisted"});

		HibernateJpaVendorAdapter vendorAdapter
			= new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2ddl.auto",
			env.getProperty("hibernate.hbm2ddl.auto"));
		properties.put("hibernate.dialect",
			env.getProperty("hibernate.dialect"));
		em.setJpaPropertyMap(properties);

		return em;
	}

	@Primary
	@Bean
	public DataSource iisLocalDataSource() {

		DriverManagerDataSource dataSource
			= new DriverManagerDataSource();
		dataSource.setDriverClassName(
			env.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(env.getProperty("iisLocal.jdbc.url"));
		dataSource.setUsername(env.getProperty("jdbc.user"));
		dataSource.setPassword(env.getProperty("jdbc.pass"));

		return dataSource;
	}

	@Primary
	@Bean
	public PlatformTransactionManager iisLocalTransactionManager() {

		JpaTransactionManager transactionManager
			= new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(
			iisLocalEntityManager().getObject());
		return transactionManager;
	}
}