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
package eu.interiot.intermw.bridge;

import eu.interiot.intermw.commons.abstracts.AbstractConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BridgeConfiguration extends AbstractConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BridgeConfiguration.class);

    public BridgeConfiguration(String propertiesFileName, String platformId, Configuration intermwConf) throws MiddlewareException {
        log.debug("Loading configuration for the platform {} from file {}...", platformId, propertiesFileName);
        properties = intermwConf.getBridgeCommonProperties();
        Properties platformProperties = loadPropertiesForPlatform(propertiesFileName, platformId);
        overrideProperties(platformProperties);
        log.debug("Configuration has been loaded successfully: {}", properties);
    }

    private Properties loadPropertiesForPlatform(String propertiesFileName, String platformId) throws MiddlewareException {
        Properties bridgeProperties = loadPropertiesFile(propertiesFileName);

        String platformsProp = bridgeProperties.getProperty("platforms");
        if (platformsProp == null || platformsProp.trim().isEmpty()) {
            return bridgeProperties;
        }
        List<String> aliases = Arrays.asList(platformsProp.trim().split(","));
        String selectedPlatformAlias = null;
        for (String alias : aliases) {
            String aliasPlatformId = bridgeProperties.getProperty(alias + ".id");
            if (aliasPlatformId != null && aliasPlatformId.equals(platformId)) {
                selectedPlatformAlias = alias;
            }
        }

        Properties commonProperties = new Properties();
        Properties platformProperties = new Properties();
        for (String propertyName : bridgeProperties.stringPropertyNames()) {
            if (propertyName.equals("platforms")) {
                continue;
            }
            String propertyValue = bridgeProperties.getProperty(propertyName);
            int index = propertyName.indexOf(".");
            if (index == -1) {
                commonProperties.setProperty(propertyName, propertyValue);
                continue;
            }

            String part1 = propertyName.substring(0, index);
            if (part1.equals(selectedPlatformAlias)) {
                String part2 = propertyName.substring(index + 1);
                if (part2.equals("id")) {
                    continue;
                } else {
                    platformProperties.setProperty(part2, propertyValue);
                }

            } else if (aliases.contains(part1)) {
                continue;

            } else {
                commonProperties.setProperty(propertyName, propertyValue);
            }
        }

        Properties properties = new Properties(commonProperties);
        for (String property : platformProperties.stringPropertyNames()) {
            properties.setProperty(property, platformProperties.getProperty(property));
        }
        return properties;
    }

    private void overrideProperties(Properties properties1) {
        for (String key : properties1.stringPropertyNames()) {
            properties.setProperty(key, properties1.getProperty(key));
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getBridgeCallbackUrl() {
        return properties.getProperty("bridge.callback.url");
    }
}
