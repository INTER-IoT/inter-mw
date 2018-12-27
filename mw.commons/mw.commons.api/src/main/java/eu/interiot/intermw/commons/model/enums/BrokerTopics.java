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
package eu.interiot.intermw.commons.model.enums;

/**
 * A {@link Enum} to have the topic names for the bridges communications
 *
 * Topic names for communication among different components. They reflect flows
 * defined in the architecture diagram. The first segment of the name is the
 * publishing component, the second is the listening component
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public enum BrokerTopics {

    /**
     * Global error topic
     */
    ERROR("error"),
    /**
     * PRM-RD Platform request manager to Services
     */
    PRM_SRM("prm_srm"),
    /**
     * PRM-RD Platform request manager to Services
     */
    SRM_PRM("srm_prm"),
    /**
     * PRM-ARM Platform request manager to API Request Manager
     */
    PRM_ARM("prm_arm"),
    /**
     * IPSMRM-Bx IPSMRM to Bridge X. To this topic name the platform ID is appended
     * to create a unique topic.
     */
    IPSMRM_BRIDGE("ipsmrm_bridge"),
    /**
     * Bx-IPSMRM Bridge X to IPSMRM To this topic name the platform ID is appended
     * to create a unique topic.
     */
    BRIDGE_IPSMRM("bridge_ipsmrm"),
    /**
     * ARM-PRM API Request Manager to Platform request manager
     */
    ARM_PRM("arm_prm"),

    /**
     * IPSMRM-PRM IPSM Request Manager to Platform request manager
     */
    IPSMRM_PRM("ipsmrm_prm"),

    /**
     * PRM-IPSMRM Platform request manager to IPSM Request Manager
     */
    PRM_IPSMRM("prm_ipsmrm"),

    /**
     * BridgeControllerEmulator topic Listen for messages from the outer
     * universe
     */
    BRIDGE_EMULATOR("bridge_controller_emulator_topic"),

    /**
     * Topic for client callback messages (REST)
     */
    REST_API("rest_api"),

    /**
     * Examples topic Listen for messages from the outer universe
     */
    DEFAULT("default");

    private final String topicName;

    public String getTopicName() {
        return topicName;
    }

    private BrokerTopics(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName(String platformId) {
        return topicName + "_" + platformId.replaceAll("[:/#]+", "_");
    }
}
