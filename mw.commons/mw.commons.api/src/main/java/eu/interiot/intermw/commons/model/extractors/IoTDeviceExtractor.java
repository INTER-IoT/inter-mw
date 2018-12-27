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
package eu.interiot.intermw.commons.model.extractors;

import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.payload.GOIoTPPayload;
import eu.interiot.message.payload.types.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gasper Vrhovsek
 */
public class IoTDeviceExtractor {
    static final String DEVICE_TYPE_PROPERTY_ID = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public static IoTDevicePayload toIoTDevicePayload(List<IoTDevice> devices) {

        // TODO support ACTUATOR and SENSOR properties

        IoTDevicePayload ioTDevicePayload = new IoTDevicePayload();
        for (IoTDevice device : devices) {
            EntityID entityID = new EntityID(device.getDeviceId());
            ioTDevicePayload.createIoTDevice(entityID);
            ioTDevicePayload.setHasName(entityID, device.getName());
            ioTDevicePayload.setHasLocation(entityID, new EntityID(device.getLocation()));
            ioTDevicePayload.setIsHostedBy(entityID, new EntityID(device.getHostedBy()));

            // Device types mapping and setting as a new property
            PropertyID propertyId = new PropertyID(DEVICE_TYPE_PROPERTY_ID);

            if (device.getDeviceTypes() == null || device.getDeviceTypes().isEmpty()) {
                ioTDevicePayload.addDataPropertyAssertionToEntity(entityID, propertyId, IoTDeviceType.DEVICE.getDeviceTypeUri());
            } else {
                for (IoTDeviceType type : device.getDeviceTypes()) {
                    ioTDevicePayload.addDataPropertyAssertionToEntity(entityID, propertyId, type.getDeviceTypeUri());
                }
            }
        }

        return ioTDevicePayload;
    }

    public static List<IoTDevice> fromIoTDevicePayload(IoTDevicePayload ioTDevicePayload) {
        Set<EntityID> ioTDevices = ioTDevicePayload.getIoTDevices();
        return ioTDevices.stream().map((EntityID entityID) -> {
            EnumSet<IoTDeviceType> deviceTypes = EnumSet.of(IoTDeviceType.DEVICE);

            // Basic properties
            String hasName = ioTDevicePayload.getHasName(entityID).orElse(null);
            EntityID hasLocation = ioTDevicePayload.getHasLocation(entityID).orElse(null);
            EntityID isHostedBy = ioTDevicePayload.getIsHostedBy(entityID).orElse(null);
            PlatformPayload platformPayload = ioTDevicePayload.asPlatformPayload();
            if (isHostedBy == null) {
                Set<EntityID> isHostedBySet = ioTDevicePayload.asSystemPayload().getIsHostedBy(entityID);
                if (isHostedBySet != null && !isHostedBySet.isEmpty()) {
                    EntityID hostedByEntityId = isHostedBySet.iterator().next();

                    if (hostedByEntityId.isUnique()) {
                        isHostedBy = hostedByEntityId;
                    } else {
                        GOIoTPPayload goIoTPPayload = ioTDevicePayload.asGOIoTPPayload();
                        Set<EntityID> hostedBy = goIoTPPayload.getEntityTypes(hostedByEntityId);
                        if (hostedBy != null && !hostedBy.isEmpty()) {
                            isHostedBy = hostedBy.iterator().next();
                        }
                    }
                }
            }

            IoTDevice device = new IoTDevice(
                    entityID.toString(),
                    hasName,
                    isHostedBy != null ? isHostedBy.toString() : null,
                    hasLocation != null ? hasLocation.toString() : null,
                    deviceTypes);

            // Device hosts other devices
            Set<EntityID> hosts = platformPayload.getHosts(entityID);
            device.setHosts(hosts.stream().map(hostsEntityID -> hostsEntityID != null ?
                    hostsEntityID.toString() : null).collect(Collectors.toList()));

            // Device is an actuator
            ActuatorPayload actuatorPayload = ioTDevicePayload.asActuatorPayload();
            Set<EntityID> forPropertySet = actuatorPayload.getForProperty(entityID);
            if (!forPropertySet.isEmpty()) {
                EntityID forProperty = forPropertySet.iterator().next();
                if (forProperty != null) {
                    device.addForProperty(forProperty.toString());
                    device.addDeviceType(IoTDeviceType.ACTUATOR);
                }
            }
            ActuationPayload actuationPayload = ioTDevicePayload.asActuationPayload();
            Set<EntityID> actsOnPropertySet = actuationPayload.getActsOnProperty(entityID);
            if (!actsOnPropertySet.isEmpty()) {
                EntityID actsOnProperty = actsOnPropertySet.iterator().next();
                if (actsOnProperty != null) {
                    device.addDeviceType(IoTDeviceType.ACTUATOR);
                    if (actsOnProperty.isUnique()) {
                        device.addForProperty(actsOnProperty.toString());
                    } else {
                        GOIoTPPayload goIoTPPayload = ioTDevicePayload.asGOIoTPPayload();
                        ActuatablePropertyPayload actuatablePropertyPayload = goIoTPPayload.asActuatablePropertyPayload();
                        Set<String> isPropertyOf = actuatablePropertyPayload.getIsPropertyOf(actsOnProperty).stream().map(EntityID::toString).collect(Collectors.toSet());
                        if (isPropertyOf.isEmpty()) {
                            device.setForProperty(
                                    goIoTPPayload.getEntityTypes(actsOnProperty).stream().map(
                                            EntityID::toString).collect(Collectors.toSet()));

                        } else {
                            device.setForProperty(isPropertyOf);
                        }
                    }
                }
            }

            // Device is a sensor
            SensorPayload sensorPayload = ioTDevicePayload.asSensorPayload();
            Set<EntityID> observesSet = sensorPayload.getObserves(entityID);
            if (!observesSet.isEmpty()) {
                EntityID observes = observesSet.iterator().next();
                if (observes != null) {
                    device.addDeviceType(IoTDeviceType.SENSOR);
                    if (observes.isUnique()) {
                        device.addObserves(observes.toString());
                    } else {
                        GOIoTPPayload goIoTPPayload = ioTDevicePayload.asGOIoTPPayload();
                        device.setObserves(goIoTPPayload.getEntityTypes(observes).stream().map(
                                EntityID::toString).collect(Collectors.toSet()));
                    }
                }
            }
            ObservationPayload observationPayload = ioTDevicePayload.asObservationPayload();
            EntityID observation = observationPayload.getObservedProperty(entityID).orElse(null);
            if (observation != null) {
                device.addDeviceType(IoTDeviceType.SENSOR);
                if (observation.isUnique()) {
                    device.addObserves(observation.toString());
                } else {
                    GOIoTPPayload goIoTPPayload = ioTDevicePayload.asGOIoTPPayload();
                    ObservablePropertyPayload observablePropertyPayload = goIoTPPayload.asObservablePropertyPayload();
                    Set<String> observes = observablePropertyPayload.getIsPropertyOf(observation).stream().map(EntityID::toString).collect(Collectors.toSet());
                    device.setObserves(observes);
                }
            }

            // If device types are contained in the custom property, add them
            PropertyID propertyId = new PropertyID(DEVICE_TYPE_PROPERTY_ID);
            Set<Object> typesArray = ioTDevicePayload.getAllDataPropertyAssertionsForEntity(entityID, propertyId);
            if (!typesArray.isEmpty()) {
                for (Object type : typesArray) {
                    device.addDeviceType(IoTDeviceType.fromDeviceTypeUri((String) type));
                }
            } else {
                Set<EntityID> typesEntityIdArray = ioTDevicePayload.getAllObjectPropertyAssertionsForEntity(entityID, propertyId);
                for (EntityID typeEntityId : typesEntityIdArray) {
                    IoTDeviceType ioTDeviceType = IoTDeviceType.fromDeviceTypeUri(typeEntityId.toString());
                    if (ioTDeviceType != null) {
                        device.addDeviceType(ioTDeviceType);
                    }
                }
            }


            return device;
        }).collect(Collectors.toList());
    }
}
