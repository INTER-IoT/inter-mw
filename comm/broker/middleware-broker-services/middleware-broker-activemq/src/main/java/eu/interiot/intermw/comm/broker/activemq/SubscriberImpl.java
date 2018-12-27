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

import eu.interiot.intermw.comm.broker.Listener;
import eu.interiot.intermw.comm.broker.Queue;
import eu.interiot.intermw.comm.broker.Subscriber;
import eu.interiot.intermw.comm.broker.Topic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.List;

@eu.interiot.intermw.comm.broker.annotations.Subscriber(broker = "activemq")
public class SubscriberImpl<M> extends AbstractActiveMQService<M> implements Subscriber<M> {

    protected Logger log = LoggerFactory.getLogger(SubscriberImpl.class);

    protected MessageConsumer consumer;

    /**
     * {@inheritDoc}
     */
    public void subscribe(Listener<M> listener) throws BrokerException {
        try {
            List<String> routingKeys = topic.getSubscribingRoutings();

            if (routingKeys == null || routingKeys.isEmpty()) {
                doSubscribe(topic, listener, destination, null);
            } else {
                for (String routingKey : routingKeys) {
                    doSubscribe(topic, listener, destination, routingKey);
                }
            }
        } catch (JMSException e1) {
            log.error("subscribe", e1);
        }
    }

    protected void doSubscribe(Topic<M> topic, Listener<M> listener, Destination destination, String routingKey)
            throws JMSException {
        MessageConsumer consumer = createConsumer(destination, routingKey);
        consumer.setMessageListener(new ActiveMQListener(listener, topic));
    }

    /**
     * {@link Queue} not supported for activeMQ
     *
     * @see Subscriber#subscribe(Object, Listener)
     */
    public void subscribe(Listener<M> listener, eu.interiot.intermw.comm.broker.Queue _queue)
            throws BrokerException {
        subscribe(listener, null);
    }

    public MessageConsumer createConsumer(Destination destination, String filter) throws JMSException {
        MessageConsumer consumer;

        if (filter == null) {
            consumer = session.createConsumer(destination);
        } else {
            consumer = session.createConsumer(destination, filter);
        }

        return consumer;
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

        if (consumer != null) {
            try {
                consumer.close();
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

    private class ActiveMQListener implements MessageListener {

        private Listener<M> listener;
        private Topic<M> topic;

        public ActiveMQListener(Listener<M> listener, Topic<M> topic) {
            this.listener = listener;
            this.topic = topic;
        }

        public void onMessage(Message message) {
            try {
                String topicMessage = null;
                if (message instanceof ObjectMessage) {
                    topicMessage = ((ObjectMessage) message).getObject().toString();
                } else if (message instanceof TextMessage) {
                    TextMessage tm = (TextMessage) message;
                    topicMessage = tm.getText();
                } else {
                    log.error("Unsupported ActveMQ Message type: " + message.getClass());
                }

                if (topicMessage != null) {
                    M deserializedMessage = topic.deserialize(topicMessage, topic.getMessageClass());
                    listener.handle(deserializedMessage);
                }
            } catch (Exception e) {
                log.error("onMessage", e);
            }
        }
    }
}
