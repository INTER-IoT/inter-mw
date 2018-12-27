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

/**
 * Author: gasper
 * On: 5.6.2018
 */
public class Actuator {

    private String actuatorId;

    private String implementsProcedure;
    private String isHostedBy;
    private String hasSubsystem;
    private String madeActuation;
    private String forProperty;
    private String hasDeployment;

    public Actuator() {
    }

    public Actuator(String actuatorId) {
        this.actuatorId = actuatorId;
    }

    public String getActuatorId() {
        return actuatorId;
    }

    public void setActuatorId(String actuatorId) {
        this.actuatorId = actuatorId;
    }

    public String getImplementsProcedure() {
        return implementsProcedure;
    }

    public void setImplementsProcedure(String implementsProcedure) {
        this.implementsProcedure = implementsProcedure;
    }

    public String getIsHostedBy() {
        return isHostedBy;
    }

    public void setIsHostedBy(String isHostedBy) {
        this.isHostedBy = isHostedBy;
    }

    public String getHasSubsystem() {
        return hasSubsystem;
    }

    public void setHasSubsystem(String hasSubsystem) {
        this.hasSubsystem = hasSubsystem;
    }

    public String getMadeActuation() {
        return madeActuation;
    }

    public void setMadeActuation(String madeActuation) {
        this.madeActuation = madeActuation;
    }

    public String getForProperty() {
        return forProperty;
    }

    public void setForProperty(String forProperty) {
        this.forProperty = forProperty;
    }

    public String getHasDeployment() {
        return hasDeployment;
    }

    public void setHasDeployment(String hasDeployment) {
        this.hasDeployment = hasDeployment;
    }
}
