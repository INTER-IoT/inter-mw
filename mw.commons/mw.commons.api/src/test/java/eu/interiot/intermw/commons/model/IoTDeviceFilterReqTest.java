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
package eu.interiot.intermw.commons.model;

import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.intermw.commons.requests.DeviceDiscoveryQueryReq;
import eu.interiot.message.Message;
import eu.interiot.message.metadata.QueryMessageMetadata;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Gasper Vrhovsek
 */
public class IoTDeviceFilterReqTest {
    private static final String CLIENT_ID = "http://inter-iot.com/client1";
    private static final String DEVICE_TYPE_ACTUATOR_QUERY = "{\"deviceTypes\":[\"ACTUATOR\"]}";
    private static final String CONVERSATION_ID = "conversationId";

    @Test
    public void testWithDeviceDiscoveryQueryObject() throws IOException {
        IoTDeviceFilter ddq = new IoTDeviceFilter();
        ddq.addDeviceType(IoTDeviceType.ACTUATOR);
        DeviceDiscoveryQueryReq ddqr = new DeviceDiscoveryQueryReq(CLIENT_ID, ddq);
        Message message = ddqr.toMessage(CONVERSATION_ID);
        QueryMessageMetadata queryMessageMetadata = message.getMetadata().asQueryMetadata();
        String queryString = queryMessageMetadata.getQueryString().get();

        assertEquals("Query string should be correctly generated", DEVICE_TYPE_ACTUATOR_QUERY, queryString);
    }

    @Test
    public void testWithStringQuery() throws IOException {
        DeviceDiscoveryQueryReq ddqr = new DeviceDiscoveryQueryReq(CLIENT_ID, DEVICE_TYPE_ACTUATOR_QUERY);
        Message message = ddqr.toMessage(CONVERSATION_ID);
        QueryMessageMetadata queryMessageMetadata = message.getMetadata().asQueryMetadata();
        String queryString = queryMessageMetadata.getQueryString().get();

        assertEquals("Query string should be correctly generated", DEVICE_TYPE_ACTUATOR_QUERY, queryString);
    }
}
