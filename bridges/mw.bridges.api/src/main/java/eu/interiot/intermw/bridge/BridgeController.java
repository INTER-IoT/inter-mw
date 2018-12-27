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
package eu.interiot.intermw.bridge;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.model.Bridge;
import eu.interiot.intermw.comm.broker.annotations.Broker;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.requests.RegisterPlatformReq;

/**
 * This interface deals with connections to the IoT {@link RegisterPlatformReq ) subscriptions and
 * the {@link Broker} subscriptions to receive information from the platform
 *
 * Internally contains an instance of {@link Bridge} that is used to make the
 * calls to the methods of the IoT platform
 *
 * Instances of this interface must be annotated with the
 * {@link eu.interiot.intermw.bridge.annotations.BridgeController} annotation to
 * be automatically loaded by the {@link BridgeContext }
 *
 * @author <a href="mailto:mllorente@prodevelop.es">Miguel A. Llorente</a>
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface BridgeController {

    /**
     * This method prepares {@link Broker} subscriptions to the list of
     * {@link eu.interiot.intermw.commons.model.enums.Actions} and makes the necessary calls to the {@link Bridge}
     *
     * @throws BridgeException
     * @throws BrokerException
     */
    public void setUpBrokerListeners() throws BridgeException, BrokerException;

    /**
     * Frees resources: Closes connections of the Bridge to the IoT platform,
     * removes subscribers, publishers, etc.
     *
     * @throws BridgeException
     * @throws BrokerException
     */
    public void destroy() throws BridgeException, BrokerException;

    /**
     * The {@link Bridge} attached to this instance of the
     * {@link BridgeController}
     *
     * @return A {@link Bridge} instance
     */
    public Bridge getBridge();
}
