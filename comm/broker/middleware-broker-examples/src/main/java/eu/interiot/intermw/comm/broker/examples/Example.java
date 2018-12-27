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
package eu.interiot.intermw.comm.broker.examples;

import eu.interiot.intermw.comm.broker.Broker;
import eu.interiot.intermw.comm.broker.BrokerContext;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.Subscriber;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;

/**
 * Usage example of the Broker broker API
 *
 * By changing the Maven profile (and the {@link Configuration}) it is possible
 * to change the broker implementation with no changes in the client
 * implementation
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public class Example {

    private Broker broker;

    public static void main(String[] args) throws MiddlewareException {
        try {
            // PropertyConfigurator.configure("log4j.properties");
        } catch (Exception ignore) {

        }

        Example client = new Example(args);
        client.pubsub();
    }

    public Example(String[] args) throws MiddlewareException {
        broker = BrokerContext.getBroker();
    }

    protected void pubsub() throws BrokerException {
        Publisher<Message> publisher = broker.createPublisher(BrokerTopics.DEFAULT.getTopicName(), Message.class);
        Subscriber<Message> subscriber = broker.createSubscriber(BrokerTopics.DEFAULT.getTopicName(), Message.class);

        Message message = new Message("Example");

        subscriber.subscribe(m -> System.out.println(m));

        publisher.publish(message);

        publisher.deleteTopic(BrokerTopics.DEFAULT.getTopicName());

        subscriber.cleanUp();
        publisher.cleanUp();
    }
}