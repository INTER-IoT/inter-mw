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
package eu.interiot.intermw.commons.model.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Author: gasper
 * On: 10.9.2018
 */
public class IoTDeviceTypeTest {

    private static final String DEVICE_URI = "http://inter-iot.eu/GOIoTP#IoTDevice";
    private static final String SENSOR_URI = "http://www.w3.org/ns/sosa/Sensor";
    private static final String ACTUATOR_URI = "http://www.w3.org/ns/sosa/Actuator";

    @Test
    public void shouldConvertEnumToUri() {
        assertEquals("DEVICE enum should return correct device type URI", DEVICE_URI, IoTDeviceType.DEVICE.getDeviceTypeUri());
        assertEquals("SENSOR enum should return correct device type URI", SENSOR_URI, IoTDeviceType.SENSOR.getDeviceTypeUri());
        assertEquals("ACTUATOR enum should return correct device type URI", ACTUATOR_URI, IoTDeviceType.ACTUATOR.getDeviceTypeUri());
    }

    @Test
    public void shouldConvertUriToEnum() {
        assertEquals("DEVICE_URI should produce DEVICE IoTDeviceType enum", IoTDeviceType.DEVICE, IoTDeviceType.fromDeviceTypeUri(DEVICE_URI));
        assertEquals("SENSOR_URI should produce SENSOR IoTDeviceType enum", IoTDeviceType.SENSOR, IoTDeviceType.fromDeviceTypeUri(SENSOR_URI));
        assertEquals("ACTUATOR_URI should produce ACTUATOR IoTDeviceType enum", IoTDeviceType.ACTUATOR, IoTDeviceType.fromDeviceTypeUri(ACTUATOR_URI));
    }
}
