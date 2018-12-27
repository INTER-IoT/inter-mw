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
package eu.interiot.intermw.comm.broker.activemq;

import eu.interiot.intermw.comm.broker.Broker;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.Queue;
import eu.interiot.intermw.comm.broker.Topic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import java.util.List;

@eu.interiot.intermw.comm.broker.annotations.Publisher(broker = "activemq")
public class PublisherImpl<M> extends AbstractActiveMQService<M> implements Publisher<M> {

    private Logger log = LoggerFactory.getLogger(PublisherImpl.class);

    protected MessageProducer producer;

    @Override
    public void init(Broker broker, List<Queue> queues, Class<Topic<M>> topicClass) throws BrokerException {
        log.warn("This implementation of ActiveMQ DOES NOT support creation of queues");
        this.init(broker, topicClass);
    }

    @Override
    public void init(Broker broker, List<eu.interiot.intermw.comm.broker.Queue> queues, String exchangeName,
                     Class<M> messageClass) throws BrokerException {
        log.warn("This implementation of ActiveMQ DOES NOT support creation of queues");
        this.init(broker, exchangeName, messageClass);
    }

    @Override
    public void publish(M message) throws BrokerException {
        topic.setMessage(message);
        ObjectMessage objectMessage;
        try {
            if (producer == null) {
                this.createProducer(topic);
            }

            objectMessage = session.createObjectMessage();
            objectMessage.setObject(topic.serialize());
            // FIXME allow routing
            // applyRouting(message, topic);

            producer.send(objectMessage);
        } catch (JMSException e) {
            log.error("publish", e);
            throw new BrokerException(e.getMessage(), e);
        }
    }

    protected void createProducer(Topic<M> topic) throws BrokerException {
        try {
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        } catch (JMSException e) {
            throw new BrokerException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                log.error("cleanUp", e);
            }
        }

        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException e) {
                log.error("cleanUp", e);
            }
        }

        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                log.error("cleanUp", e);
            }
        }
    }
}
