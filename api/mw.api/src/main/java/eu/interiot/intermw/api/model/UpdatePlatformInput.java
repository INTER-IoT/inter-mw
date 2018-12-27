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

public class UpdatePlatformInput {

    private String baseEndpoint;
    private String location;
    private String name;
    private String username;
    private String encryptedPassword;
    private String encryptionAlgorithm;

    private String downstreamInputAlignmentName;
    private String downstreamInputAlignmentVersion;
    private String downstreamOutputAlignmentName;
    private String downstreamOutputAlignmentVersion;

    private String upstreamInputAlignmentName;
    private String upstreamInputAlignmentVersion;
    private String upstreamOutputAlignmentName;
    private String upstreamOutputAlignmentVersion;


    public UpdatePlatformInput() {
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getDownstreamInputAlignmentName() {
        return downstreamInputAlignmentName;
    }

    public void setDownstreamInputAlignmentName(String downstreamInputAlignmentName) {
        this.downstreamInputAlignmentName = downstreamInputAlignmentName;
    }

    public String getDownstreamInputAlignmentVersion() {
        return downstreamInputAlignmentVersion;
    }

    public void setDownstreamInputAlignmentVersion(String downstreamInputAlignmentVersion) {
        this.downstreamInputAlignmentVersion = downstreamInputAlignmentVersion;
    }

    public String getDownstreamOutputAlignmentName() {
        return downstreamOutputAlignmentName;
    }

    public void setDownstreamOutputAlignmentName(String downstreamOutputAlignmentName) {
        this.downstreamOutputAlignmentName = downstreamOutputAlignmentName;
    }

    public String getDownstreamOutputAlignmentVersion() {
        return downstreamOutputAlignmentVersion;
    }

    public void setDownstreamOutputAlignmentVersion(String downstreamOutputAlignmentVersion) {
        this.downstreamOutputAlignmentVersion = downstreamOutputAlignmentVersion;
    }

    public String getUpstreamInputAlignmentName() {
        return upstreamInputAlignmentName;
    }

    public void setUpstreamInputAlignmentName(String upstreamInputAlignmentName) {
        this.upstreamInputAlignmentName = upstreamInputAlignmentName;
    }

    public String getUpstreamInputAlignmentVersion() {
        return upstreamInputAlignmentVersion;
    }

    public void setUpstreamInputAlignmentVersion(String upstreamInputAlignmentVersion) {
        this.upstreamInputAlignmentVersion = upstreamInputAlignmentVersion;
    }

    public String getUpstreamOutputAlignmentName() {
        return upstreamOutputAlignmentName;
    }

    public void setUpstreamOutputAlignmentName(String upstreamOutputAlignmentName) {
        this.upstreamOutputAlignmentName = upstreamOutputAlignmentName;
    }

    public String getUpstreamOutputAlignmentVersion() {
        return upstreamOutputAlignmentVersion;
    }

    public void setUpstreamOutputAlignmentVersion(String upstreamOutputAlignmentVersion) {
        this.upstreamOutputAlignmentVersion = upstreamOutputAlignmentVersion;
    }
}
