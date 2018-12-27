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
package eu.interiot.intermw.commons.model.extractors;

import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.Message;
import eu.interiot.message.payload.GOIoTPPayload;
import eu.interiot.message.payload.types.*;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gasper Vrhovsek
 */
public class IoTDeviceExtractorTest {

    @Test
    public void testExtractSingleDevice() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);
        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);

        assertEquals("", 1, ioTDevices.size());
        assertEquals("", hasName, ioTDevices.get(0).getName());
        assertEquals("", hostedBy, ioTDevices.get(0).getHostedBy());
        assertEquals("", hasLocation, ioTDevices.get(0).getLocation());
    }

    @Test
    public void testExtractMultipleDevices() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        EntityID ioTDeviceID1 = new EntityID();
        EntityID ioTDeviceID2 = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName + 1, hostedBy + 1, hasLocation + 1, ioTDeviceID1);

        // add another
        iotDevicePayload.createIoTDevice(ioTDeviceID2);
        iotDevicePayload.setHasName(ioTDeviceID2, hasName + 2);
        iotDevicePayload.setIsHostedBy(ioTDeviceID2, new EntityID(hostedBy + 2));
        iotDevicePayload.setHasLocation(ioTDeviceID2, new EntityID(hasLocation + 2));

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);
        Map<String, IoTDevice> ioTDeviceMap = ioTDevices.stream().collect(Collectors.toMap(IoTDevice::getDeviceId, x -> x));

        assertEquals("", 2, ioTDevices.size());
        assertEquals("", hasName + 1, ioTDeviceMap.get(ioTDeviceID1.toString()).getName());
        assertEquals("", hostedBy + 1, ioTDeviceMap.get(ioTDeviceID1.toString()).getHostedBy());
        assertEquals("", hasLocation + 1, ioTDeviceMap.get(ioTDeviceID1.toString()).getLocation());
        assertEquals("", hasName + 2, ioTDeviceMap.get(ioTDeviceID2.toString()).getName());
        assertEquals("", hostedBy + 2, ioTDeviceMap.get(ioTDeviceID2.toString()).getHostedBy());
        assertEquals("", hasLocation + 2, ioTDeviceMap.get(ioTDeviceID2.toString()).getLocation());
    }

    @Test
    public void testExtractWithDeviceTypeProperty() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);
        PropertyID propertyID = new PropertyID(IoTDeviceExtractor.DEVICE_TYPE_PROPERTY_ID);

        iotDevicePayload.addDataPropertyAssertionToEntity(
                ioTDeviceID,
                propertyID,
                IoTDeviceType.DEVICE.getDeviceTypeUri());
        iotDevicePayload.addDataPropertyAssertionToEntity(
                ioTDeviceID,
                propertyID,
                IoTDeviceType.SENSOR.getDeviceTypeUri());

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);
        IoTDevice ioTDevice = ioTDevices.get(0);

        assertEquals("IoTDevice has 2 types", 2, ioTDevice.getDeviceTypes().size());
        assertTrue("IoTDevice is of type DEVICE", ioTDevice.getDeviceTypes().contains(IoTDeviceType.DEVICE));
        assertTrue("IoTDevice is of type SENSOR", ioTDevice.getDeviceTypes().contains(IoTDeviceType.SENSOR));
    }

    @Test
    public void textExtractActuator() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        String forProperty = "http://inter-iot.eu/forProperty";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);

        ActuatorPayload actuatorPayload = iotDevicePayload.asActuatorPayload();
        actuatorPayload.setForProperty(ioTDeviceID, new EntityID(forProperty));

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);

        IoTDevice ioTDevice = ioTDevices.get(0);
        assertEquals("", 1, ioTDevices.size());
        assertEquals("", hasName, ioTDevice.getName());
        assertEquals("", hostedBy, ioTDevice.getHostedBy());
        assertEquals("", hasLocation, ioTDevice.getLocation());
        assertEquals("", 1, ioTDevice.getForProperty().size());
        assertTrue("", ioTDevice.getForProperty().contains(forProperty));
        assertTrue(ioTDevice.getDeviceTypes().contains(IoTDeviceType.ACTUATOR));
        assertTrue(ioTDevice.getDeviceTypes().contains(IoTDeviceType.DEVICE));
    }

    @Test
    public void testExtractActuatorWithUniqueActsOnProperty() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        String actsOnProperty = "http://inter-iot.eu/forProperty";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);
        ActuationPayload actuationPayload = iotDevicePayload.asActuationPayload();
        actuationPayload.setActsOnProperty(ioTDeviceID, new EntityID(actsOnProperty));

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);

        IoTDevice ioTDevice = ioTDevices.get(0);

        assertEquals(
                "IoTDevice should contain correct forProperty value",
                actsOnProperty,
                ioTDevice.getForProperty().iterator().next());

    }

    @Test
    public void testExtractActuatorWithLocalActsOnProperty() throws IOException {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        String actsOnProperty = "http://inter-iot.eu/forProperty";
        String actuation = "http://inter-iot.eu/actuation";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);

        GOIoTPPayload payload = iotDevicePayload.asGOIoTPPayload();
        ActuatablePropertyPayload actuatablePropertyPayload = payload.asActuatablePropertyPayload();

        EntityID actuatablePropertyID = new EntityID();
        actuatablePropertyPayload.createActuatableProperty(actuatablePropertyID);
        actuatablePropertyPayload.setIsPropertyOf(actuatablePropertyID, new EntityID(actsOnProperty));

        ActuationPayload actuationPayload = iotDevicePayload.asActuationPayload();
        actuationPayload.setActsOnProperty(ioTDeviceID, actuatablePropertyID);

        printJsonLDMessage(iotDevicePayload);

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);
        IoTDevice ioTDevice = ioTDevices.get(0);

        assertEquals(
                "IoTDevice should contain correct forProperty value",
                actsOnProperty,
                ioTDevice.getForProperty().iterator().next());
    }

    @Test
    public void textExtractSensor() {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        String observes = "http://inter-iot.eu/observes";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);


        SensorPayload sensorPayload = iotDevicePayload.asSensorPayload();
        sensorPayload.setObserves(ioTDeviceID, new EntityID(observes));

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);

        IoTDevice ioTDevice = ioTDevices.get(0);
        assertEquals("", 1, ioTDevices.size());
        assertEquals("", hasName, ioTDevice.getName());
        assertEquals("", hostedBy, ioTDevice.getHostedBy());
        assertEquals("", hasLocation, ioTDevice.getLocation());
        assertEquals("", 1, ioTDevice.getObserves().size());
        assertTrue("", ioTDevice.getObserves().contains(observes));
        assertTrue(ioTDevice.getDeviceTypes().contains(IoTDeviceType.SENSOR));
        assertTrue(ioTDevice.getDeviceTypes().contains(IoTDeviceType.DEVICE));
    }

    @Test
    public void testExtractSensorWithLocalActsOnProperty() throws IOException {
        String hasName = "hasName";
        String hostedBy = "http://inter-iot.eu/hostedby";
        String hasLocation = "http://inter-iot.eu/hasLocation";
        String observes = "http://inter-iot.eu/observes";
        EntityID ioTDeviceID = new EntityID();

        IoTDevicePayload iotDevicePayload = getIoTDevicePayload(hasName, hostedBy, hasLocation, ioTDeviceID);

        GOIoTPPayload payload = iotDevicePayload.asGOIoTPPayload();
        ObservablePropertyPayload observablePropertyPayload = payload.asObservablePropertyPayload();

        EntityID observablePropertyID = new EntityID();
        observablePropertyPayload.createObservableProperty(observablePropertyID);
        observablePropertyPayload.setIsPropertyOf(observablePropertyID, new EntityID(observes));

        ObservationPayload observationPayload = iotDevicePayload.asObservationPayload();
        observationPayload.setObservedProperty(ioTDeviceID, observablePropertyID);

        printJsonLDMessage(iotDevicePayload);

        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(iotDevicePayload);
        IoTDevice ioTDevice = ioTDevices.get(0);

        assertEquals(
                "IoTDevice should contain correct observes value",
                observes,
                ioTDevice.getObserves().iterator().next());
    }

    // TODO test TO PAYLOAD methods

    private void printJsonLDMessage(IoTDevicePayload iotDevicePayload) throws IOException {
        Message msg = new Message();
        msg.setPayload(iotDevicePayload);

        System.out.println(msg.serializeToJSONLD());
    }

    private IoTDevicePayload getIoTDevicePayload(String hasName, String hostedBy, String hasLocation, EntityID ioTDeviceID) {
        IoTDevicePayload iotDevicePayload = new IoTDevicePayload();
        iotDevicePayload.createIoTDevice(ioTDeviceID);
        iotDevicePayload.setHasName(ioTDeviceID, hasName);
        iotDevicePayload.setIsHostedBy(ioTDeviceID, new EntityID(hostedBy));
        iotDevicePayload.setHasLocation(ioTDeviceID, new EntityID(hasLocation));
        return iotDevicePayload;
    }
}
