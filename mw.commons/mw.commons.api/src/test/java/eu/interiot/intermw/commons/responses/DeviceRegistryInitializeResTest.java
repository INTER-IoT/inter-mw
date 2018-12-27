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
package eu.interiot.intermw.commons.responses;

import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
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
public class DeviceRegistryInitializeResTest {

    private static String deviceInitJson;

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassLoader classLoader = ObservationResTest.class.getClassLoader();
        deviceInitJson = IOUtils.toString(classLoader.getResource("device_init_response.json"), "UTF-8");
    }

    @Test
    public void test() throws IOException, MessageException {

        Message message = new Message(deviceInitJson);

        DeviceRegistryInitializeRes response = new DeviceRegistryInitializeRes(message);
        List<IoTDevice> ioTDevices = response.getIoTDevices();
        Map<String, IoTDevice> ioTDeviceMap = ioTDevices.stream().collect(Collectors.toMap(IoTDevice::getDeviceId, x -> x));

        IoTDevice ioTDeviceEmission1 = ioTDeviceMap.get("http://inter-iot.eu/wso2port/emission/1");
        IoTDevice ioTDeviceLightL01 = ioTDeviceMap.get("http://inter-iot.eu/wso2port/light/L01");

        assertEquals("", 37, ioTDevices.size());

        // Assert sensor iotDeviceEmission1
        assertEquals("", "CABINA PUERTO", ioTDeviceEmission1.getName());
        assertEquals("", "http://www.inter-iot.eu/wso2port", ioTDeviceEmission1.getHostedBy());
        assertTrue("", ioTDeviceEmission1.getDeviceTypes().contains(IoTDeviceType.SENSOR));
        assertTrue("", ioTDeviceEmission1.getDeviceTypes().contains(IoTDeviceType.DEVICE));
        assertTrue("", ioTDeviceEmission1.getObserves().contains("http://inter-iot.eu/LogVPmod#Emissions"));
        assertTrue("", ioTDeviceEmission1.getObserves().contains("http://www.w3.org/ns/sosa/ObservableProperty"));
        assertTrue("", ioTDeviceEmission1.getObserves().contains("http://inter-iot.eu/GOIoTP#MeasurementKind"));

        // Assert actuator ioTDeviceLight01
        assertEquals("", "Noatum access road - E3TCity managed", ioTDeviceLightL01.getName());
        assertEquals("", "http://www.inter-iot.eu/wso2port", ioTDeviceLightL01.getHostedBy());
        assertTrue("", ioTDeviceLightL01.getDeviceTypes().contains(IoTDeviceType.ACTUATOR));
        assertTrue("", ioTDeviceLightL01.getDeviceTypes().contains(IoTDeviceType.DEVICE));
        assertTrue("", ioTDeviceLightL01.getForProperty().contains("http://inter-iot.eu/LogVPmod#Light"));
        assertTrue("", ioTDeviceLightL01.getForProperty().contains("http://www.w3.org/ns/sosa/ActuableProperty"));
        assertTrue("", ioTDeviceLightL01.getForProperty().contains("http://inter-iot.eu/GOIoTP#MeasurementKind"));
    }
}
