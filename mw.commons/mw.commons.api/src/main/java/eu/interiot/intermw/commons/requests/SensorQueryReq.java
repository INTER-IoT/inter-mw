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

import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.payload.types.IoTDevicePayload;

import java.util.List;

import static eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;

/**
 * Author: gasper
 * On: 17.10.2018
 */
public class SensorQueryReq {
    private final String clientId;
    private final String platformId;
    private List<IoTDevice> sensors;

    public SensorQueryReq(List<IoTDevice> sensors, String clientId, String platformId) {
        this.sensors = sensors;
        this.clientId = clientId;
        this.platformId = platformId;
    }

    public Message toMessage() {
        Message message = new Message();
        MessageMetadata metadata = message.getMetadata();
        metadata.setMessageType(MessageTypesEnum.QUERY);
        metadata.setClientId(clientId);
        metadata.asPlatformMessageMetadata().addReceivingPlatformID(new EntityID(platformId));

        IoTDevicePayload payload = new IoTDevicePayload();
        for (IoTDevice sensor : sensors) {
            payload.createIoTDevice(new EntityID(sensor.getDeviceId()));
        }

        message.setPayload(payload);
        return message;
    }

    public List<IoTDevice> getSensors() {
        return sensors;
    }

    public void setSensors(List<IoTDevice> sensors) {
        this.sensors = sensors;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPlatformId() {
        return platformId;
    }
}
