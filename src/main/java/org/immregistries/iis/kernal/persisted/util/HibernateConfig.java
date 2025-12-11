package org.immregistries.iis.kernal.persisted.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.immregistries.iis.kernal.persisted.SecondDatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the Users and tenant database, outside of spring, converted form
 * old .cfg.xml file
 */
public class HibernateConfig {
	private static Logger logger = LoggerFactory.getLogger(HibernateConfig.class);

	public static SessionFactory sessionFactory() {
		return SecondDatabaseConfiguration.getSessionFactory();
	}

	public static Session getDataSession() {
		return sessionFactory().openSession();
	}
}
