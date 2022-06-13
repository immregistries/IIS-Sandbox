package org.immregistries.iis.kernal.fhir.subscription;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.Context;
import org.immregistries.iis.kernal.fhir.client.SubscriptionClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimerTask;

public class HeartbeatTask extends TimerTask {

    private final Logger logger = LoggerFactory.getLogger(HeartbeatTask.class);

    private Subscription subscription;
    private IGenericClient client;
    private Bundle bundle;
    private SubscriptionStatus subscriptionStatus;

    public HeartbeatTask(Subscription subscription) {
        this.subscription = subscription;
        // TODO Check Bundle shape http://build.fhir.org/notification-heartbeat.json.html
        subscriptionStatus = new SubscriptionStatus()
                .setStatus(Enumerations.SubscriptionState.ACTIVE)
                .setType(SubscriptionStatus.SubscriptionNotificationType.HEARTBEAT)
                .setTopic(subscription.getTopic());
        this.bundle = new Bundle(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
        this.bundle.addEntry().setResource(subscriptionStatus);
        SubscriptionClientBuilder subscriptionClientBuilder = new SubscriptionClientBuilder(subscription);
        this.client = subscriptionClientBuilder.getClient();
//        this.client = Context.getCtx().newRestfulGenericClient(subscription.getEndpoint());
    }

    @Override
    public void run() {
        logger.info(" Heartbeat task ran at {}", LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()),
                ZoneId.systemDefault()));
//       this.client.create().resource(this.bundle).execute();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
}
