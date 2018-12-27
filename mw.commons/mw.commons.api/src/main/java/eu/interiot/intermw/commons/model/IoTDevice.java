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

import com.google.common.base.Objects;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import java.util.*;

public class IoTDevice {

    /* COMMON PROPERTIES */
    private EnumSet<IoTDeviceType> deviceTypes;

    private String deviceId;
    private String hostedBy;
    private String location;
    private String name;

    /* Other IoTDevices that can be hosted by another IoTDevice
     * For instance one device has multiple sensors hosted, this
     * is where their ids would be saved
     * */
    private List<String> hosts; // List<IoTDevice> hosts <-- ??

    /* ACTUATOR PROPERTIES */
    private Set<String> forProperty; // sosa:ActuableProperty
    private String madeActuation; // optional, sosa:Actuation
    private String implementsProcedure; // sosa:Procedure

    /* SENSOR PROPERTIES */
    private Set<String> observes; // sosa:ObservableProperty
    private String detects; // ssn:Stimulus
    private String madeObservation; // sosa:Observation

    public IoTDevice() {
        this.hosts = new ArrayList<>();
        this.forProperty = new HashSet<>();
        this.observes = new HashSet<>();
        this.deviceTypes = EnumSet.noneOf(IoTDeviceType.class);
    }

    public IoTDevice(@Nonnull String deviceId, String name, String hostedBy, String location, EnumSet<IoTDeviceType> deviceTypes) {
        this();
        this.deviceId = deviceId;
        this.name = name;
        this.hostedBy = hostedBy;
        this.location = location;
        if (deviceTypes != null) {
            this.deviceTypes = deviceTypes;
        }
    }

    public IoTDevice(@Nonnull String deviceId, EnumSet<IoTDeviceType> deviceTypes) {
        this(deviceId, null, null, null, deviceTypes);
    }

    public IoTDevice(@Nonnull String deviceId) {
        this(deviceId, null, null, null, null);
    }

    public EnumSet<IoTDeviceType> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(EnumSet<IoTDeviceType> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public void addDeviceType(IoTDeviceType deviceType) {
        if (!this.deviceTypes.contains(deviceType))
            this.deviceTypes.add(deviceType);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public void addHosts(String hosts) {
        if (hosts != null && !this.hosts.contains(hosts))
            this.hosts.add(hosts);
    }

    public Set<String> getForProperty() {
        return forProperty;
    }

    public void addForProperty(String forProperty) {
        if (forProperty != null) {
            this.forProperty.add(forProperty);
        }
    }

    public void setForProperty(Set<String> forProperty) {
        this.forProperty = forProperty;
    }

    public String getMadeActuation() {
        return madeActuation;
    }

    public void setMadeActuation(String madeActuation) {
        this.madeActuation = madeActuation;
    }

    public String getImplementsProcedure() {
        return implementsProcedure;
    }

    public void setImplementsProcedure(String implementsProcedure) {
        this.implementsProcedure = implementsProcedure;
    }

    public Set<String> getObserves() {
        return observes;
    }

    public void addObserves(String observes) {
        if (observes != null) {
            this.observes.add(observes);
        }
    }

    public void setObserves(Set<String> observes) {
        this.observes = observes;
    }

    public String getDetects() {
        return detects;
    }

    public void setDetects(String detects) {
        this.detects = detects;
    }

    public String getMadeObservation() {
        return madeObservation;
    }

    public void setMadeObservation(String madeObservation) {
        this.madeObservation = madeObservation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IoTDevice ioTDevice = (IoTDevice) o;
        return Objects.equal(deviceTypes, ioTDevice.deviceTypes) &&
                Objects.equal(deviceId, ioTDevice.deviceId) &&
                Objects.equal(hostedBy, ioTDevice.hostedBy) &&
                Objects.equal(location, ioTDevice.location) &&
                Objects.equal(name, ioTDevice.name) &&
                Objects.equal(hosts, ioTDevice.hosts) &&
                Objects.equal(forProperty, ioTDevice.forProperty) &&
                Objects.equal(madeActuation, ioTDevice.madeActuation) &&
                Objects.equal(implementsProcedure, ioTDevice.implementsProcedure) &&
                Objects.equal(observes, ioTDevice.observes) &&
                Objects.equal(detects, ioTDevice.detects) &&
                Objects.equal(madeObservation, ioTDevice.madeObservation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceTypes, deviceId, hostedBy, location, name, hosts, forProperty, madeActuation, implementsProcedure, observes, detects, madeObservation);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("deviceTypes", deviceTypes)
                .append("deviceId", deviceId)
                .append("hostedBy", hostedBy)
                .append("location", location)
                .append("name", name)
                .append("hosts", hosts)
                .append("forProperty", forProperty)
                .append("madeActuation", madeActuation)
                .append("implementsProcedure", implementsProcedure)
                .append("observes", observes)
                .append("detects", detects)
                .append("madeObservation", madeObservation)
                .toString();
    }
}
