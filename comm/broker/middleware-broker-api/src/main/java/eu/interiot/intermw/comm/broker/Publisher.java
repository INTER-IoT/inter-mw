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
package eu.interiot.intermw.comm.broker;

import eu.interiot.intermw.comm.broker.exceptions.BrokerException;

import java.util.List;

/**
 * A {@link Service} for publishing messages in a message broker instance
 *
 * @param <M> The message class to be published
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Publisher<M> extends Service<M> {

    /**
     * Publishes a message
     *
     * @param message The message to be published
     * @throws BrokerException
     */
    public void publish(M message) throws BrokerException;

    /**
     * Initializes a publisher and creates the corresponding queues (only if the
     * message bus supports queue creation)
     *
     * When publishing a message it should be published on each queue
     *
     * @param broker     A {@link Broker} instance
     * @param queues     A list of {@link Queue}
     * @param topicClass The {@link Topic} class attached to this service
     * @throws BrokerException When something went wrong
     */
    public void init(Broker broker, List<Queue> queues, Class<Topic<M>> topicClass) throws BrokerException;

    /**
     * Initializes a publisher and creates the corresponding queues (only if the
     * message bus supports queue creation)
     *
     * When publishing a message it should be published on each queue
     *
     * @param broker       A {@link Broker} instance
     * @param queues       A list of {@link Queue}
     * @param exchangeName The name of the exchange where to publish the messages
     * @param messageClass The name of Class of the messages that are going to be
     *                     published
     * @throws BrokerException When something went wrong
     */
    public void init(Broker broker, List<Queue> queues, String exchangeName, Class<M> messageClass)
            throws BrokerException;
}
