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

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gasper Vrhovsek
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IoTDeviceFilter implements Serializable {

    // basic IoTDevice fields
    private List<IoTDeviceType> deviceTypes;
    private String hostedBy;
    private String location;
    private String name;

    // additional fields
    private String type;
    private String localId;


    public IoTDeviceFilter() {
        super();
    }

    public List<IoTDeviceType> getDeviceTypes() {
        return deviceTypes;
    }

    public void addDeviceType(IoTDeviceType deviceType) {
        if (this.deviceTypes == null) {
            this.deviceTypes = new ArrayList<>();
        }
        this.deviceTypes.add(deviceType);
    }

    public void setDeviceTypes(List<IoTDeviceType> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public String getHostedBy() {
        return hostedBy;
    }

    public void setHostedBy(String hostedBy) {
        this.hostedBy = hostedBy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String buildConditions() {

        StringBuilder sb = new StringBuilder();

        if (deviceTypes != null && !deviceTypes.isEmpty()) {
            for (IoTDeviceType deviceType : deviceTypes) {
                sb.append("  rdf:type <").append(deviceType.getDeviceTypeUri()).append(">;\n");
            }
        }

        if (StringUtils.isNotBlank(hostedBy)) {
            sb.append("  sosa:isHostedBy <").append(hostedBy).append(">;\n");
        }

        if (StringUtils.isNotBlank(location)) {
            // not literal
            sb.append("  iiot:hasLocation <").append(location).append(">;\n");
        }

        if (StringUtils.isNotBlank(name)) {
            // literal
            sb.append("  iiot:hasName \"").append(name).append("\";\n");
        }

        if (StringUtils.isNotBlank(type)) {
            sb.append("  rdf:type <").append(type).append(">;\n");
        }

        if (StringUtils.isNotBlank(localId)) {
            sb.append("  iiot:hasLocalId \"").append(localId).append("\";\n");
        }

        return sb.toString();
    }
}
