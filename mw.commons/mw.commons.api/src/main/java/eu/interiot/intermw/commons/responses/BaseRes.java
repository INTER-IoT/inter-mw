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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.util.Set;

/**
 * @author Gasper Vrhovsek
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseRes {
    private Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypes;
    private String senderPlatform;
    private String conversationId;

    public BaseRes() {
    }

    public BaseRes(Message message) {
        this.messageTypes = message.getMetadata().getMessageTypes();
        this.conversationId = message.getMetadata().printConversationId();
        this.senderPlatform = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().isPresent() ?
                message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString() :
                null;
    }

    public Set<URIManagerMessageMetadata.MessageTypesEnum> getMessageTypes() {
        return messageTypes;
    }

    public String getSenderPlatform() {
        return senderPlatform;
    }

    public String getConversationId() {
        return conversationId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("messageTypes", messageTypes)
                .append("senderPlatform", senderPlatform)
                .append("conversationId", conversationId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BaseRes baseRes = (BaseRes) o;

        return new EqualsBuilder()
                .append(messageTypes, baseRes.messageTypes)
                .append(senderPlatform, baseRes.senderPlatform)
                .append(conversationId, baseRes.conversationId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(messageTypes)
                .append(senderPlatform)
                .append(conversationId)
                .toHashCode();
    }

    public static BaseRes fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, BaseRes.class);
    }
}
