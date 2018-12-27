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
package eu.interiot.intermw.comm.ipsm;

import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.control.abstracts.AbstractControlComponent;
import eu.interiot.intermw.commons.exceptions.ErrorCode;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.intermw.commons.requests.RegisterPlatformReq;
import eu.interiot.intermw.commons.requests.UpdatePlatformReq;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The IPSM Request Manager routes messages towards Sripas's or trivial IPSM for translation,
 * receives back translated messages and forwards them to bridges.
 * <p>
 * Created by matevz_markovic
 * (matevz.markovic@xlab.si)
 */
@eu.interiot.intermw.comm.ipsm.annotations.IPSMRequestManager
public class DefaultIPSMRequestManager extends AbstractControlComponent implements IPSMRequestManager {
    private final static Logger logger = LoggerFactory.getLogger(DefaultIPSMRequestManager.class);
    private final static String IPSM_BROKER_TYPE = "kafka";
    private Publisher<Message> publisherPRM;
    private Publisher<Message> publisherARM;
    private IPSMApiClient ipsmApiClient;
    private Map<String, IPSMRoutingInfo> ipsmRoutingTable;

    /**
     * @param configuration The configuration for this ipsm request manager
     */
    public DefaultIPSMRequestManager(Configuration configuration) throws MiddlewareException {
        super();
        logger.debug("DefaultIPSMRequestManager is initializing...");
        publisherPRM = getPublisher(BrokerTopics.IPSMRM_PRM.getTopicName(), Message.class);
        publisherARM = getPublisher(BrokerTopics.PRM_ARM.getTopicName(), Message.class);
        ipsmApiClient = new IPSMApiClient(configuration.getIPSMApiBaseUrl());
        setUpListeners();
        ipsmRoutingTable = new HashMap<>();
        logger.debug("DefaultIPSMRequestManager has been initialized successfully.");
    }

    private String getTopicIpsmrmToBridge(String platformId) {
        return BrokerTopics.IPSMRM_BRIDGE.getTopicName(platformId);
    }

    private String getTopicBridgeToIpsmrm(String platformId) {
        return BrokerTopics.BRIDGE_IPSMRM.getTopicName(platformId);
    }

    public static String getTopicToIPSMDownstream(String platforId) {
        return escapeTopicName("mw-ipsm-downstream-" + platforId);
    }

    public static String getTopicFromIPSMDownstream(String platformId) {
        return escapeTopicName("ipsm-mw-downstream-" + platformId);
    }

    public static String getTopicToIPSMUpstream(String platforId) {
        return escapeTopicName("mw-ipsm-upstream-" + platforId);
    }

    public static String getTopicFromIPSMUpstream(String platformId) {
        return escapeTopicName("ipsm-mw-upstream-" + platformId);
    }

    private static String escapeTopicName(String name) {
        return name.replaceAll("[:/#]+", "_");
    }

    /**
     * Handle message, coming from the PRM
     *
     * @param message Action to be performed
     * @return If action is subscription, unique flow id is created for the
     * execution of this action
     * @throws ???
     */
    private void handleFromPRM(Message message) throws MiddlewareException {
        MessageMetadata metadata = message.getMetadata();
        Set<MessageTypesEnum> messageTypes = metadata.getMessageTypes();
        logger.debug("Processing downstream message coming from PRM of type {} with ID {} and conversationId {}...",
                messageTypes, metadata.getMessageID().get(), metadata.getConversationId().get());

        String platformId = message.getMetadata().asPlatformMessageMetadata().getReceivingPlatformIDs().iterator().next().toString();

        if (messageTypes.contains(MessageTypesEnum.PLATFORM_REGISTER)) {

            RegisterPlatformReq req = new RegisterPlatformReq(message);
            registerPlatform(req);
            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE)) {

            UpdatePlatformReq req = new UpdatePlatformReq(message);
            updatePlatform(req);
            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.SUBSCRIBE)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.UNSUBSCRIBE)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.DEVICE_ADD_OR_UPDATE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.DEVICE_REMOVE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.DEVICE_REGISTRY_INITIALIZE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.LIST_DEVICES)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.DEVICE_DISCOVERY_QUERY)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.QUERY)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_CREATE_DEVICE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE_DEVICE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_DELETE_DEVICE)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.OBSERVATION)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.ACTUATION)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.ERROR)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_DISCOVERY_QUERY)) {

            publishToBridge(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.QUERY)) {

            publishToIPSMDownstream(message, platformId);

        } else if (messageTypes.contains(MessageTypesEnum.UNRECOGNIZED)) {

            publishToBridge(message, platformId);

        } else {

            throw new MiddlewareException(String.format("Unsupported message type: %s.", messageTypes));
        }
    }

    private void registerPlatform(RegisterPlatformReq registerPlatformReq) throws MiddlewareException {
        Platform platform = registerPlatformReq.getPlatform();
        String platformId = platform.getPlatformId();
        logger.debug("Registering platform '{}'...", platformId);

        IPSMRoutingInfo ipsmRoutingInfo = new IPSMRoutingInfo();
        ipsmRoutingTable.put(platformId, ipsmRoutingInfo);

        if (platform.getDownstreamInputAlignmentName().isEmpty() &&
                platform.getDownstreamOutputAlignmentName().isEmpty() &&
                platform.getUpstreamInputAlignmentName().isEmpty() &&
                platform.getUpstreamOutputAlignmentName().isEmpty()) {

            ipsmRoutingInfo.useIPSMDownstream = false;
            ipsmRoutingInfo.useIPSMUpstream = false;

        } else {
            ipsmApiClient.setupChannelsForPlatform(platform);
            setupIPSMPublishersSubscribers(platform);
        }

        // create publisher (RabbitMQ) for messages going downstream from IPSMRM to bridge
        String ipsmrmToBridgeTopic = getTopicIpsmrmToBridge(platformId);
        logger.debug("Creating publisher for queue {} (IPSMRM -> bridge) for the platform {}.", ipsmrmToBridgeTopic, platformId);
        getPublisher(ipsmrmToBridgeTopic, Message.class);

        // create subscriber (RabbitMQ) for messages going upstream from bridge to IPSMRM
        String bridgeToIpsmrmTopic = getTopicBridgeToIpsmrm(platformId);
        logger.debug("Creating subscriber for queue {} (bridge -> IPSMRM) for the platform {}.", bridgeToIpsmrmTopic, platformId);
        subscribe(bridgeToIpsmrmTopic, message -> {
            try {
                logger.debug("Received upstream message of type {} from the bridge (from queue {}) with ID {} and conversationId {} from platform {}.",
                        message.getMetadata().getMessageTypes(),
                        bridgeToIpsmrmTopic,
                        message.getMetadata().getMessageID().orElse(null),
                        message.getMetadata().getConversationId().orElse(null),
                        platformId);

                handleFromBridge(message, platformId);

            } catch (Exception e) {
                String description = String.format("IPSMRM failed to handle upstream message %s of type %s received from the bridge.",
                        message.getMetadata().getMessageID().orElse("N/A"), message.getMetadata().getMessageTypes());
                logger.error(description, e);
                getErrorReporter().sendErrorResponseMessage(message, e, description,
                        ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
            }
        }, Message.class);
    }

    private void setupIPSMPublishersSubscribers(Platform platform) throws MiddlewareException {
        String platformId = platform.getPlatformId();
        if (platform.getDownstreamInputAlignmentName().isEmpty() && platform.getDownstreamOutputAlignmentName().isEmpty()) {
            logger.debug("Downstream channel alignments are not given, there will be no IPSM translation for downstream messages.");
            ipsmRoutingTable.get(platformId).useIPSMDownstream = false;

        } else {
            logger.debug("Downstream channel alignments are given, IPSM translation for downstream messages is required.");
            ipsmRoutingTable.get(platformId).useIPSMDownstream = true;

            // create publisher for downstream messages going to IPSM
            String topicToIPSMDownstream = getTopicToIPSMDownstream(platformId);
            logger.debug("Creating IPSM Kafka publisher for platform {} on topic {}...", platformId, topicToIPSMDownstream);
            getPublisher(topicToIPSMDownstream, Message.class, IPSM_BROKER_TYPE);

            // create subscriber for downstream messages coming from IPSM
            String topicFromIPSMDownstream = getTopicFromIPSMDownstream(platformId);
            logger.debug("Creating IPSM Kafka subscriber for platform {} on topic {} for messages going downstream...",
                    platformId, topicFromIPSMDownstream);
            subscribe(topicFromIPSMDownstream, message -> {
                try {
                    logger.debug("Received downstream message from Kafka topic {} with ID {} and conversationId {} going to platform {}.",
                            topicFromIPSMDownstream,
                            message.getMetadata().getMessageID().orElse(null),
                            message.getMetadata().getConversationId().orElse(null),
                            platformId);

                    publishToBridge(message, platformId);

                } catch (Exception e) {
                    String description = String.format("IPSMRM failed to handle downstream message %s of type %s received from IPSM.",
                            message.getMetadata().getMessageID().orElse("N/A"), message.getMetadata().getMessageTypes());
                    logger.error(description, e);
                    getErrorReporter().sendErrorResponseMessage(message, e, description,
                            ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
                }
            }, Message.class, IPSM_BROKER_TYPE);
        }

        // upstream messages
        if (platform.getUpstreamInputAlignmentName().isEmpty() && platform.getUpstreamOutputAlignmentName().isEmpty()) {
            logger.debug("Upstream channel alignments are not given, there will be no IPSM translation for upstream messages.");
            ipsmRoutingTable.get(platformId).useIPSMUpstream = false;

        } else {
            logger.debug("Upstream channel alignments are given, IPSM translation for upstream messages is required.");
            ipsmRoutingTable.get(platformId).useIPSMUpstream = true;

            // create subscriber for upstream messages coming from IPSM
            String topicFromIPSMUpstream = getTopicFromIPSMUpstream(platformId);
            logger.debug("Creating IPSM Kafka subscriber for platform {} on topic {} for messages going upstream...",
                    platformId, topicFromIPSMUpstream);
            subscribe(topicFromIPSMUpstream, message -> {
                try {
                    logger.debug("Received upstream message from IPSM routing table topic {} with ID {} and conversationId {} coming from platform {}.",
                            topicFromIPSMUpstream,
                            message.getMetadata().getMessageID().orElse(null),
                            message.getMetadata().getConversationId().orElse(null),
                            platformId);

                    publisherPRM.publish(message);

                } catch (Exception e) {
                    String description = String.format("IPSMRM failed to handle upstream message %s of type %s received from IPSM.",
                            message.getMetadata().getMessageID().orElse("N/A"), message.getMetadata().getMessageTypes());
                    logger.error(description, e);
                    getErrorReporter().sendErrorResponseMessage(message, e, description,
                            ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
                }
            }, Message.class, IPSM_BROKER_TYPE);
        }
    }

    private void updatePlatform(UpdatePlatformReq req) throws MiddlewareException {
        Platform platform = req.getPlatform();
        String platformId = platform.getPlatformId();
        logger.debug("Updating IPSM configuration for platform {}...", platformId);

        if (!platform.getDownstreamInputAlignmentName().isEmpty() ||
                !platform.getDownstreamOutputAlignmentName().isEmpty() ||
                !platform.getUpstreamInputAlignmentName().isEmpty() ||
                !platform.getUpstreamOutputAlignmentName().isEmpty()) {

            ipsmApiClient.setupChannelsForPlatform(platform);
        }
    }

    private void unregisterPlatform(Message responseMessage) throws MiddlewareException {
        String platformId = responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
        logger.debug("Unregistering platform {}...", platformId);
        ipsmRoutingTable.remove(platformId);

        try {
            removePublisher(getTopicIpsmrmToBridge(platformId));
            unsubscribe(getTopicFromIPSMDownstream(platformId));
            unsubscribe(getTopicFromIPSMUpstream(platformId));
            unsubscribe(getTopicBridgeToIpsmrm(platformId));
            logger.debug("Platform {} has been unregistered successfully.", platformId);

        } catch (Exception e) {
            throw new MiddlewareException(String.format(
                    "Failed to remove publisher and subscriber for platform %s.", platformId), e);
        }
    }

    /**
     * Publish message to IPSM (message is going downstream)
     *
     * @param message
     * @throws MiddlewareException
     */
    private void publishToIPSMDownstream(Message message, String platformId) throws MiddlewareException {

        if (!ipsmRoutingTable.containsKey(platformId)) {
            throw new MiddlewareException(String.format("IPSM routing table doesn't contain any information about platform %s.", platformId));
        }

        if (ipsmRoutingTable.get(platformId).useIPSMDownstream) {
            String topic = getTopicToIPSMDownstream(platformId);
            logger.debug("Publishing message for platform {} to IPSM using {} broker on topic {}...",
                    platformId, IPSM_BROKER_TYPE, topic);
            getPublisher(topic, Message.class, IPSM_BROKER_TYPE).publish(message);

        } else {
            publishToBridge(message, platformId);
        }
    }

    /**
     * Publish message to IPSM (message is going upstream)
     *
     * @param message
     * @throws MiddlewareException
     */
    private void publishToIPSMUpstream(Message message) throws MiddlewareException {

        String platformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();

        if (ipsmRoutingTable.get(platformId).useIPSMUpstream) {
            String topic = getTopicToIPSMUpstream(platformId);
            logger.debug("Publishing message from platform {} to IPSM using {} broker on topic {}...",
                    platformId, IPSM_BROKER_TYPE, topic);
            getPublisher(topic, Message.class, IPSM_BROKER_TYPE).publish(message);

        } else {
            publisherPRM.publish(message);
        }
    }

    /**
     * Handle message, coming from a bridge
     *
     * @param message    Action to be performed
     * @param platformId Platform ID of the message
     * @return If action is subscription, unique flow id is created for the
     * execution of this action
     * @throws ???
     */
    private void handleFromBridge(Message message, String platformId) throws MiddlewareException {
        Set<MessageTypesEnum> messageTypes = message.getMetadata().getMessageTypes();

        if (messageTypes.contains(MessageTypesEnum.ERROR)) {
            publisherPRM.publish(message);

        } else if (!Collections.disjoint(messageTypes, EnumSet.of(
                MessageTypesEnum.OBSERVATION,
                MessageTypesEnum.QUERY,
                MessageTypesEnum.DEVICE_REGISTRY_INITIALIZE,
                MessageTypesEnum.DEVICE_ADD_OR_UPDATE
        ))) {
            logger.debug("Message will be forwarded to IPSM (if needed).");
            publishToIPSMUpstream(message);

        } else if (messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER)) {
            publisherPRM.publish(message);
            unregisterPlatform(message);

        } else {
            logger.debug("Message will be forwarded to PRM.");
            publisherPRM.publish(message);
        }
    }

    /**
     * Push the message (possibly translated by IPSM) towards the bridge with this platformId
     *
     * @throws MiddlewareException
     */
    private void publishToBridge(Message message, String platformId) throws MiddlewareException {
        logger.debug("Publishing message to bridge...");
        getPublisher(getTopicIpsmrmToBridge(platformId), Message.class).publish(message);
    }

    private void setUpListeners() throws MiddlewareException {
        subscribe(BrokerTopics.PRM_IPSMRM.getTopicName(), message -> {
            MessageMetadata metadata = message.getMetadata();
            logger.debug("Received downstream message of type {} from the queue {} with id {} and conversationId {}.",
                    metadata.getMessageTypes(), BrokerTopics.PRM_IPSMRM.getTopicName(),
                    metadata.getMessageID().orElse(null), metadata.getConversationId().orElse(null));

            try {
                handleFromPRM(message);

            } catch (Exception e) {
                String description = String.format("IPSMRM failed to handle downstream message %s of type %s received from PRM.",
                        message.getMetadata().getMessageID().orElse("N/A"), message.getMetadata().getMessageTypes());
                logger.error(description, e);
                getErrorReporter().sendErrorResponseMessage(message, e, description,
                        ErrorCode.ERROR_HANDLING_RECEIVED_MESSAGE, publisherARM);
            }
        }, Message.class);
    }

    private class IPSMRoutingInfo {
        public boolean useIPSMDownstream;
        public boolean useIPSMUpstream;
    }
}
