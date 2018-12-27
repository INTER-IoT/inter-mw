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
package eu.interiot.intermw.commons.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: gasper
 * On: 28.5.2018
 */
public class Actuation {

    private String clientId;
    private String platformId;

    private String madeByActuator;
    private String madeByActuatorLocalId;

    private String deviceId;

    private Set<ActuationResult> actuationResults;

    public Actuation() {
        this.actuationResults = new HashSet<>();
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

    public String getMadeByActuator() {
        return madeByActuator;
    }

    public void setMadeByActuator(String madeByActuator) {
        this.madeByActuator = madeByActuator;
    }

    public String getMadeByActuatorLocalId() {
        return madeByActuatorLocalId;
    }

    public void setMadeByActuatorLocalId(String madeByActuatorLocalId) {
        this.madeByActuatorLocalId = madeByActuatorLocalId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Set<ActuationResult> getActuationResults() {
        return actuationResults;
    }

    public void setActuationResults(Set<ActuationResult> actuationResults) {
        this.actuationResults = actuationResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Actuation actuation = (Actuation) o;

        return new EqualsBuilder()
                .append(clientId, actuation.clientId)
                .append(platformId, actuation.platformId)
                .append(madeByActuator, actuation.madeByActuator)
                .append(madeByActuatorLocalId, actuation.madeByActuatorLocalId)
                .append(deviceId, actuation.deviceId)
                .append(actuationResults, actuation.actuationResults)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(clientId)
                .append(platformId)
                .append(madeByActuator)
                .append(madeByActuatorLocalId)
                .append(deviceId)
                .append(actuationResults)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("clientId", clientId)
                .append("platformId", platformId)
                .append("madeByActuator", madeByActuator)
                .append("madeByActuatorLocalId", madeByActuatorLocalId)
                .append("deviceId", deviceId)
                .append("actuationResults", actuationResults)
                .toString();
    }
}
