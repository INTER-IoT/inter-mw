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
package eu.interiot.intermw.performancetest;

import eu.interiot.intermw.bridge.BridgeConfiguration;
import eu.interiot.intermw.bridge.test.MWTestBridge;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.InvalidMessageException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.requests.SubscribeReq;
import eu.interiot.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "http://inter-iot.eu/MWPerformanceTestPlatform")
public class MWPerformanceTestBridge extends MWTestBridge {
    private static int messageGenerationInterval = 5000;
    private static long generatedMessageCounter = 0;

    private final Logger logger = LoggerFactory.getLogger(MWPerformanceTestBridge.class);

    public MWPerformanceTestBridge(BridgeConfiguration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
    }

    static void setMessageGenerationInterval(int interval) {
        messageGenerationInterval = interval;
    }

    static long getGeneratedMessageCounter() {
        return generatedMessageCounter;
    }

    @Override
    public Message subscribe(Message message) throws InvalidMessageException {
        SubscribeReq subscribeReq = new SubscribeReq(message);
        String conversationId = subscribeReq.getConversationId();
        ObservationDispatcher dispatcher = new ObservationDispatcher(subscribeReq.getDeviceIds(), conversationId);
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(dispatcher, 1000, messageGenerationInterval, TimeUnit.MILLISECONDS);
        subscriptionTaskMap.put(conversationId, scheduledFuture);
        logger.debug("Subscription {} has been scheduled.", conversationId);
        return createResponseMessage(message);
    }

    private class ObservationDispatcher implements Runnable {
        private List<String> deviceIds;
        private String conversationId;

        ObservationDispatcher(List<String> deviceIds, String conversationId) {
            this.deviceIds = deviceIds;
            this.conversationId = conversationId;
        }

        @Override
        public void run() {
            logger.debug("Sending observation messages for subscription {}...", conversationId);

            for (String deviceId : deviceIds) {
                Message observationMessage;
                observationMessage = createComplexObservationMessage(conversationId, deviceId, "10");
                try {
                    publisher.publish(observationMessage);
                    logger.debug("Dispatched complex observation message.");
                } catch (BrokerException e) {
                    logger.error("Failed to dispatch complex observation message: {}.", e);
                }

                synchronized (this) {
                    generatedMessageCounter++;
                }
            }
        }
    }
}
