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
package eu.interiot.intermw.commons.requests;

import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gasper Vrhovsek
 */
public class ListDevicesReqTest {

    private static final String CLIENT_ID = "myclient";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";

    @Test
    public void testPlatformListDevicesReq() throws IOException {
        ListDevicesReq req = new ListDevicesReq(CLIENT_ID, PLATFORM_ID);
        Message message = req.toMessage();

        ListDevicesReq reqConverted = new ListDevicesReq(message);

        // assert message
        assertTrue("Message metadata types should include LIST_DEVICES type",
                message.getMetadata().getMessageTypes().contains(URIManagerMessageMetadata.MessageTypesEnum.LIST_DEVICES));
        assertEquals("Message metadata receiving platform should equal PLATFORM_ID",
                PLATFORM_ID,
                message.getMetadata().asPlatformMessageMetadata().getReceivingPlatformIDs().iterator().next().toString());
        assertEquals("Message metadata client ID should equal CLIENT_ID", CLIENT_ID, message.getMetadata().getClientId().get());

        // assert converted back into req
        assertEquals("Converted ListDevicesReq client ID should equal CLIENT_ID", CLIENT_ID, reqConverted.getClientId());
        assertEquals("Converted ListDevicesReq platform ID should equal CLIENT_ID", PLATFORM_ID, reqConverted.getPlatformId());
    }
}