package org.immregistries.iis.kernal.logic;


import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ExampleMessageWriter //extends IncomingMessageHandler
	implements IExampleMessageWriter {

	@Autowired
	protected RepositoryClientFactory repositoryClientFactory;
	@Autowired
	protected IncomingMessageHandler incomingMessageHandler;

	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	LocationMapper locationMapper;



}
