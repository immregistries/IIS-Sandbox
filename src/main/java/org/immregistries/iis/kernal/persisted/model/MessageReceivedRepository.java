package org.immregistries.iis.kernal.persisted.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReceivedRepository extends JpaRepository<MessageReceived, Integer> {
}
