package org.immregistries.iis.kernal.fhir.subscription;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.BundleBuilder;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.*;
//import org.hl7.fhir.r5.model.codesystems.SubscriptionChannelType;
import org.immregistries.iis.kernal.fhir.Context;
import org.immregistries.iis.kernal.fhir.client.SubscriptionClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;


public class SubscriptionProvider implements IResourceProvider {

    private final static int DEFAULT_HEARTBEAT_PERIOD = 60;
    private final static int MAX_HEARTBEAT_PERIOD = 3600;
    private final static int MIN_HEARTBEAT_PERIOD = 1;

    private final Logger logger = LoggerFactory.getLogger(SubscriptionProvider.class);



    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<org.hl7.fhir.r5.model.Subscription> getResourceType() {
        return org.hl7.fhir.r5.model.Subscription.class;
    }

    @Create()
    public org.hl7.fhir.r5.model.Subscription createSubscription(@ResourceParam org.hl7.fhir.r5.model.Subscription subscription) {
        boolean isValid;
        if (subscription.getHeartbeatPeriod() > MAX_HEARTBEAT_PERIOD || subscription.getHeartbeatPeriod() < MIN_HEARTBEAT_PERIOD){
            subscription.setHeartbeatPeriod(DEFAULT_HEARTBEAT_PERIOD);
        }
        /**
         * TODO
         * store/save active subscription
         *
         */
        if (subscription.getStatus().equals(Enumerations.SubscriptionState.REQUESTED)){
            subscription = handshake(subscription);
        } else if (subscription.getStatus().equals(Enumerations.SubscriptionState.ACTIVE)){
            // TODO check if already exists
        } else if (subscription.getStatus().equals(Enumerations.SubscriptionState.ENTEREDINERROR)){
            // TODO cancel subscription
        } else {
            throw new InvalidRequestException("SubscriptionState not accepted");
        }
        // TODO make all heartbeat timers accessible any time to cancel, static dictionnary with endpoint Index /  array with subId ?
        HeartbeatTask heartbeatTask = new HeartbeatTask(subscription);
        Timer heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(heartbeatTask,subscription.getHeartbeatPeriod()* 1000L,10000);
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(subscription.getHeartbeatPeriod()*1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        heartbeatTimer.cancel();

        return subscription;
    }

    public Subscription handshake(Subscription subscription) {
        BundleBuilder builder = new BundleBuilder(Context.getCtx());
        builder.addEntry();
        Bundle handshakeBundle = new Bundle().setType(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
        handshakeBundle.addEntry().setResource(
                new SubscriptionStatus()
                        .setType(SubscriptionStatus.SubscriptionNotificationType.HANDSHAKE)
                        .setEventsSinceSubscriptionStart(0)
                        .setTopic(subscription.getTopic())
        );
        IGenericClient client = new SubscriptionClientBuilder(subscription).getClient();
        IBaseBundle outcome = client.transaction().withBundle(handshakeBundle).execute();
        logger.info(outcome.toString());
        return subscription;
    }

    @Read()
    /**
     * TODO needs to register/specify :
     *
     * Url of EHR fhir server,
     * login and passwords ?
     *
     * events and triggers are supposedly specified in subcriptionTopic: still need to be defined in hard code in logic
     *
     * Payloads are operationOutcome with resources references specified in 'deprecated' field issue.location
     * OR issues are added by extension directly to the resource
     *
     */
    public org.hl7.fhir.r5.model.Subscription readSubscription() {
        OperationOutcome operationOutcome = new OperationOutcome();
        org.hl7.fhir.r5.model.Subscription sub = new Subscription();
//        Subscription.SubscriptionChannelComponent channel = new Subscription.SubscriptionChannelComponent()
//                .setType(Subscription.SubscriptionChannelType.RESTHOOK)
//                .setEndpoint(Server.serverBaseUrl)
//                .setPayload("application/json");
//        sub.setChannel(channel);
        sub.setReason("testing purposes");
        sub.setTopic("localhost:9091/ehr-sandbox/subscriptionTopic");
        sub.setChannelType(new Coding().setCode("RESTHOOK").setSystem("http://hl7.org/fhir/ValueSet/subscription-channel-type"));
//        sub.setEndpoint(self.serverUrl)
//        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        return sub;
    }


}
