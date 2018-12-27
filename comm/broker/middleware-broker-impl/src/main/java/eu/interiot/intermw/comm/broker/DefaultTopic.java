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

import eu.interiot.intermw.comm.broker.abstracts.AbstractTopic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.exceptions.ErrorCodes;

/**
 * A default {@link Topic} implementation
 *
 * When using this {@link Topic} there is no need to subclass the {@link Topic}
 * interface
 *
 * This is the preferred way of working with the {@link Broker}
 *
 * @param <M> The message class this {@link Topic} is tied to
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 * @see Broker#createPublisher(String, Class)
 * @see Broker#createSubscriber(String, Class)
 * @see ErrorCodes#DEFAULT_TOPIC_EXCEPTION
 */
@eu.interiot.intermw.comm.broker.annotations.Topic(isDefault = true)
public class DefaultTopic<M> extends AbstractTopic<M> {

    /**
     * The constructor
     *
     * @param exchangeName The name of the exchange
     * @param broker       The {@link Broker} instance
     * @param messageClass The message class this {@link Topic} is tied to
     * @throws BrokerException
     */
    public DefaultTopic(String exchangeName, Broker broker, Class<M> messageClass) throws BrokerException {
        super(exchangeName, broker, messageClass);
    }

    // public DefaultTopic(String exchangeName, Broker broker,
    // Routing<M> routing, Class<M> messageClass)
    // throws BrokerException {
    // super(exchangeName, broker, routing, messageClass);
    // }
}
