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
package eu.interiot.intermw.commons.interfaces;

import java.util.List;
import java.util.Properties;


/**
 * An interface to retrieve configuration elements for the Communication and Control { @link Middleware}
 * entities
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Configuration {
    public static final String SCAN_PACKAGES = "scan-packages";

    /**
     * A list of strings representing the packages where the implementations of
     * {APIs are
     *
     * @return
     */
    List<String> getScanPackages();

    /**
     * The whole {@link Properties} object so that any class uses what it needs
     *
     * @return
     */
    Properties getProperties();

    /**
     * Checks if given configuration property is set.
     *
     * @param key The property key
     * @return true if given configuration property is set
     */
    boolean contains(String key);

    /**
     * A configuration property given a <key>
     *
     * @param key The property key
     * @return The property value
     */
    String getProperty(String key);

    /**
     * From the whole {@link Properties} object it takes the ones that starts
     * with <prefix>
     *
     * @param prefix The prefix to filter the {@link Properties} object
     * @return A subset of the {@link Properties} object
     */
    Properties getPropertiesWithPrefix(String prefix);

    Properties getPropertiesWithPrefix(String prefix, boolean removePrefix);

    String getParliamentUrl();

    String getBrokerType();

    String getRabbitmqHostname();

    int getRabbitmqPort();

    String getRabbitmqUsername();

    String getRabbitmqPassword();

    Properties getBridgeCommonProperties();

    String getBridgeCallbackUrl();

    String getIPSMApiBaseUrl();

    int getClientReceivingCapacityDefault();

    int getQueryResponseTimeout();

    String getRestApiAuthzMethod();
}
