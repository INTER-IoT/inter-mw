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
import eu.interiot.intermw.comm.broker.abstracts.AbstractService;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.exceptions.ErrorCodes;
import org.apache.activemq.broker.jmx.BrokerViewMBean;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Properties;

public abstract class AbstractActiveMQService<M> extends AbstractService<M> {

    protected Connection connection;
    protected Session session;
    protected Destination destination;

    public final static String ACTIVEMQ_PROPERTIY_PREFIX = "activemq-";

    protected String domain = "org.apache.activemq";

    protected Properties getActiveMQProperties() {
        return this.broker.getConfiguration().getPropertiesWithPrefix(ACTIVEMQ_PROPERTIY_PREFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initConnection(Broker broker) throws BrokerException {
        try {
            connection = ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory()
                    .createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new BrokerException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createTopic(String name) throws BrokerException {
        if (session == null) {
            throw new BrokerException(ErrorCodes.CANNOT_CREATE_TOPIC.errorCode,
                    ErrorCodes.CANNOT_CREATE_TOPIC.errorDescription);
        }

        try {
            destination = session.createTopic(name);
        } catch (JMSException e) {
            throw new BrokerException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * FIXME: To be tested with a running instance
     */
    public void deleteTopic(String name) throws BrokerException {
        JMXConnector jmxc = null;
        MBeanServerConnection conn = null;

        try {
            JMXServiceURL url = new JMXServiceURL(
                    "service:jmx:rmi:///jndi/rmi://" + this.getActiveMQProperties().getProperty("rmi") + "/jmxrmi");
            jmxc = JMXConnectorFactory.connect(url);
            conn = jmxc.getMBeanServerConnection();

            ObjectName brokerName = new ObjectName("org.apache.activemq:BrokerName=" + this.getActiveMQProperties().getProperty("brokername") + ",Type=Broker");
            BrokerViewMBean broker = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(conn, brokerName,
                    BrokerViewMBean.class, true);

            broker.removeTopic(name);

        } catch (Exception e) {
            throw new BrokerException(e);
        } finally {
            if (jmxc != null) {
                try {
                    jmxc.close();
                } catch (Exception e) {
                    throw new BrokerException(e);
                }
            }
        }
    }

}
