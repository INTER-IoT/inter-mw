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
package eu.interiot.intermw.commons;

import eu.interiot.intermw.commons.abstracts.AbstractConfiguration;
import eu.interiot.intermw.commons.annotations.Configuration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * The default {@link Configuration} implentation based on a {@link Properties}
 * file
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 */
@Configuration
public class DefaultConfiguration extends AbstractConfiguration implements eu.interiot.intermw.commons.interfaces.Configuration {
    /**
     * Crates a new {@link DefaultConfiguration} given a <propertiesFileName>
     *
     * @param propertiesFileName The properties file name
     * @throws MiddlewareException
     */
    public DefaultConfiguration(String propertiesFileName) throws MiddlewareException {
        super(propertiesFileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getScanPackages() {
        String scanPackages = this.properties.getProperty(eu.interiot.intermw.commons.interfaces.Configuration.SCAN_PACKAGES);
        if (scanPackages == null) {
            List<String> defaultArray = new ArrayList<String>();
            defaultArray.add("eu");
            return defaultArray;
        }
        return Arrays.asList(scanPackages.split(","));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public boolean contains(String key) {
        return this.properties.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }

    public Properties getPropertiesWithPrefix(String prefix) {
        return getPropertiesWithPrefix(prefix, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getPropertiesWithPrefix(String prefix, boolean removePrefix) {
        Properties prefixedProperties = new Properties();

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String newKey = removePrefix ? key.substring(prefix.length()) : key;
                prefixedProperties.put(newKey, properties.getProperty(key));
            }
        }

        return prefixedProperties;
    }

    @Override
    public String getParliamentUrl() {
        return properties.getProperty("parliament.url");
    }

    @Override
    public String getBrokerType() {
        return properties.getProperty("broker.type");
    }

    @Override
    public String getRabbitmqHostname() {
        return properties.getProperty("rabbitmq.hostname");
    }

    @Override
    public int getRabbitmqPort() {
        return Integer.parseInt(properties.getProperty("rabbitmq.port"));
    }

    @Override
    public String getRabbitmqUsername() {
        return properties.getProperty("rabbitmq.username");
    }

    @Override
    public String getRabbitmqPassword() {
        return properties.getProperty("rabbitmq.password");
    }

    @Override
    public Properties getBridgeCommonProperties() {
        return getPropertiesWithPrefix("bridge.");
    }

    @Override
    public String getBridgeCallbackUrl() {
        return properties.getProperty("bridge.callback.url");
    }

    @Override
    public String getIPSMApiBaseUrl() {
        return properties.getProperty("ipsm.api.baseUrl");
    }

    @Override
    public int getClientReceivingCapacityDefault() {
        return Integer.parseInt(properties.getProperty("client.receivingCapacity.default"));
    }

    @Override
    public int getQueryResponseTimeout() {
        return Integer.parseInt(properties.getProperty("intermw.api.query.timeout"));
    }

    @Override
    public String getRestApiAuthzMethod() {
        return properties.getProperty("restapi.authz.method");
    }
}
