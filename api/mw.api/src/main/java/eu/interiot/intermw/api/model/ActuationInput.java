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
package eu.interiot.intermw.api.model;

import eu.interiot.intermw.commons.model.ActuationResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Gasper Vrhovsek
 */
@ApiModel(value = "ActuationInput", description = "Actuation POST request payload.")
public class ActuationInput implements Serializable {
    private String actuatorId;
    private String actuatorLocalId;
    private Set<ActuationResult> actuationResultSet;

    @ApiModelProperty(value = "Actuator ID, example: http://inter-iot.eu/light/L01", dataType = "String", required = true)
    public String getActuatorId() {
        return actuatorId;
    }

    public void setActuatorId(String actuatorId) {
        this.actuatorId = actuatorId;
    }

    @ApiModelProperty(value = "Actuator local ID. This is not an unique identifier, but rather an identifier inside of a platform example: lights", dataType = "String", required = false)
    public String getActuatorLocalId() {
        return actuatorLocalId;
    }

    public void setActuatorLocalId(String actuatorLocalId) {
        this.actuatorLocalId = actuatorLocalId;
    }

    @ApiModelProperty(value = "Actuation results, consist of actuation value and actuation unit", required = true)
    public Set<ActuationResult> getActuationResultSet() {
        return actuationResultSet;
    }

    public void setActuationResultSet(Set<ActuationResult> actuationResultSet) {
        this.actuationResultSet = actuationResultSet;
    }
}
