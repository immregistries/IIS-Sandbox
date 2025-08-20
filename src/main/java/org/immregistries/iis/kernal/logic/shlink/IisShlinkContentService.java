package org.immregistries.iis.kernal.logic.shlink;

import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.persisted.IisShlinkContent;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.springframework.stereotype.Service;

@Service
public class IisShlinkContentService {

	public IisShlinkContent getContent(String contentId, UserAccess userAccess) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery(
				"from IisShlinkContent where userAccess = :user and id = :id", IisShlinkContent.class);
			query.setParameter("user", userAccess.getUserAccessId());
			query.setParameter("id", contentId);
			return (IisShlinkContent) query.getSingleResult();
		}
	}

	public void saveIisShlinkContent(IisShlinkContent iisShlinkContent) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(iisShlinkContent);
			transaction.commit();
		}
	}
}
