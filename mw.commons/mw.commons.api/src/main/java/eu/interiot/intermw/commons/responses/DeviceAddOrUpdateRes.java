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

import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.extractors.IoTDeviceExtractor;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.payload.GOIoTPPayload;
import eu.interiot.message.payload.types.IoTDevicePayload;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Gasper Vrhovsek
 */
public class DeviceAddOrUpdateRes {

    private IoTDevice device;

    public DeviceAddOrUpdateRes() {
    }

    public DeviceAddOrUpdateRes(Message message) {
        GOIoTPPayload payload = message.getPayloadAsGOIoTPPayload();

        IoTDevicePayload ioTDevicePayload = payload.asIoTDevicePayload();
        List<IoTDevice> ioTDevices = IoTDeviceExtractor.fromIoTDevicePayload(ioTDevicePayload);

        if (ioTDevices.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("%s message must contain one IoTDevice.",
                            URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE.name())
            );
        }
        if (ioTDevices.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("%s message can only contain one IoTDevice.",
                            URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE.name())
            );
        }

        device = ioTDevices.get(0);
    }

    public IoTDevice getDevice() {
        return device;
    }

    public void setDevice(IoTDevice device) {
        this.device = device;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof DeviceAddOrUpdateRes)) return false;

        DeviceAddOrUpdateRes that = (DeviceAddOrUpdateRes) o;

        return new EqualsBuilder()
                .append(device, that.device)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(device)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("device", device)
                .toString();
    }
}
