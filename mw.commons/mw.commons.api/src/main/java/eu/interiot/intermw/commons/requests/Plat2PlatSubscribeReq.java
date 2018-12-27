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
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerINTERMW;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.VirtualSubscriptionMessageMetadata;
import eu.interiot.message.payload.TypedPayload;

import java.util.Map;

public class Plat2PlatSubscribeReq {
    private static final PropertyID TARGET_DEVICE_ID_PROP = new PropertyID(URIManagerINTERMW.PREFIX.intermw + "targetDeviceId");
    private static final PropertyID TARGET_PLATFORM_ID_PROP = new PropertyID(URIManagerINTERMW.PREFIX.intermw + "targetPlatformId");
    private static final PropertyID SOURCE_DEVICE_ID_PROP = new PropertyID(URIManagerINTERMW.PREFIX.intermw + "sourceDeviceId");
    private static final PropertyID SOURCE_PLATFORM_ID_PROP = new PropertyID(URIManagerINTERMW.PREFIX.intermw + "sourcePlatformId");
    private static final EntityID SUBSCRIPTION_ENTITYID = new EntityID("http://inter-iot.eu/GOIoTP#plat2plat-subscription");

    private String conversationId;
    private String subscriptionId;
    private String clientId;
    private String targetDeviceId;
    private String targetPlatformId;
    private String sourceDeviceId;
    private String sourcePlatformId;

    public Plat2PlatSubscribeReq() {
    }

    public Plat2PlatSubscribeReq(Message message) throws InvalidMessageException {
        conversationId = message.getMetadata().getConversationId().orElse(null);
        clientId = message.getMetadata().getClientId().orElse(null);
        subscriptionId = message.getMetadata().asVirtualSubscriptionMetadata().getSubscriptionId().get();

        TypedPayload payload =
                new TypedPayload(SUBSCRIPTION_ENTITYID, message.getPayload().getJenaModel(), message.getPayload().usesInferencer());
        Map<PropertyID, String> assertions = payload.getAllDataPropertyAssertionsForEntityAsStrings(SUBSCRIPTION_ENTITYID);
        if (payload.getTypedEntities().isEmpty()) {
            throw new InvalidMessageException("Invalid VIRTUAL_SUBSCRIBE message: subscription is not defined.");
        } else if (payload.getTypedEntities().size() > 1) {
            throw new InvalidMessageException("Invalid VIRTUAL_SUBSCRIBE message: multiple subscriptions are not supported.");
        }
        EntityID subscriptionEntityId = payload.getTypedEntities().iterator().next();
        Map<PropertyID, String> propertiesMap = payload.getAllDataPropertyAssertionsForEntityAsStrings(subscriptionEntityId);
        targetDeviceId = propertiesMap.get(TARGET_DEVICE_ID_PROP);
        targetPlatformId = propertiesMap.get(TARGET_PLATFORM_ID_PROP);
        sourceDeviceId = propertiesMap.get(SOURCE_DEVICE_ID_PROP);
        sourcePlatformId = propertiesMap.get(SOURCE_PLATFORM_ID_PROP);
    }

    public Message toMessage() {
        Message message = new Message();
        VirtualSubscriptionMessageMetadata metadata = message.getMetadata().asVirtualSubscriptionMetadata();
        metadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.VIRTUAL_SUBSCRIBE);
        metadata.setClientId(clientId);
        metadata.setConversationId(conversationId);
        metadata.setSubscriptionId(subscriptionId);

        TypedPayload payload = new TypedPayload(SUBSCRIPTION_ENTITYID);
        message.setPayload(payload);

        EntityID subscriptionEntityId = new EntityID(targetDeviceId);
        payload.createTypedEntity(subscriptionEntityId);
        payload.addDataPropertyAssertionToEntity(subscriptionEntityId, TARGET_DEVICE_ID_PROP, targetDeviceId);
        payload.addDataPropertyAssertionToEntity(subscriptionEntityId, TARGET_PLATFORM_ID_PROP, targetPlatformId);
        payload.addDataPropertyAssertionToEntity(subscriptionEntityId, SOURCE_DEVICE_ID_PROP, sourceDeviceId);
        payload.addDataPropertyAssertionToEntity(subscriptionEntityId, SOURCE_PLATFORM_ID_PROP, sourcePlatformId);

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

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getTargetPlatformId() {
        return targetPlatformId;
    }

    public void setTargetPlatformId(String targetPlatformId) {
        this.targetPlatformId = targetPlatformId;
    }

    public String getSourceDeviceId() {
        return sourceDeviceId;
    }

    public void setSourceDeviceId(String sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }

    public String getSourcePlatformId() {
        return sourcePlatformId;
    }

    public void setSourcePlatformId(String sourcePlatformId) {
        this.sourcePlatformId = sourcePlatformId;
    }
}
