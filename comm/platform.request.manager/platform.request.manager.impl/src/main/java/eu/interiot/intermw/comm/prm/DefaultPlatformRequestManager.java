/*
 * Copyright 2016-2018 Universitat Politècnica de València
 * Copyright 2016-2018 Università della Calabria
 * Copyright 2016-2018 Prodevelop, SL
 * Copyright 2016-2018 Technische Universiteit Eindhoven
 * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
 * Investigación, Promoción y Estudios Comerciales de Valenciaport
 * Copyright 2016-2018 Rinicom Ltd
 * Copyright 2016-2018 Association pour le développement de la formation
 * professionnelle dans le transport
 * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
 * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
 * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
 * Copyright 2016-2018 Azienda Sanitaria Locale TO5
 * Copyright 2016-2018 Alessandro Bassi Consulting SARL
 * Copyright 2016-2018 Neways Technologies B.V.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.interiot.intermw.comm.prm;

import eu.interiot.intermw.bridge.BridgeContext;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.control.abstracts.AbstractControlComponent;
import eu.interiot.intermw.commons.exceptions.ErrorCode;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Plat2PlatSubscription;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.model.QueryReturn;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.intermw.commons.model.enums.QueryType;
import eu.interiot.intermw.commons.requests.*;
import eu.interiot.intermw.commons.responses.DeviceRemoveRes;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerINTERMW;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.metadata.QueryMessageMetadata;
import eu.interiot.message.payload.GOIoTPPayload;
import eu.interiot.message.payload.types.ObservationPayload;
import eu.interiot.message.utils.MessageUtils;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.RDFOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum.*;

/*
 * The Platform Request Manager prepares and sends requests to specific platforms through bridges, using already established
 * 	permanent data streams, which it creates during startup with the help of Data Flow Manager, or it creates new data streams.
 * All data streams that go south, from the Platform Request Manager to bridges, go through permanent data streams,
 * 	which can be either routed through IPSM (when needing ontological and/or semantical translation, as decided by consulting
 * 	Platform Registry & Capabilities), or bypassing it and connecting directly to the bridges (thus eliminating the overhead).
 * All data streams that go north, from the bridges to the Platform Request Manager, need to be created as needed.
 * Platform Request Manager is during request pre-processing potentially assisted by some middleware services,
 * 	such as routing or the device registry. It sends requests to underlying platforms as/when needed.
 */

/**
 * Created by flavio_fuart on 19-Dec-16. Edited by Matevz Markovic
 * (matevz.markovic@xlab.si)
 */
@eu.interiot.intermw.comm.prm.annotations.PlatformRequestManager
public class DefaultPlatformRequestManager extends AbstractControlComponent implements PlatformRequestManager {

    private final static Logger logger = LoggerFactory.getLogger(DefaultPlatformRequestManager.class);
    private Publisher<Message> publisherIPSMRM;
    private Publisher<Message> publisherARM;
    private ParliamentRegistry registry;
    /**
     * Conversation ID to Plat2PlatSubscription objects mapping used for routing observation messages originating from plat-to-plat subscriptions
     */
    private Map<String, Plat2PlatSubscription> plat2PlatSubscriptionsRoutingMap = new HashMap<>();

    /**
     * Conversation ID of UNSUBSCRIBE message to subscription ID mapping for platform-to-platform subscriptions.
     */
    private Map<String, String> plat2PlatUnsubscribeConversationIdMap = new HashMap<>();

    /**
     * @param configuration The configuration for this platform request manager
     */
    public DefaultPlatformRequestManager(Configuration configuration) throws MiddlewareException {
        super();
        logger.debug("DefaultPlatformRequestManager is initializing...");
        registry = initParliamentRegistry(configuration);
        publisherIPSMRM = getPublisher(BrokerTopics.PRM_IPSMRM.getTopicName(), Message.class);
        publisherARM = getPublisher(BrokerTopics.PRM_ARM.getTopicName(), Message.class);
        setUpListeners();
        restoreState();
        logger.debug("DefaultPlatformRequestManager has been initialized successfully.");
    }

    ParliamentRegistry initParliamentRegistry(Configuration configuration) {
        return new ParliamentRegistry(configuration);
    }

    private void setUpListeners() throws MiddlewareException {
        logger.debug("Setting up PRM listeners...");

        subscribe(BrokerTopics.IPSMRM_PRM.getTopicName(), message -> {
            MessageMetadata metadata = message.getMetadata();
            logger.debug("Received message from the queue {} of type {} with id {} and conversationId {}.",
                    BrokerTopics.IPSMRM_PRM.getTopicName(), metadata.getMessageTypes(), metadata.getMessageID().get(),
                    metadata.getConversationId().get());

            try {
                handleFromIPSMRM(message);

            } catch (Exception e) {
                String description = String.format("PRM failed to handle upstream message %s of type %s received from IPSMRM.",
                        metadata.getMessageID().orElse("N/A"), metadata.getMessageTypes());
                logger.error(description, e);
                getErrorReporter().sendErrorResponseMessage(message, e, description,
                        ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
            }
        }, Message.class);

        subscribe(BrokerTopics.ARM_PRM.getTopicName(), message -> {
            MessageMetadata metadata = message.getMetadata();
            logger.debug("Received message from the queue {} of type {} with id {} and conversationId {}.",
                    BrokerTopics.ARM_PRM.getTopicName(), metadata.getMessageTypes(), metadata.getMessageID().get(),
                    metadata.getConversationId().get());

            try {
                handleFromARM(message);

            } catch (Exception e) {
                String description = String.format("PRM failed to handle downstream message %s of type %s received from ARM.",
                        metadata.getMessageID().orElse("N/A"), metadata.getMessageTypes());
                logger.error(description, e);
                getErrorReporter().sendErrorResponseMessage(message, e, description,
                        ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
            }
        }, Message.class);

        logger.debug("Listeners have been set up successfully.");
    }

    /**
     * Handle message coming from ARM going downstream
     *
     * @param message Action to be performed
     * @return If action is subscription, unique flow id is created for the
     * execution of this action
     * @throws ???
     */
    public void handleFromARM(Message message) throws MiddlewareException {
        MessageMetadata metadata = message.getMetadata();
        String conversationId = metadata.getConversationId().orElse(null);
        Set<MessageTypesEnum> messageTypes = metadata.getMessageTypes();

        logger.debug("Processing downstream message coming from ARM with ID {} and conversationId {} of type {}...",
                metadata.getMessageID().get(), conversationId, messageTypes);

        if (messageTypes.contains(MessageTypesEnum.PLATFORM_REGISTER)) {

            RegisterPlatformReq registerPlatformReq = new RegisterPlatformReq(message);
            registerPlatform(registerPlatformReq);
            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE)) {

            UpdatePlatformReq updatePlatformReq = new UpdatePlatformReq(message);
            updatePlatform(updatePlatformReq);
            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_CREATE_DEVICE)) {

            PlatformCreateDeviceReq req = new PlatformCreateDeviceReq(message);
            platformCreateDevice(req);
            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE_DEVICE)) {

            PlatformUpdateDeviceReq req = new PlatformUpdateDeviceReq(message);
            platformUpdateDevice(req);
            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_DELETE_DEVICE)) {

            PlatformDeleteDeviceReq req = new PlatformDeleteDeviceReq(message);
            platformDeleteDevice(req);
            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.SUBSCRIBE)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.UNSUBSCRIBE)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.OBSERVATION)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.VIRTUAL_SUBSCRIBE)) {

            Plat2PlatSubscribeReq req = new Plat2PlatSubscribeReq(message);
            subscribePlat2Plat(req);

        } else if (messageTypes.contains(MessageTypesEnum.VIRTUAL_UNSUBSCRIBE)) {

            Plat2PlatUnsubscribeReq req = new Plat2PlatUnsubscribeReq(message);
            unsubscribePlat2Plat(req);

        } else if (messageTypes.contains(MessageTypesEnum.LIST_DEVICES)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.ACTUATION)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.LIST_SUPPORTED_PLATFORM_TYPES)) {

            Message responseMsg = listPlatformTypes(message);
            publisherARM.publish(responseMsg);

        } else if (messageTypes.contains(QUERY)) {

            publisherIPSMRM.publish(message);

        } else if (messageTypes.contains(MessageTypesEnum.DEVICE_DISCOVERY_QUERY)) {
            Message responseMsg = discoveryQuery(message);
            publisherARM.publish(responseMsg);
        } else {
            throw new MiddlewareException(String.format("Unexpected message type: %s.", messageTypes));
        }

        logger.debug("Message {} has been processed successfully.",
                metadata.getMessageID().get(), conversationId, messageTypes);
    }

    private void registerPlatform(RegisterPlatformReq registerPlatformReq) throws MiddlewareException {
        Platform platform = registerPlatformReq.getPlatform();
        try {
            logger.debug("Creating bridge for the platform {}...", platform.getPlatformId());
            BridgeContext.createBridge(platform);
            getRegistry().registerPlatform(platform);
        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to create bridge for the platform %s.",
                    platform.getPlatformId()), e);
        }
    }

    private void restorePlatform(Platform platform) throws MiddlewareException {
        try {
            logger.debug("Creating bridge for the platform {}...", platform.getPlatformId());
            BridgeContext.createBridge(platform);
            RegisterPlatformReq req = new RegisterPlatformReq(platform);
            Message message = req.toMessage();
            message.getMetadata().addMessageType(MessageTypesEnum.SYS_INIT);
            publisherIPSMRM.publish(message);

        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to create bridge for the platform %s.",
                    platform.getPlatformId()), e);
        }
    }

    private void updatePlatform(UpdatePlatformReq updatePlatformReq) throws MiddlewareException {
        Platform platform = updatePlatformReq.getPlatform();
        try {
            getRegistry().updatePlatform(platform);

        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to update platform %s.",
                    platform.getPlatformId()), e);
        }
    }

    private void unregisterPlatform(String platformId) throws MiddlewareException {
        try {
            logger.debug("Removing bridge for the platform {}...", platformId);
            BridgeContext.removeBridge(platformId);
            getRegistry().removePlatform(platformId);

        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to remove bridge for the platform %s.",
                    platformId), e);
        }
    }

    private void platformCreateDevice(PlatformCreateDeviceReq req) throws MiddlewareException {
        getRegistry().registerDevices(req.getDevices());
    }

    private void platformUpdateDevice(PlatformUpdateDeviceReq req) throws MiddlewareException {
        getRegistry().updateDevices(req.getDevices());
    }

    private void platformDeleteDevice(PlatformDeleteDeviceReq req) throws MiddlewareException {
        getRegistry().removeDevices(req.getDeviceIds());
    }

    private void subscribePlat2Plat(Plat2PlatSubscribeReq req) throws MiddlewareException {
        logger.debug("subscribePlat2Plat started.");
        Plat2PlatSubscription sub = new Plat2PlatSubscription();
        sub.setConversationId(req.getConversationId());
        sub.setClientId(req.getClientId());
        sub.setTargetDeviceId(req.getTargetDeviceId());
        sub.setTargetPlatformId(req.getTargetPlatformId());
        sub.setSourceDeviceId(req.getSourceDeviceId());
        sub.setSourcePlatformId(req.getSourcePlatformId());

        registry.addPlat2PlatSubscription(sub);
        logger.debug("Plat-to-plat subscription {} has been stored to the registry.", req.getConversationId());

        SubscribeReq subscribeReq = new SubscribeReq();
        subscribeReq.setClientId(req.getClientId());
        subscribeReq.setConversationId(req.getConversationId());
        subscribeReq.setSubscriptionId(req.getConversationId());
        subscribeReq.setPlatformId(req.getSourcePlatformId());
        subscribeReq.setDeviceIds(Collections.singletonList(req.getSourceDeviceId()));

        IoTDevice ioTDevice = new IoTDevice(req.getSourceDeviceId());
        ioTDevice.setHostedBy(req.getSourcePlatformId());
        subscribeReq.setDevices(Collections.singletonList(ioTDevice));

        publisherIPSMRM.publish(subscribeReq.toMessage());
        logger.debug("Published SUBSCRIBE request with conversationID {} to the platform {} for device {} relating to plat-to-plat subscription {}.",
                req.getConversationId(), req.getSourcePlatformId(), req.getSourceDeviceId(), req.getConversationId());

        plat2PlatSubscriptionsRoutingMap.put(req.getConversationId(), sub);
        logger.debug("Plat-to-plat subscription {} has been stored to the routing map.", req.getConversationId());
    }

    private void unsubscribePlat2Plat(Plat2PlatUnsubscribeReq req) throws MiddlewareException {
        String subscriptionId = req.getSubscriptionId();
        logger.debug("Canceling plat-to-plat subscription {}...", subscriptionId);

        Plat2PlatSubscription plat2PlatSubscription = registry.getPlat2PlatSubscription(subscriptionId);
        UnsubscribeReq unsubscribeReq = new UnsubscribeReq();
        unsubscribeReq.setClientId(req.getClientId());
        unsubscribeReq.setConversationId(req.getConversationId());
        unsubscribeReq.setSubscriptionId(subscriptionId);
        unsubscribeReq.setPlatformId(plat2PlatSubscription.getSourcePlatformId());
        unsubscribeReq.setDeviceIds(Collections.singletonList(plat2PlatSubscription.getSourceDeviceId()));

        publisherIPSMRM.publish(unsubscribeReq.toMessage());
        plat2PlatUnsubscribeConversationIdMap.put(unsubscribeReq.getConversationId(), subscriptionId);
        logger.debug("Published UNSUBSCRIBE request with conversationID {} to the platform {} for device {} relating to plat-to-plat subscription {}.",
                unsubscribeReq.getConversationId(), unsubscribeReq.getPlatformId(), plat2PlatSubscription.getSourceDeviceId(),
                subscriptionId);


        registry.deletePlat2PlatSubscription(subscriptionId);

        plat2PlatSubscriptionsRoutingMap.remove(subscriptionId);
        logger.debug("Plat-to-plat subscription {} has been removed from the routing map.", subscriptionId);

        logger.debug("Plat-to-plat subscription {} has been canceled successfuly.", subscriptionId);
    }

    private void restorePlat2PlatSubscriptions(List<Plat2PlatSubscription> subscriptions) {
        for (Plat2PlatSubscription sub : subscriptions) {
            plat2PlatSubscriptionsRoutingMap.put(sub.getConversationId(), sub);
            logger.debug("Plat-to-plat subscription {} has been restored.", sub.getConversationId());
        }
    }

    private Message listPlatformTypes(Message message) throws MiddlewareException {
        try {
            MessagePayload payload = new MessagePayload();

            Collection<String> supportedPlatformTypes = BridgeContext.getSupportedPlatformTypes();
            logger.debug("Supported platform types: {}", supportedPlatformTypes);
            EntityID entityID = new EntityID(URIManagerINTERMW.PREFIX.intermw + "platform-types");
            for (String platformType : supportedPlatformTypes) {
                payload.addTypeToEntity(entityID, new EntityID(platformType));
            }

            Message responseMessage = MessageUtils.createResponseMessage(message);
            responseMessage.setPayload(payload);
            return responseMessage;

        } catch (Exception e) {
            throw new MiddlewareException("Failed to list platform types.", e);
        }
    }

    private Message discoveryQuery(Message message) {

        QueryMessageMetadata queryMessageMetadata = message.getMetadata().asQueryMetadata();
        String queryString = queryMessageMetadata.getQueryString().get();

        QueryType queryType = QueryType.extractFromQuery(queryString);

        Message responseMessage = MessageUtils.createResponseMessage(message);

        QueryReturn queryReturn = getRegistry().executeQuery(queryString, queryType);
        if (queryReturn.getConstructResult() != null) {
            // we can put model directly in the message
            responseMessage.setPayload(new MessagePayload(queryReturn.getConstructResult()));
        } else if (queryReturn.getSelectResult() != null) {
            ResultSet selectResult = queryReturn.getSelectResult();
            Model model = RDFOutput.encodeAsModel(selectResult);
            responseMessage.setPayload(new MessagePayload(model));
        } else if (queryReturn.isAskResult() != null) {
            GOIoTPPayload payloadAsGOIoTPPayload = responseMessage.getPayloadAsGOIoTPPayload();
            payloadAsGOIoTPPayload.setDataPropertyAssertionForEntity(
                    new EntityID("http://inter-iot.eu/askResult"),
                    new PropertyID("rdf:value"),
                    queryReturn.isAskResult());
        }

        return responseMessage;
    }

    /**
     * Push IPSM-translated message upstream towards ARM or towards MW2MW (Resource discovery)
     * <p>
     * Based on sequence diagrams: MW02, MW03, MW04, MW08
     *
     * @param message represents IIOTHS{M1,Og}, where M is data and O is a ontology
     * @throws ???
     */
    public void handleFromIPSMRM(Message message) throws MiddlewareException {
        MessageMetadata metadata = message.getMetadata();
        String conversationId = metadata.getConversationId().orElse(null);
        Set<MessageTypesEnum> messageTypes = metadata.getMessageTypes();

        logger.debug("Processing message coming from IPSMRM going upstream with ID {} and conversationId {} of type {}...",
                metadata.getMessageID().get(), conversationId, messageTypes);

        if (messageTypes.contains(DEVICE_REGISTRY_INITIALIZE)
                || messageTypes.contains(DEVICE_ADD_OR_UPDATE)) {
            String platformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
            getRegistry().registerDevices(platformId, message);
        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER)) {
            String platformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
            unregisterPlatform(platformId);
            publisherARM.publish(message);

        } else if (messageTypes.contains(DEVICE_REMOVE)) {
            removeDevice(new DeviceRemoveRes(message));

        } else if (messageTypes.contains(LIST_DEVICES)) {
            // nothing to do

        } else if (messageTypes.contains(SUBSCRIBE) && plat2PlatSubscriptionsRoutingMap.containsKey(conversationId)) {
            sendPlat2PlatSubscribeResponse(message, conversationId);

        } else if (messageTypes.contains(UNSUBSCRIBE) && plat2PlatUnsubscribeConversationIdMap.containsKey(conversationId)) {
            sendPlat2PlatUnsubscribeResponse(message, conversationId);

        } else if (messageTypes.contains(OBSERVATION)) {
            handleObservationMessage(message, conversationId);

        } else {
            publisherARM.publish(message);
        }
    }

    private void removeDevice(DeviceRemoveRes response) throws MiddlewareException {
        List<String> deviceIds = Collections.singletonList(response.getDevice().getDeviceId());
        logger.debug("DeviceDiscovery - Removing " + deviceIds + " devices");
        getRegistry().removeDevices(deviceIds);
    }

    private void sendPlat2PlatSubscribeResponse(Message originalMessage, String conversationId) throws BrokerException {
        logger.debug("Received SUBSCRIBE response message from the platform {} corresponding to plat-to-plat subscription {}.",
                originalMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId(), conversationId);
        Message responseMsg = new Message();
        MessageMetadata respMsgMetadata = responseMsg.getMetadata();
        respMsgMetadata.addMessageType(MessageTypesEnum.VIRTUAL_SUBSCRIBE);
        respMsgMetadata.addMessageType(MessageTypesEnum.RESPONSE);
        respMsgMetadata.setConversationId(conversationId);
        publisherARM.publish(responseMsg);
    }

    private void sendPlat2PlatUnsubscribeResponse(Message originalMessage, String conversationId) throws BrokerException {
        String subscriptionId = plat2PlatUnsubscribeConversationIdMap.get(conversationId);
        logger.debug("Received UNSUBSCRIBE response message from the platform {} corresponding to plat-to-plat subscription {}.",
                originalMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId(), subscriptionId);
        Message responseMsg = new Message();
        MessageMetadata respMsgMetadata = responseMsg.getMetadata();
        respMsgMetadata.addMessageType(MessageTypesEnum.VIRTUAL_UNSUBSCRIBE);
        respMsgMetadata.addMessageType(MessageTypesEnum.RESPONSE);
        respMsgMetadata.setConversationId(conversationId);
        respMsgMetadata.asPlatformMessageMetadata().setSubscriptionId(subscriptionId);
        publisherARM.publish(responseMsg);
    }

    private void handleObservationMessage(Message message, String conversationId) throws MiddlewareException {
        if (plat2PlatSubscriptionsRoutingMap.containsKey(conversationId)) {
            MessageMetadata metadata = message.getMetadata();
            Plat2PlatSubscription sub = plat2PlatSubscriptionsRoutingMap.get(conversationId);
            logger.debug("Observation message corresponds to plat-to-plat subscription {}.", sub.getConversationId());
            String senderPlatformId = metadata.asPlatformMessageMetadata().getSenderPlatformId().isPresent() ?
                    metadata.asPlatformMessageMetadata().getSenderPlatformId().get().toString() : null;
            if (!sub.getSourcePlatformId().equals(senderPlatformId)) {
                throw new MiddlewareException("Unexpected sender platform ID: %s, doesn't match with plat-to-plat subscription %s.",
                        senderPlatformId, sub.getConversationId());
            }

            metadata.asPlatformMessageMetadata().setReceivingPlatformID(new EntityID(sub.getTargetPlatformId()));

            // set madeBySensor attributes in observation message to target device ID
            ObservationPayload observationPayload = message.getPayloadAsGOIoTPPayload().asObservationPayload();
            for (EntityID observationEntityID : observationPayload.getObservations()) {
                observationPayload.setMadeBySensor(observationEntityID, new EntityID(sub.getTargetDeviceId()));
            }

            publisherIPSMRM.publish(message);
            logger.debug("Observation message has been published to the platform {}.", sub.getTargetPlatformId());

        } else {
            publisherARM.publish(message);
        }
    }

    private void restoreState() throws MiddlewareException {
        logger.debug("Restoring PRM state...");
        List<Platform> platforms = getRegistry().listPlatforms();
        if (platforms.isEmpty()) {
            logger.debug("No platforms registered.");
        }
        for (Platform platform : platforms) {
            logger.debug("Restoring bridge for the platform {}...", platform.getPlatformId());
            try {
                restorePlatform(platform);

            } catch (Exception e) {
                throw new MiddlewareException(String.format("Failed to restore bridge for the platform %s.",
                        platform.getPlatformId()), e);
            }
        }

        logger.debug("Restoring plat-to-plat subscriptions...");
        List<Plat2PlatSubscription> plat2PlatSubscriptions = registry.listPlat2PlatSubscriptions();
        if (plat2PlatSubscriptions.isEmpty()) {
            logger.debug("No plat-to-plat subscriptions registered.");
        } else {
            restorePlat2PlatSubscriptions(plat2PlatSubscriptions);
        }

        logger.debug("PRM state has been restored successfully.");
    }

    public ParliamentRegistry getRegistry() {
        return registry;
    }
}
