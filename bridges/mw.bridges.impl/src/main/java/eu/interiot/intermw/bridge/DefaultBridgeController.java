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
package eu.interiot.intermw.bridge;

import eu.interiot.intermw.bridge.abstracts.AbstractBridgeController;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.model.Bridge;
import eu.interiot.intermw.comm.broker.Listener;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.Subscriber;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.ErrorReporter;
import eu.interiot.intermw.commons.exceptions.ActionException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Default implementation for {@link BridgeController}
 * <p>
 * It is supposed to handle communications with any platform type. Other
 * implementations can be added by annotating them with
 * {@link eu.interiot.intermw.bridge.annotations.BridgeController} and
 * indicating a concrete list of
 * {@link eu.interiot.intermw.bridge.annotations.BridgeController#platforms()}
 *
 * @author <a href="mailto:mllorente@prodevelop.es">Miguel A. Llorente</a>
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
@eu.interiot.intermw.bridge.annotations.BridgeController()
public class DefaultBridgeController extends AbstractBridgeController {

    private final static Logger logger = LoggerFactory.getLogger(DefaultBridgeController.class);

    private Publisher<Message> publisher;
    List<Subscriber<Message>> subscribers = new ArrayList<>();

    /**
     * The constructor. It creates a new instance of
     * {@link DefaultBridgeController}
     * <p>
     * Internally calls to the {@link #init(Bridge)} { and
     * {@link #setUpBrokerListeners()}
     *
     * @param bridge The {@link Bridge} instance
     * @throws BridgeException
     */
    public DefaultBridgeController(Platform platform, Bridge bridge) throws BridgeException {
        super(platform, bridge);
        try {
            logger.debug("Creating instance of DefaultBridgeController for platform {}...", platform.getPlatformId());
            init(bridge);
            setUpBrokerListeners();
            logger.debug("DefaultBridgeController has been instantiated successfully.");

        } catch (Exception e) {
            throw new BridgeException(String.format("Failed to create instance of DefaultBridgeController for the platform %s.",
                    platform.getPlatformId()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bridge getBridge() {
        return this.bridge;
    }

    /**
     * {@inheritDoc}
     *
     * @throws BrokerException
     */
    @Override
    public void setUpBrokerListeners() throws BridgeException, BrokerException {
        String topic = BrokerTopics.IPSMRM_BRIDGE.getTopicName(platform.getPlatformId());
        logger.info("Subscribing to topic {}...", topic);
        Subscriber<Message> ipsmrmBridgeSubscriber = broker.createSubscriber(topic, Message.class);

        ipsmrmBridgeSubscriber.subscribe(new Listener<Message>() {

            @Override
            public void handle(Message message) throws ActionException, MessageException {
                logger.debug("Received downstream message of type {} from IPSMRM with ID {} and conversationId {} for platform {}.",
                        message.getMetadata().getMessageTypes(),
                        message.getMetadata().getMessageID().orElse(null),
                        message.getMetadata().getConversationId().orElse(null),
                        platform.getPlatformId());

                try {
                    bridge.process(message);

                } catch (Exception e) {
                    Set<String> messageTypes = message.getMetadata().getMessageTypesAsStrings();
                    logger.error("Bridge failed to process message of type " + messageTypes + ": " + e.getMessage(), e);

                } catch (Error e) {
                    // serious error, e.g. NoClassDefFoundError
                    logger.error("Bridge failed: " + e.getMessage(), e);
                }
            }
        });

        subscribers.add(ipsmrmBridgeSubscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws BridgeException, BrokerException {
        logger.debug("Destroying DefaultBridgeController...");
        super.destroy();
        for (Subscriber<Message> subscriber : subscribers) {
            subscriber.cleanUp();
        }

        logger.debug("DefaultBridgeController has been destroyed successfully.");
    }

    /**
     * Initializes this {@link BridgeController} instance. This is additional to
     * {@link #setUpBrokerListeners()}
     *
     * @param bridge The {@link Bridge} instance
     * @throws BridgeException Thrown on any {@link Exception}
     */
    protected void init(Bridge bridge) throws BridgeException {
        try {
            publisher = getPublisher(BrokerTopics.BRIDGE_IPSMRM.getTopicName(platform.getPlatformId()));
            bridge.setPublisher(publisher);

            Publisher<Message> errorPublisher = getPublisher(BrokerTopics.ERROR.getTopicName());
            ErrorReporter errorReporter = new ErrorReporter(errorPublisher);
            bridge.setErrorReporter(errorReporter);

        } catch (Exception e) {
            throw new BridgeException(String.format("Failed to create publisher for topic BRIDGE_IPSMRM for the platform %s.", platform.getPlatformId()));
        }
    }

    public Publisher<Message> getPublisher() {
        return publisher;
    }
}
