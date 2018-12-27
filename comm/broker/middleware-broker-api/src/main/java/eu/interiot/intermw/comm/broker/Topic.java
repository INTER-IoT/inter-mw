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
import eu.interiot.intermw.comm.broker.exceptions.EmptyTopicException;

import java.util.List;

/**
 * An abstraction for configuring the topic exchange of a message broker
 *
 * @param <M> The message class this {@link Topic} is tied to
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Topic<M> {

    /**
     * FIXME DON'T USE THIS AT THE MOMENT
     *
     *
     * A list of routing or binding keys for this topic's message when it is
     * published
     *
     * Depending on the message bus implementation this routing keys can be
     * ignored
     *
     * The usual use of the publishing routing keys consists on assigning a key
     * to each message of a {@link Topic} so that they can be filtered by a
     * {@link Subscriber}
     *
     * Usually zero or one routing key should be provided. For more than one
     * routing key, the message will be published several times, each one with a
     * different routing key
     *
     * @return The list of publishing routing keys
     */
    public List<String> getPublishingRoutings();

    /**
     * FIXME DON'T USE THIS AT THE MOMENT
     *
     * A list of routing or binding keys for a subscriber of this topic's
     * messages
     *
     * Depending on the message bus implementation this routing keys can be
     * ignored
     *
     * The usual use of the subscribing routing keys consists on filtering
     * messages by their publishing routing key
     *
     * When no subscribing routings are provided, the {@link Subscriber} should
     * receive all the messages published
     *
     * When a single subscribing routing key is provided it is expected some
     * filtering
     *
     * When more than one subscribing routing key are provided it is expected to
     * create different subscribers, each one with a different subscribing
     * routing key
     *
     * @return The list of publishing routing keys
     */
    public List<String> getSubscribingRoutings();

    /**
     * An exchange is an entity in the middle of a publisher and a queue in some
     * message bus implementations
     *
     * @return
     */
    public String getExchangeName();

    /**
     * Serializes a message as a string
     *
     * @return The string
     * @throws EmptyTopicException
     */
    public String serialize() throws BrokerException;

    /**
     * Deserializes a string into a message instance
     *
     * Each call to this method creates a new instance of the message
     *
     * @param message The message instance as a string
     * @param type    The message type which is known at runtime
     * @return The message instance
     * @throws EmptyTopicException
     */
    public M deserialize(String message, Class<M> type) throws BrokerException;

    /**
     * The message instance
     *
     * @return
     * @throws EmptyTopicException
     */
    public M getMessage() throws EmptyTopicException;

    /**
     * Sets the message instance
     *
     * @return
     * @throws EmptyTopicException
     */
    public void setMessage(M message) throws EmptyTopicException;

    /**
     * The {@link Broker} instance
     *
     * @return
     */
    public Broker getBroker();

    /**
     * The message class this {@link Topic} is tied to
     *
     * @return
     */
    public Class<M> getMessageClass();

}
