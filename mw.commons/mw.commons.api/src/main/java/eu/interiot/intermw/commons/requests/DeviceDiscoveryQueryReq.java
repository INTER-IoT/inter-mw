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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.commons.model.IoTDeviceFilter;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.QueryMessageMetadata;

import java.io.IOException;

/**
 * @author Gasper Vrhovsek
 */
public class DeviceDiscoveryQueryReq {
    private String clientId;
    private IoTDeviceFilter query;
    private String sparqlQuery;

    public DeviceDiscoveryQueryReq(String clientId, IoTDeviceFilter query) {
        this.clientId = clientId;
        this.query = query;
    }

    public DeviceDiscoveryQueryReq(String clientId, String deviceDiscoveryQueryJson) throws IOException {
        this.clientId = clientId;
        ObjectMapper om = new ObjectMapper();
        this.query = om.readValue(deviceDiscoveryQueryJson, IoTDeviceFilter.class);
    }

    public Message toMessage(String conversationId) throws JsonProcessingException {
        Message message = new Message();
        MessageMetadata messageMetadata = message.getMetadata();

        messageMetadata.setClientId(clientId);
        messageMetadata.setConversationId(conversationId);
        messageMetadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_DISCOVERY_QUERY);

        QueryMessageMetadata queryMessageMetadata = messageMetadata.asQueryMetadata();

        ObjectMapper om = new ObjectMapper();
        String queryString = om.writeValueAsString(query);
        queryMessageMetadata.setQueryString(queryString);

        return message;
    }

    public String getClientId() {
        return clientId;
    }

    public IoTDeviceFilter getQuery() {
        return query;
    }
}
