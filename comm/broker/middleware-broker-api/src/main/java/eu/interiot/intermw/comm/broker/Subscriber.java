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
package eu.interiot.intermw.comm.broker;

import eu.interiot.intermw.comm.broker.exceptions.BrokerException;

/**
 * A {@link Service} for subscribing to messages published by a
 * {@link Publisher}
 *
 * @param <M> The messages class this {@link Service} works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Subscriber<M> extends Service<M> {

    /**
     * Subscribes to the messages of a {@link Topic}
     *
     * @param listener A {@link Listener} instance to handle the messages as they
     *                 arrive
     * @throws BrokerException
     */
    public void subscribe(Listener<M> listener) throws BrokerException;

    /**
     * Subscribes to a the messages of a {@link Topic} creating a specific
     * message queue
     *
     * This API method depends on the support of the specific message bus
     * implementation. For some implementations, the <queue> parameter could
     * have no effect
     *
     * @param listener A {@link Listener} instance to handle the messages as they
     *                 arrive
     * @param queue    The {@link Queue} configuration. This parameter depends on the
     *                 support of the specific message bus implementation
     * @throws BrokerException
     */
    public void subscribe(Listener<M> listener, Queue queue) throws BrokerException;
}
