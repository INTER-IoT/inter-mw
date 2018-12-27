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
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.payload.types.IoTDevicePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SubscribeReq {

    private String conversationId;
    private String subscriptionId;
    private String clientId;
    private String platformId;
    private List<String> deviceIds;
    private List<IoTDevice> devices;

    public SubscribeReq() {
    }

    public SubscribeReq(String subscriptionId, String clientId, String platformId, List<IoTDevice> devices) {
        this.subscriptionId = subscriptionId;
        this.clientId = clientId;
        this.platformId = platformId;
        this.devices = devices;
    }

    public SubscribeReq(Message message) throws InvalidMessageException {
        conversationId = message.getMetadata().getConversationId().orElse(null);
        clientId = message.getMetadata().getClientId().orElse(null);

        subscriptionId = message.getMetadata().asPlatformMessageMetadata().getSubscriptionId().isPresent() ?
                message.getMetadata().asPlatformMessageMetadata().getSubscriptionId().get() : conversationId;

        platformId = message.getMetadata().asPlatformMessageMetadata().getReceivingPlatformIDs().iterator().next().toString();

        IoTDevicePayload ioTDevicePayload = message.getPayloadAsGOIoTPPayload().asIoTDevicePayload();
        Set<EntityID> deviceEntityIds = ioTDevicePayload.getIoTDevices();
        devices = new ArrayList<>();
        deviceIds = new ArrayList<>();
        for (EntityID deviceEntityId : deviceEntityIds) {
            String deviceId = deviceEntityId.toString();
            Optional<EntityID> hostedBy = ioTDevicePayload.getIsHostedBy(deviceEntityId);
            Optional<EntityID> location = ioTDevicePayload.getHasLocation(deviceEntityId);
            Optional<String> name = ioTDevicePayload.getHasName(deviceEntityId);

            if (hostedBy.isPresent() && !hostedBy.get().toString().equals(platformId)) {
                throw new InvalidMessageException(String.format(
                        "Invalid SUBSCRIBE message: receiving platform ID %s doesn't match device's HostedBy value %s.",
                        platformId, hostedBy.get()
                ));
            }

            IoTDevice ioTDevice = new IoTDevice(deviceId);
            ioTDevice.setHostedBy(hostedBy.isPresent() ? hostedBy.get().toString() : null);
            ioTDevice.setLocation(location.isPresent() ? location.get().toString() : null);
            ioTDevice.setName(name.orElse(null));
            devices.add(ioTDevice);
            deviceIds.add(deviceId);
        }
    }

    public Message toMessage() {
        return toMessage(conversationId);
    }

    public Message toMessage(String conversationId) {
        IoTDevicePayload ioTDevicePayload = new IoTDevicePayload();
        for (IoTDevice device : devices) {
            EntityID entityID = new EntityID(device.getDeviceId());
            ioTDevicePayload.createIoTDevice(entityID);
            if (device.getHostedBy() != null) {
                ioTDevicePayload.setIsHostedBy(entityID, new EntityID(device.getHostedBy()));
            }
            if (device.getLocation() != null) {
                ioTDevicePayload.setHasLocation(entityID, new EntityID(device.getLocation()));
            }
            if (device.getName() != null) {
                ioTDevicePayload.setHasName(entityID, device.getName());
            }
        }

        Message message = new Message();
        MessageMetadata metadata = message.getMetadata();
        metadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.SUBSCRIBE);
        metadata.setClientId(clientId);
        metadata.setConversationId(conversationId);
        metadata.asPlatformMessageMetadata().setSubscriptionId(subscriptionId);
        metadata.asPlatformMessageMetadata().addReceivingPlatformID(new EntityID(platformId));
        message.setPayload(ioTDevicePayload);

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public List<IoTDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<IoTDevice> devices) {
        this.devices = devices;
    }
}
