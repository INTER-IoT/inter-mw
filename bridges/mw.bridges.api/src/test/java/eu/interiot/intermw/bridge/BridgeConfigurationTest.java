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

import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BridgeConfigurationTest {

    @Test
    public void testBridgeConfigurationLoading() throws Exception {
        Configuration intermwConf = new DefaultConfiguration("intermw-test.properties");

        BridgeConfiguration conf = new BridgeConfiguration("bridge-config-test.properties", "http://test.inter-iot.eu/test-platform1", intermwConf);
        assertEquals(conf.getProperties().size(), 7);
        assertEquals(conf.getProperty("bridge.callback.url"), "http://localhost:8980");
        assertEquals(conf.getProperty("bridge.testproperty"), "testproperty-value");
        assertEquals(conf.getProperty("testproperty1"), "instanceA-value1");
        assertEquals(conf.getProperty("testproperty2"), "instanceA-value2");
        assertEquals(conf.getProperty("testproperty3"), "value3");
        assertEquals(conf.getProperty("testproperty5"), "value5");
        assertEquals(conf.getProperty("testproperty6"), "value6");

        conf = new BridgeConfiguration("bridge-config-test.properties", "http://test.inter-iot.eu/test-platform2", intermwConf);
        assertEquals(conf.getProperty("bridge.callback.url"), "http://localhost:8980");
        assertEquals(conf.getProperty("bridge.testproperty"), "testproperty-value");
        assertEquals(conf.getProperties().size(), 8);
        assertEquals(conf.getProperty("testproperty1"), "value1");
        assertEquals(conf.getProperty("testproperty2"), "value2");
        assertEquals(conf.getProperty("testproperty3"), "value3");
        assertEquals(conf.getProperty("testproperty5"), "value5");
        assertEquals(conf.getProperty("testproperty6"), "value6");
        assertEquals(conf.getProperty("testproperty7"), "value7");
    }
}