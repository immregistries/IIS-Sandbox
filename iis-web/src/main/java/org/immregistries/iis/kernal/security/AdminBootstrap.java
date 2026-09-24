package org.immregistries.iis.kernal.security;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.enums.UserRole;
import org.immregistries.iis.kernal.persisted.SecondDatabaseConfiguration;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.UserAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the administrator account at startup when the deployment asks for one.
 * <p>
 * Runs on ApplicationReadyEvent rather than the {@code @PostConstruct} used elsewhere in
 * this codebase for two reasons: {@code @PostConstruct} would force the JPA stack to be
 * instantiated early in a context that relies on {@code allow-circular-references}, and
 * {@code @Transactional} is silently ignored on an init method because the transaction
 * proxy is not applied to {@code this} yet. An {@code @EventListener} is invoked through
 * the bean proxy, so the transaction is real.
 * <p>
 * Idempotent: safe to run on every boot, which matters because hbm2ddl may be configured
 * to rebuild the schema.
 */
@Component
public class AdminBootstrap {

	/**
	 * Values that mean "nobody configured this", so no account should be created.
	 */
	private static final List<String> PLACEHOLDERS = List.of("change-me", "\"change-me\"");

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserAccessRepository userAccessRepository;

	@Value("${iis.deployment.admin.user:}")
	private String adminUser;
	@Value("${iis.deployment.admin.password:}")
	private String adminPassword;
	@Value("${iis.deployment.admin.create-if-not-exists:false}")
	private boolean createIfNotExists;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public void createAdminIfNotExists() {
		if (!createIfNotExists) {
			return;
		}
		if (isUnset(adminUser) || isUnset(adminPassword)) {
			logger.warn(
				"IIS_CREATE_ADMIN_IF_NOT_EXISTS is set but IIS_ADMIN_USER and IIS_ADMIN_PASSWORD are not both configured, no admin account created");
			return;
		}
		String accessName = adminUser.trim();

		Optional<UserAccess> existing = userAccessRepository.findByAccessName(accessName);
		if (existing.isPresent()) {
			UserAccess userAccess = existing.get();
			if (userAccess.hasRole(UserRole.ADMIN)) {
				logger.info("Admin user {} already exists", accessName);
				return;
			}
			/*
			 * Promote, but never touch an existing password.
			 */
			Set<UserRole> roles = new HashSet<>(userAccess.getRoles());
			roles.add(UserRole.ADMIN);
			userAccess.setRoles(roles);
			userAccessRepository.save(userAccess);
			logger.info("Granted ADMIN to existing user {}", accessName);
			return;
		}

		UserAccess userAccess = new UserAccess();
		userAccess.setAccessName(accessName);
		userAccess.setAccessKey(UserAccessUtil.hashPassword(adminPassword));
		userAccess.setEnabled(true);
		userAccess.setRoles(new HashSet<>(Set.of(UserRole.ADMIN, UserRole.USER)));
		userAccessRepository.save(userAccess);
		logger.info("Created admin user {}", accessName);
	}

	private boolean isUnset(String value) {
		return StringUtils.isBlank(value) || PLACEHOLDERS.contains(value.trim());
	}
}
