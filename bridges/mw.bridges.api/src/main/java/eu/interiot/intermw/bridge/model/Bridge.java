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
package eu.interiot.intermw.bridge.model;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.commons.ErrorReporter;
import eu.interiot.message.Message;

/**
 * A {@link Bridge} instance deals with communication with IoT platforms at the
 * level of middleware
 * <p>
 * One implementation per platform type should be provided
 * <p>
 * The platform type supported by a {@link Bridge} instance is specified
 * by means of {@link eu.interiot.intermw.bridge.annotations.Bridge#platformType()}
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a> * @author
 * <a href="mailto:mllorente@prodevelop.es">Miguel A. Llorente</a>
 */
public interface Bridge {

    void setPublisher(Publisher<Message> publisher);

    void setErrorReporter(ErrorReporter errorReporter);

    void process(Message message) throws BridgeException;
}
