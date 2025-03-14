package org.immregistries.iis.kernal;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Configures the Users and tenant database
 */
public class HibernateConfig {

	public static Configuration configuration() {
		Configuration cfg = new Configuration().configure();
		/*
		 * For deploying time configuration with ENV variable
		 */
		String database_url = System.getenv("IIS_MYSQL_URL");
		String database_user = System.getenv("ENV_IIS_MYSQL_USER");
		String database_password = System.getenv("ENV_IIS_MYSQL_PASSWORD");
		if (StringUtils.isNotBlank(database_url)) {
			cfg.setProperty("hibernate.connection.url", database_url);
			System.out.println(database_url);
			if (StringUtils.isBlank(cfg.getProperty("hibernate.dialect"))) {
				if (database_url.startsWith("jdbc:mysql:")) {
					cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
				} else if (database_url.startsWith("jdbc:h2:")) {
					cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
				}
			}
		}
		if (StringUtils.isNotBlank(database_user)) {
			cfg.setProperty("hibernate.connection.username", database_user);
		}
		if (StringUtils.isNotBlank(database_password)) {
			cfg.setProperty("hibernate.connection.password", database_password);
		}
		return cfg;
	}

	public static SessionFactory sessionFactory() {
		SessionFactory sessionFactory = configuration().buildSessionFactory();
		return sessionFactory;
	}
}
