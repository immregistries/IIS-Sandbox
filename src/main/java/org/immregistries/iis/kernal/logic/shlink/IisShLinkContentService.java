package org.immregistries.iis.kernal.logic.shlink;

import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.persisted.IisShLinkContent;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.shlink.ShLinkContentController;
import org.springframework.stereotype.Service;

@Service
public class IisShLinkContentService {

	public IisShLinkContent getContent(String contentId, UserAccess userAccess) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery(
				"from IisShLinkContent where userAccess = :user and id = :id", IisShLinkContent.class);
			query.setParameter("user", userAccess);
			query.setParameter("id", contentId);
			return (IisShLinkContent) query.getSingleResult();
		}
	}

	public IisShLinkContent getContent(String contentId) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery(
				"from IisShLinkContent where id = :id", IisShLinkContent.class);
			query.setParameter("id", contentId);
			return (IisShLinkContent) query.getSingleResult();
		}
	}

	public void saveIisShLinkContent(IisShLinkContent iisShLinkContent) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(iisShLinkContent);
			transaction.commit();
		}
	}

	public String getUrl(IisShLinkContent iisShLinkContent) {
		return ShLinkContentController.SHLINK_CONTENT_PATH + "/" + iisShLinkContent.getId();
	}
}
