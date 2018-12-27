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

import com.google.common.base.Strings;
import eu.interiot.intermw.commons.exceptions.InvalidMessageException;
import eu.interiot.intermw.commons.model.Actuation;
import eu.interiot.intermw.commons.model.ActuationResult;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.payload.types.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: gasper
 * On: 28.5.2018
 */
public class ActuationReq {

    // TODO remove when new version of IDManagerGOIoTPex
    //        IDManagerGOIoTPex.PROPERTIES.OBJECT....
    private static final String GOIOTPEX_HAS_LOCAL_ID = "http://inter-iot.eu/GOIoTPex#hasLocalID";

    private Actuation actuation;

    public ActuationReq(Actuation actuation) {
        this.actuation = actuation;
    }

    public ActuationReq(Message message) throws InvalidMessageException {
        actuation = new Actuation();
        actuation.setClientId(message.getMetadata().getClientId().orElse(null));
        actuation.setPlatformId(message.getMetadata().asPlatformMessageMetadata().getReceivingPlatformIDs().iterator().next().toString());

        ActuationPayload payload = message.getPayloadAsGOIoTPPayload().asActuationPayload();

        if (!payload.getActuations().iterator().hasNext()) {
            throw new InvalidMessageException("ActuationPayload does not contain actuations.");
        }

        ActuatorPayload actuatorPayload = payload.asActuatorPayload();
        if (!actuatorPayload.getActuators().iterator().hasNext()) {
            throw new InvalidMessageException("ActuationPayload does not contain any ACTUATOR.");
        }
        EntityID actuatorID = actuatorPayload.getActuators().iterator().next();
        actuation.setMadeByActuator(actuatorID.toString());

        String actuatorLocalId = actuatorPayload.getAllDataPropertyAssertionsForEntityAsStrings(actuatorID,
                new PropertyID(GOIOTPEX_HAS_LOCAL_ID)).iterator().hasNext() ?
                actuatorPayload.getAllDataPropertyAssertionsForEntityAsStrings(actuatorID,
                        new PropertyID(GOIOTPEX_HAS_LOCAL_ID)).iterator().next() : null;
        actuation.setMadeByActuatorLocalId(actuatorLocalId);

        IoTDevicePayload ioTDevicePayload = payload.asIoTDevicePayload();
        actuation.setDeviceId(ioTDevicePayload.getIoTDevices().iterator().next().toString());

        ResultPayload resultPayload = payload.asResultPayload();

        Set<ActuationResult> actuationResults = new HashSet<>();
        for (EntityID resultEntityID : resultPayload.getResults()) {
            String resultValue = resultPayload.getHasResultValue(resultEntityID).orElse(null);
            String resultUnit = resultPayload.getHasUnit(resultEntityID).orElse(null).toString();
            String actuableProperty = resultPayload.asActuatorPayload().getForProperty(resultEntityID).iterator().next().toString();

            actuationResults.add(new ActuationResult(actuableProperty, resultValue, resultUnit));
        }
        actuation.setActuationResults(actuationResults);
    }

    public Message toMessage(String conversationId) {

        if (Strings.isNullOrEmpty(actuation.getClientId())) {
            throw new IllegalArgumentException("actuation.clientId is null.");
        }
        if (Strings.isNullOrEmpty(actuation.getMadeByActuator())) {
            throw new IllegalArgumentException("actuation.madeByActuator is null.");
        }
        if (Strings.isNullOrEmpty(actuation.getPlatformId())) {
            throw new IllegalArgumentException("actuation.platformId is null.");
        }
        if (actuation.getActuationResults().isEmpty()) {
            throw new IllegalArgumentException("actuation.actuationResults is empty.");
        }

        ActuationPayload actuationPayload = new ActuationPayload();

        EntityID actuationID = new EntityID();
        EntityID actuatorID = new EntityID(actuation.getMadeByActuator());

        // Create ACTUATION
        actuationPayload.createActuation(actuationID);

        ResultPayload resultPayload = actuationPayload.asResultPayload();
        for (ActuationResult actuationResult : actuation.getActuationResults()) {
            EntityID resultEntityID = new EntityID();
            EntityID actuablePropertyID = new EntityID(actuationResult.getActuatableProperty());

            resultPayload.createResult(resultEntityID);
            resultPayload.setHasResultValue(resultEntityID, actuationResult.getValue());
            resultPayload.setHasUnit(resultEntityID, new EntityID(actuationResult.getUnit()));
            // FOR PROPERTY in the RESULT links to which property a certain result is linked
            resultPayload.asActuatorPayload().setForProperty(resultEntityID, actuablePropertyID);

            actuationPayload.addHasResult(actuationID, resultEntityID);

            // Actuatable properties
            ActuatablePropertyPayload actuatablePropertyPayload = actuationPayload.asActuatablePropertyPayload();
            actuatablePropertyPayload.createActuatableProperty(actuablePropertyID);
            actuationPayload.addActsOnProperty(actuationID, actuablePropertyID);
        }

        // set actuator entity
        ActuatorPayload actuatorPayload = actuationPayload.asActuatorPayload();
        actuatorPayload.createActuator(actuatorID);
        actuatorPayload.addMadeActuation(actuatorID, actuationID);

        if (!Strings.isNullOrEmpty(actuation.getMadeByActuatorLocalId())) {
            actuatorPayload.addDataPropertyAssertionToEntity(
                    actuatorID,
                    new PropertyID(GOIOTPEX_HAS_LOCAL_ID),
                    actuation.getMadeByActuatorLocalId());
        }

        IoTDevicePayload ioTDevicePayload = actuationPayload.asIoTDevicePayload();
        EntityID ioTDeviceID = new EntityID(actuation.getDeviceId());
        ioTDevicePayload.createIoTDevice(ioTDeviceID);

        if (!actuation.getDeviceId().equals(actuation.getMadeByActuator())) {
            PlatformPayload devicePlatformPayload = ioTDevicePayload.asPlatformPayload();
            devicePlatformPayload.setHosts(ioTDeviceID, actuatorID);
            SystemPayload deviceSystemPayload = ioTDevicePayload.asSystemPayload();
            deviceSystemPayload.createSystem(ioTDeviceID);
        }

        Message message = new Message();
        MessageMetadata metadata = message.getMetadata();
        metadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.ACTUATION);
        metadata.setConversationId(conversationId);
        metadata.setClientId(actuation.getClientId());
        metadata.asPlatformMessageMetadata().addReceivingPlatformID(new EntityID(actuation.getPlatformId()));
        message.setPayload(actuationPayload);

        return message;
    }

    public Actuation getActuation() {
        return actuation;
    }

    public void setActuation(Actuation actuation) {
        this.actuation = actuation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ActuationReq that = (ActuationReq) o;

        return new EqualsBuilder()
                .append(actuation, that.actuation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(actuation)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("actuation", actuation)
                .toString();
    }
}
