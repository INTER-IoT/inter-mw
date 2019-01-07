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
package eu.interiot.intermw.comm.arm;

import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.rabbitmq.QueueImpl;
import eu.interiot.intermw.comm.control.abstracts.AbstractControlComponent;
import eu.interiot.intermw.commons.exceptions.ErrorCode;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.ApiCallback;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Subscription;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.intermw.commons.requests.SubscribeReq;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A default {@link ApiRequestManager} implementation
 * <p>
 * Usage:
 * Application registers itself within intermw by presenting its client object (serving as an id)
 * --> intermw creates an ApiCallback for this client object and associates these two together by calling registerCallback()
 * <p>
 * Application sends a message downstream, and includes its client object
 * --> entry into intermw is via the call(client,message) method
 * --> intermw associates message's conversationId with the client by putting the association into the conversationClientMap map
 * <p>
 * <p>
 * <p>
 * Application unregisters itself within intermw
 * --> intermw dissasociates the client object	from the ApiCallback by calling unregisterCallback()
 * --> all conversationId's, associated with the client, are removed from the conversationClientMap map by using the removeConversationIdClient() method
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 */
@eu.interiot.intermw.comm.arm.annotations.ApiRequestManager
public class DefaultApiRequestManager extends AbstractControlComponent implements ApiRequestManager {

    private final static Logger logger = LoggerFactory.getLogger(DefaultApiRequestManager.class);
    private static QueueImpl queue;
    private static Map<String, Message> queryResponseMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, ApiCallback<Message>> clientCallbacks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> conversationClientMap = Collections.synchronizedMap(new HashMap<>());
    private Publisher<Message> publisherPRM;
    private ParliamentRegistry registry;
    private Configuration configuration;

    /**
     * Map of platformId -> metadata of last message received from the platform
     */
    private Map<String, MessageMetadata> lastMessageMetadataMap = new HashMap<>();

    /**
     * Constructor for the DefaultApiRequestManager
     *
     * @param configuration Configuration for this instance of DefaultApiRequestManager
     */
    public DefaultApiRequestManager(Configuration configuration) throws MiddlewareException {
        logger.debug("DefaultApiRequestManager is initializing...");
        this.configuration = configuration;
        publisherPRM = getPublisher(BrokerTopics.ARM_PRM.getTopicName(), Message.class);
        setUpListeners();
        queue = new QueueImpl();
        registry = new ParliamentRegistry(configuration);
        restoreState();
        logger.debug("DefaultApiRequestManager has been initialized successfully.");
    }

    private void setUpListeners() throws MiddlewareException {
        subscribe(BrokerTopics.PRM_ARM.getTopicName(), message -> {
            MessageMetadata metadata = message.getMetadata();
            logger.debug("Received upstream message from the PRM of type {} with ID {} and conversationId {}.",
                    metadata.getMessageTypes(), metadata.getMessageID().get(), metadata.getConversationId().get());
            try {
                handleFromPRM(message);

            } catch (Exception e) {
                String description = String.format("ARM failed to handle upstream message %s of type %s received from PRM.",
                        metadata.getMessageID().orElse("N/A"), metadata.getMessageTypes());
                logger.error(description, e);
                getErrorReporter().sendErrorResponseMessage(message, e, description,
                        ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE);
            }
        }, Message.class);
    }

    public Message getQueryResponseMessage(String conversationId) {
        return queryResponseMap.get(conversationId);
    }

    private void handleFromPRM(Message message) {
        MessageMetadata metadata = message.getMetadata();
        String messageID = metadata.getMessageID().get();
        String conversationId = metadata.getConversationId().orElse(null);
        Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypes = metadata.getMessageTypes();
        logger.debug("Processing message {}...", messageID);

        if ((metadata.asPlatformMessageMetadata().getSenderPlatformId().isPresent())) {
            String senderPlatformId = metadata.asPlatformMessageMetadata().getSenderPlatformId().get().toString();
            lastMessageMetadataMap.put(senderPlatformId, metadata);
        }

        if (Collections.disjoint(messageTypes,
                EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.OBSERVATION, MessageTypesEnum.ERROR))) {
            logger.warn("Unexpected upstream message {} received from PRM of type {}. Message will be dropped.",
                    messageID, messageTypes);
            return;
        }

        // save query messages to static map for faking synchronous responses
        if (messageTypes.contains(MessageTypesEnum.QUERY)) {
            queryResponseMap.put(conversationId, message);
            // we drop these messages from the queue
            return;
        }

        String clientId = conversationClientMap.get(conversationId);
        if (clientId == null) {
            logger.warn("No client is registered for conversation {}. Message {} will be dropped.", conversationId, messageID);
            return;

        } else {
            logger.debug("Message will be routed to the client {}.", clientId);
        }

        ApiCallback<Message> callback = clientCallbacks.get(clientId);
        if (callback == null) {
            logger.warn("No callback is registered for client {}. Message will be dropped.", clientId);
        } else {
            try {
                callback.handle(message);
                logger.debug("Callback has been executed successfully.");
            } catch (Exception e) {
                logger.error("Failed to handle response message, error encountered while executing callback: " + e.getMessage(), e);
            }
        }

        if (Collections.disjoint(messageTypes,
                EnumSet.of(MessageTypesEnum.SUBSCRIBE, MessageTypesEnum.OBSERVATION))) {
            conversationClientMap.remove(conversationId);
            logger.debug("Conversation {} has been removed.", conversationId);
        }
    }

    /**
     * Entry function into the Comm layer (from the outside world)
     *
     * @param message
     * @return Returns a unique conversation (session) identifier that can be used by
     * the client to relate answers (callbacks) with the respective
     * request
     * @throws ???
     */
    public String processDownstream(Message message) throws MiddlewareException {

        if (!message.getMetadata().getConversationId().isPresent()) {
            throw new MiddlewareException("ConversationId is missing.");
        }
        String conversationId = message.getMetadata().getConversationId().get();
        Set<MessageTypesEnum> messageTypes = message.getMetadata().getMessageTypes();
        String clientId = message.getMetadata().getClientId().orElse(null);

        logger.debug("Processing message with ID {} and conversationId {} of type {} from client {}...",
                message.getMetadata().getMessageID().get(), conversationId, messageTypes, clientId);

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Message content:\n{}", message.serializeToJSONLD());
            }

            if (messageTypes.contains(MessageTypesEnum.UNSUBSCRIBE)) {
                String subscriptionID = message.getMetadata().asPlatformMessageMetadata().getSubscriptionId().get();
                conversationClientMap.remove(subscriptionID);
                logger.debug("Subscription ID {} has been removed from conversations registry.", subscriptionID);
            }

            if (clientId != null) {
                conversationClientMap.put(conversationId, clientId);
            }

            publisherPRM.publish(message);

            return conversationId;

        } catch (Exception e) {
            throw new MiddlewareException(String.format(
                    "Failed to process message %s of type %s from the client %s.",
                    message.getMetadata().getMessageID().get(), messageTypes, clientId), e);
        }
    }

    @Override
    public synchronized void registerCallback(String clientId, ApiCallback<Message> apiCallback) {
        clientCallbacks.put(clientId, apiCallback);
    }

    @Override
    public synchronized void updateCallback(Client client) throws MiddlewareException {
        if (!clientCallbacks.containsKey(client.getClientId())) {
            throw new MiddlewareException(String.format("No ApiCallback is registered for the client %s.", client.getClientId()));
        }

        ApiCallback<Message> apiCallback = clientCallbacks.get(client.getClientId());
        try {
            apiCallback.update(client);
        } catch (Exception e) {
            throw new MiddlewareException(e, "Failed to update callback for the client %s.",
                    client.getClientId());
        }
    }

    @Override
    public synchronized void unregisterCallback(String clientId) {
        Iterator it = conversationClientMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getValue().equals(clientId)) {
                it.remove();
            }
        }
        if (clientCallbacks.containsKey(clientId)) {
            clientCallbacks.get(clientId).stop();
            clientCallbacks.remove(clientId);
        }
    }

    public MessageMetadata getLastMessageMetadataInfo(String platformId) {
        return lastMessageMetadataMap.get(platformId);
    }

    private void restoreState() throws MiddlewareException {
        logger.debug("Restoring ARM state...");
        List<Client> clients = registry.listClients();
        if (clients.isEmpty()) {
            logger.debug("No clients found.");

        } else {
            logger.debug("Restoring client callbacks...");
            for (Client client : clients) {
                logger.debug("Restoring callback for client {}...", client.getClientId());
                try {
                    if (client.getResponseDelivery() == null) {
                        logger.warn("Callback for client {} cannot be restored.", client.getClientId());

                    } else {
                        ApiCallback<Message> apiCallback;
                        switch (client.getResponseDelivery()) {
                            case CLIENT_PULL:
                                apiCallback = new RabbitMQApiCallback(client.getClientId(), queue);
                                break;
                            case SERVER_PUSH:
                                apiCallback = new HttpPushApiCallback(client, queue, configuration);
                                break;
                            default:
                                throw new AssertionError("Unexpected response delivery: " + client.getResponseDelivery());
                        }
                        registerCallback(client.getClientId(), apiCallback);
                    }
                } catch (Exception e) {
                    throw new MiddlewareException(String.format("Failed to restore callback for client %s.",
                            client.getClientId()), e);
                }
            }
        }

        List<Subscription> subscriptions = registry.listSubcriptions();
        if (subscriptions.isEmpty()) {
            logger.debug("No subscriptions found.");

        } else {
            logger.debug("Restoring subscriptions...");
            for (Subscription subscription : subscriptions) {
                try {
                    restoreSubscriptions(subscription);
                } catch (MiddlewareException e) {
                    logger.error(String.format("Failed to restore subscription %s: %s",
                            subscription.getConversationId(), e.getMessage()), e);
                }
            }
        }

        logger.debug("ARM state has been restored successfully.");
    }

    private void restoreSubscriptions(Subscription subscription) throws MiddlewareException {
        logger.debug("Restoring subscription {} for client {}...", subscription.getConversationId(), subscription.getClientId());
        List<IoTDevice> devices = registry.getDevices(subscription.getDeviceIds());
        if (devices.isEmpty()) {
            throw new MiddlewareException(String.format("No devices found corresponding to subscription %s.", subscription.getConversationId()));
        }

        Map<String, List<IoTDevice>> devicesPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!devicesPerPlatform.containsKey(device.getHostedBy())) {
                devicesPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            devicesPerPlatform.get(device.getHostedBy()).add(device);
        }

        for (String platformId : devicesPerPlatform.keySet()) {
            SubscribeReq req = new SubscribeReq(subscription.getConversationId(), subscription.getClientId(),
                    platformId, devicesPerPlatform.get(platformId));
            Message message = req.toMessage();
            message.getMetadata().addMessageType(MessageTypesEnum.SYS_INIT);
            processDownstream(message);
        }
    }
}
