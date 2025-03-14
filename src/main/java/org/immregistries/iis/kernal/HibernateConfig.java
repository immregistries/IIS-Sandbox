package org.immregistries.iis.kernal;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the Users and tenant database
 */
public class HibernateConfig {
	static Logger logger = LoggerFactory.getLogger(HibernateConfig.class);

	public static Configuration configuration() {
		Configuration cfg = new Configuration();
		/*
		 * For deploying time configuration with ENV variable
		 */
		String database_url = getSystemVariableFromEnvOrProperty("IIS_MYSQL_URL");
		String database_user = getSystemVariableFromEnvOrProperty("ENV_IIS_MYSQL_USER");
		String database_password = getSystemVariableFromEnvOrProperty("ENV_IIS_MYSQL_PASSWORD");
		if (StringUtils.isNotBlank(database_url)) {
			cfg.setProperty("hibernate.connection.url", database_url);
			if (database_url.startsWith("jdbc:h2:")) {
				cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
				cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			} else {
				cfg.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
				cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
				cfg.setProperty("hibernate.ddl-auto", "create");
			}
		}
		if (StringUtils.isNotBlank(database_user)) {
			cfg.setProperty("hibernate.connection.username", database_user);
		}
		if (StringUtils.isNotBlank(database_password)) {
			cfg.setProperty("hibernate.connection.password", database_password);
		}

		cfg.setProperty("show_sql", "false");
		cfg.setProperty("hibernate.c3p0.acquire_increment", "1");
		cfg.setProperty("hibernate.c3p0.idle_test_period", "100");
		cfg.setProperty("hibernate.c3p0.timeout", "100");
		cfg.setProperty("hibernate.c3p0.max_size", "30");
		cfg.setProperty("hibernate.c3p0.min_size", "5");
		cfg.setProperty("hibernate.c3p0.max_statements", "10");
		cfg.setProperty("hibernate.enable_lazy_load_no_trans", "true");

		cfg.addResource("org/immregistries/iis/kernal/model/Tenant.hbm.xml");
		cfg.addResource("org/immregistries/iis/kernal/model/UserAccess.hbm.xml");
		cfg.addResource("org/immregistries/iis/kernal/model/MessageReceived.hbm.xml");
		return cfg;
	}

	private static String getSystemVariableFromEnvOrProperty(String variableName) {
		String database_url = System.getenv(variableName);
		if (StringUtils.isBlank(database_url)) {
			database_url = System.getProperty(variableName);
		}
		return database_url;
	}

	public static SessionFactory sessionFactory() {
		Configuration configuration = configuration();
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
			.applySettings(configuration.getProperties()).build();
		SessionFactory sessionFactory = configuration().buildSessionFactory(serviceRegistry);
		return sessionFactory;
	}
}
