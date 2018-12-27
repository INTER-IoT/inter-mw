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

import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.payload.types.IoTDevicePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UnsubscribeReq {
    private String conversationId;
    private String subscriptionId;
    private String platformId;
    private List<String> deviceIds;
    private String clientId;

    public UnsubscribeReq() {
    }

    public UnsubscribeReq(Message message) {
        conversationId = message.getMetadata().getConversationId().orElse(null);
        clientId = message.getMetadata().getClientId().orElse(null);

        subscriptionId = message.getMetadata().asPlatformMessageMetadata().getSubscriptionId().isPresent() ?
                message.getMetadata().asPlatformMessageMetadata().getSubscriptionId().get() : conversationId;

        IoTDevicePayload ioTDevicePayload = message.getPayloadAsGOIoTPPayload().asIoTDevicePayload();
        if (ioTDevicePayload.getIoTDevices() != null && ioTDevicePayload.getIoTDevices().size() > 0) {
            deviceIds = new ArrayList<>();
            Set<EntityID> deviceEntityIds = ioTDevicePayload.getIoTDevices();
            for (EntityID deviceEntityId : deviceEntityIds) {
                deviceIds.add(deviceEntityId.toString());
            }
        }
    }

    public UnsubscribeReq(String subscriptionId, String platformId, String clientId) {
        this.subscriptionId = subscriptionId;
        this.platformId = platformId;
        this.clientId = clientId;
    }

    public Message toMessage() {
        Message message = new Message();
        MessageMetadata metadata = message.getMetadata();
        metadata.setConversationId(conversationId);
        metadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.UNSUBSCRIBE);
        metadata.setClientId(clientId);
        metadata.asPlatformMessageMetadata().addReceivingPlatformID(new EntityID(platformId));
        metadata.asPlatformMessageMetadata().setSubscriptionId(subscriptionId);

        if (deviceIds != null) {
            IoTDevicePayload ioTDevicePayload = new IoTDevicePayload();
            for (String deviceId : deviceIds) {
                EntityID entityID = new EntityID(deviceId);
                ioTDevicePayload.createIoTDevice(entityID);
            }
            message.setPayload(ioTDevicePayload);
        }

        return message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
