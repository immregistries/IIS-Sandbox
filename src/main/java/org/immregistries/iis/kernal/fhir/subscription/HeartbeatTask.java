package org.immregistries.iis.kernal.fhir.subscription;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimerTask;

public class HeartbeatTask extends TimerTask {
    private static SessionFactory factory;
    public static Session getDataSession() {
        if (factory == null) {
            factory = new AnnotationConfiguration().configure().buildSessionFactory();
        }
        return factory.openSession();
    }

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
                .setEventsSinceSubscriptionStart(0)
                .setTopic(subscription.getTopic());
        this.bundle = new Bundle(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
        this.bundle.addEntry().setResource(subscriptionStatus);
        this.client = new SubscriptionClientFactory(subscription).newGenericClient(subscription.getEndpoint());
//        this.client = Context.getCtx().newRestfulGenericClient(subscription.getEndpoint());
    }

    @Override
    public void run() {
        Session session = factory.openSession();
        logger.info(" Heartbeat task ran at {}", LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()),
                ZoneId.systemDefault()));
        this.subscriptionStatus.setEventsSinceSubscriptionStart(subscriptionStatus.getEventsSinceSubscriptionStart() + 1);
        if (this.subscriptionStatus.getEventsSinceSubscriptionStart() >= 3) {
            cancel();
        }
        /**
         * TODO fetch subscription in db, see if still active, if not cancel
         */
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

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}
