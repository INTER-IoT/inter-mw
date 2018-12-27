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
package eu.interiot.intermw.comm.broker.mqtt;

import eu.interiot.intermw.comm.broker.Broker;
import eu.interiot.intermw.comm.broker.abstracts.AbstractService;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;
import java.util.Properties;

public abstract class AbstractMQTTService<M> extends AbstractService<M> {

    protected MqttClient mqttClient;
    protected MqttConnectOptions connOpts = new MqttConnectOptions();
    protected MemoryPersistence persistence = new MemoryPersistence();

    public final static String MQTT_PROPERTIY_PREFIX = "mqtt.";

    private final static String MQTT_HOST_PROPERTY = "host";
    private final static String MQTT_QOS_PROPERTY = "qos";
    private final static String MQTT_CLIENTID_PROPERTY = "client-id";
    private final static String MQTT_USER_PROPERTY = "user";
    private final static String MQTT_PASSWORD_PROPERTY = "password";
    private final static int DEFAULT_QOS = 2;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTopic(String name) throws BrokerException {
        // UNUSED
    }

    @Override
    public void deleteTopic(String name) throws BrokerException {
        // UNUSED
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initConnection(Broker broker) throws BrokerException {
        this.broker = broker;
        connOpts.setCleanSession(true);

        String password = this.getPassword();
        if (!StringUtils.isEmpty(password)) {
            connOpts.setPassword(password.toCharArray());
        }

        String userName = this.getUserName();
        if (!StringUtils.isEmpty(userName)) {
            connOpts.setUserName(userName);
        }

        try {
            mqttClient = new MqttClient(this.getHost(), this.getClientId(), persistence);
            mqttClient.connect(connOpts);
        } catch (Exception e) {
            throw new BrokerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp() throws BrokerException {
        if (this.mqttClient != null) {
            try {
                this.mqttClient.disconnect();
            } catch (Exception e) {
                throw new BrokerException(e);
            }
        }
    }

    protected int getQos() {
        try {
            return Integer.parseInt(this.getMQTTProperties().getProperty(MQTT_QOS_PROPERTY));
        } catch (Exception e) {
            return DEFAULT_QOS;
        }
    }

    protected Properties getMQTTProperties() {
        return this.broker.getConfiguration().getPropertiesWithPrefix(MQTT_PROPERTIY_PREFIX);
    }

    protected String getClientId() {
        return this.getMQTTProperties().getProperty(MQTT_CLIENTID_PROPERTY) + getUniqueClientId();
    }

    protected String getUniqueClientId() {
        return String.valueOf(new Date().getTime());
    }

    private String getHost() {
        return this.getMQTTProperties().getProperty(MQTT_HOST_PROPERTY);
    }

    private String getUserName() {
        return this.getMQTTProperties().getProperty(MQTT_USER_PROPERTY);
    }

    private String getPassword() {
        return this.getMQTTProperties().getProperty(MQTT_PASSWORD_PROPERTY);
    }
}
