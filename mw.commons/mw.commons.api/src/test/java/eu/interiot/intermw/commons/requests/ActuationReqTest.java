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

import eu.interiot.intermw.commons.exceptions.InvalidMessageException;
import eu.interiot.intermw.commons.model.Actuation;
import eu.interiot.intermw.commons.model.ActuationResult;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.payload.GOIoTPPayload;
import eu.interiot.message.payload.types.ActuationPayload;
import eu.interiot.message.payload.types.IoTDevicePayload;
import eu.interiot.message.payload.types.PlatformPayload;
import eu.interiot.message.payload.types.SystemPayload;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: gasper
 * On: 20.6.2018
 */
public class ActuationReqTest {

    private static final Logger logger = LoggerFactory.getLogger(ActuationReqTest.class);

    private static final String CLIENT_ID = "myclient";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String ACTUATOR_ID = "http://test.inter-iot.eu/actuator";
    private static final String ACTUATOR_LOCAL_ID = "actuator";
    private static final String DEVICE_ID = "http://test.inter-iot.eu/device";
    private static final String PROPERTY_ID = "http://test.inter-iot.eu/actuator#lights%d";
    private static final String RESULT_VALUE = "%d";
    private static final String RESULT_UNIT = "http://test.inter-iot.eu/unit/celsius";
    private static final String CONVERSATION_ID = "conversation_1";
    private static final int RESULT_SET_LEGNTH = 5;

    @Test
    public void shouldConvertSuccessfullyOnCompleteActuation() throws InvalidMessageException, IOException {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();
        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        Message message = request.toMessage(CONVERSATION_ID);
        ActuationReq requestFromMessage = new ActuationReq(message);

        Assert.assertTrue(
                "ActuationReq should be equal after transforming to message and back",
                request.equals(requestFromMessage));

        Assert.assertEquals(
                "ActsOnProperty should match",
                CLIENT_ID,
                requestFromMessage.getActuation().getClientId());

        Assert.assertEquals(
                "ActsOnProperty should match",
                ACTUATOR_ID,
                requestFromMessage.getActuation().getMadeByActuator());

        Assert.assertEquals(
                "ActsOnProperty should match",
                PLATFORM_ID,
                requestFromMessage.getActuation().getPlatformId());

        Assert.assertEquals(
                "ActuationResults size should match",
                RESULT_SET_LEGNTH,
                requestFromMessage.getActuation().getActuationResults().size());

        Assert.assertEquals(
                "ActuationResults elements should match",
                true,
                requestFromMessage.getActuation().getActuationResults().containsAll(request.getActuation().getActuationResults()));
    }

    @Test
    public void shouldAddAditionalDataWhenDeviceIdDiffersFromActuationId() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();
        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        Message message = request.toMessage(CONVERSATION_ID);

        // Assert message
        GOIoTPPayload payloadAsGOIoTPPayload = message.getPayloadAsGOIoTPPayload();
        ActuationPayload actuationPayload = payloadAsGOIoTPPayload.asActuationPayload();
        IoTDevicePayload ioTDevicePayload = actuationPayload.asIoTDevicePayload();
        SystemPayload systemPayload = ioTDevicePayload.asSystemPayload();
        EntityID ioTDeviceEntitiyId = ioTDevicePayload.getIoTDevices().iterator().next();

        PlatformPayload platformPayload = ioTDevicePayload.asPlatformPayload();
        Set<EntityID> platforms = platformPayload.getPlatforms();
        Set<EntityID> systems = systemPayload.getSystems();
        Set<EntityID> hosts = platformPayload.getHosts(ioTDeviceEntitiyId);


        Assert.assertNotNull("IoTDevicePayload should not be null", ioTDevicePayload);
        Assert.assertEquals("IoTDevicePayload should contain 1 device", 1, ioTDevicePayload.getIoTDevices().size());
        Assert.assertEquals("IoTDevice should host one entity", 1, hosts.size());
        Assert.assertEquals("IoTDevicePayload as SystemPayload should contain 1 system", 1, systems.size());
        Assert.assertEquals("IoTDevicePayload as PlatformPayload should contain 0 platforms", 0, platforms.size());
        Assert.assertTrue("IoTDevice should also be a system", systems.iterator().next().toString().equals(ioTDeviceEntitiyId.toString()));
    }

    @Test
    public void shouldNotAddAditionalDataWhenDeviceIdEqualsActuationId() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(ACTUATOR_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();
        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        Message message = request.toMessage(CONVERSATION_ID);

        // Assert message
        GOIoTPPayload payloadAsGOIoTPPayload = message.getPayloadAsGOIoTPPayload();
        ActuationPayload actuationPayload = payloadAsGOIoTPPayload.asActuationPayload();
        IoTDevicePayload ioTDevicePayload = actuationPayload.asIoTDevicePayload();
        SystemPayload systemPayload = ioTDevicePayload.asSystemPayload();
        EntityID ioTDeviceEntitiyId = ioTDevicePayload.getIoTDevices().iterator().next();

        PlatformPayload platformPayload = ioTDevicePayload.asPlatformPayload();
        Set<EntityID> platforms = platformPayload.getPlatforms();
        Set<EntityID> systems = systemPayload.getSystems();
        Set<EntityID> hosts = platformPayload.getHosts(ioTDeviceEntitiyId);


        Assert.assertNotNull("IoTDevicePayload should not be null", ioTDevicePayload);
        Assert.assertEquals("IoTDevicePayload should contain 1 device", 1, ioTDevicePayload.getIoTDevices().size());
        Assert.assertEquals("IoTDevice should host no entities", 0, hosts.size());
        Assert.assertEquals("IoTDevicePayload as SystemPayload should contain 0 systems", 0, systems.size());
        Assert.assertEquals("IoTDevicePayload as PlatformPayload should contain 0 platforms", 0, platforms.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailConversionOnMissingClientId() {
        Actuation actuation = new Actuation();
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();

        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        request.toMessage(CONVERSATION_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailConversionOnMissingMadeByActuator() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();

        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        request.toMessage(CONVERSATION_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailConversionOnMissingPlatformId() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setMadeByActuatorLocalId(ACTUATOR_LOCAL_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();

        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        request.toMessage(CONVERSATION_ID);
    }

    @Test()
    public void shouldNotFailConversionOnMissingMadeByActuatorLocalId() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setDeviceId(DEVICE_ID);

        Set<ActuationResult> actuationResultList = new HashSet<>();

        for (int i = 0; i < RESULT_SET_LEGNTH; i++) {
            actuationResultList.add(
                    new ActuationResult(String.format(PROPERTY_ID, i), String.format(RESULT_VALUE, i), RESULT_UNIT));
        }
        actuation.setActuationResults(actuationResultList);

        ActuationReq request = new ActuationReq(actuation);
        request.toMessage(CONVERSATION_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailConversionOnEmptyActuationResults() {
        Actuation actuation = new Actuation();
        actuation.setClientId(CLIENT_ID);
        actuation.setPlatformId(PLATFORM_ID);
        actuation.setMadeByActuator(ACTUATOR_ID);
        actuation.setDeviceId(DEVICE_ID);
        ActuationReq request = new ActuationReq(actuation);
        request.toMessage(CONVERSATION_ID);
    }
}
