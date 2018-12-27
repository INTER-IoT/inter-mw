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

import com.google.common.hash.Hashing;
import eu.interiot.intermw.commons.requests.RegisterPlatformReq;
import eu.interiot.message.Message;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class RegisterPlatformReqTest {

    @Test
    public void testConversion() throws Exception {
        long timeCreated = System.currentTimeMillis();
        Platform platform = new Platform(timeCreated);
        platform.setClientId("myclient");
        platform.setPlatformId("http://test.inter-iot.eu/test-platform1");
        platform.setName("InterMW Test Platform");
        platform.setType("http://inter-iot.eu/MWTestPlatform");
        platform.setBaseEndpoint(new URL("http://localhost:4568"));
        platform.setLocationId("http://test.inter-iot.eu/TestLocation");

        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        platform.setUsername("interiotuser");
        platform.setEncryptedPassword(encryptedPassword);
        platform.setEncryptionAlgorithm("SHA-256");

        RegisterPlatformReq req = new RegisterPlatformReq(platform);
        assertEquals(timeCreated, req.getPlatform().getTimeCreated());

        Message message = req.toMessage();
        RegisterPlatformReq req1 = new RegisterPlatformReq(message);
        Platform platform1 = req1.getPlatform();
        assertEquals(timeCreated, platform1.getTimeCreated());
        assertEquals(platform1.getUsername(), platform.getUsername());
        assertEquals(platform1.getEncryptedPassword(), platform.getEncryptedPassword());
        assertEquals(platform1.getEncryptionAlgorithm(), platform.getEncryptionAlgorithm());
        assertEquals(platform1.getType(), platform.getType());
    }
}