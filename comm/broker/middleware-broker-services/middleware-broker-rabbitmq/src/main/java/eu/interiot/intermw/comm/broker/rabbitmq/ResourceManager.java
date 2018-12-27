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
package eu.interiot.intermw.comm.broker.rabbitmq;

import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.rabbitmq.exceptions.MissingCredentialsException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton to manage connections to RabbitMQ
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public class ResourceManager {

    private Logger log = LoggerFactory.getLogger(ResourceManager.class);

    private static Map<String, ResourceManager> connections = new HashMap<String, ResourceManager>();

    private ConnectionFactory cf;

    /**
     * Gets a single instance of the {@link ResourceManager} for the given
     * parameters
     *
     * @param configuration A {@link Configuration} instance with the properties needed to
     *                      connect to RabbitMQ
     * @return A {@link ResourceManager} to connect to RabbitMQ
     * @throws BrokerException Thrown when any of the parameters required is missing
     */
    public static ResourceManager getInstance(Configuration configuration) throws BrokerException {
        String host = configuration.getRabbitmqHostname();
        int port = configuration.getRabbitmqPort();
        String user = configuration.getRabbitmqUsername();
        String password = configuration.getRabbitmqPassword();

        if (host == null || user == null || password == null) {
            throw new MissingCredentialsException();
        }

        String key = host + user;
        if (!connections.containsKey(key)) {
            connections.put(key, new ResourceManager(host, port, user, password));
        }
        return connections.get(key);
    }

    /**
     * The {@link ConnectionFactory} to connect to RabbitMQ
     *
     * @return
     */
    public ConnectionFactory getConnectionFactory() {
        return cf;
    }

    /**
     * Constructor, which we make private to enforce the singleton pattern.
     */
    private ResourceManager() {

    }

    /**
     * Constructor, which we make private to enforce the singleton pattern.
     */
    private ResourceManager(String host, int port, String username, String password) {
        this.connect(host, port, username, password);
    }

    /**
     * Creates the instance {@link CachingConnectionFactory} object
     *
     * @param host     The RabbitMQ host
     * @param port     The RabbitMQ port
     * @param username The RabbitMQ username
     * @param password The RabbitMQ password
     */
    private void connect(String host, int port, String username, String password) {
        cf = new CachingConnectionFactory(host, port);
        ((CachingConnectionFactory) cf).setUsername(username);
        ((CachingConnectionFactory) cf).setPassword(password);

        log.info("Connecting to " + host + " with the rabbit user " + username);
    }
}
