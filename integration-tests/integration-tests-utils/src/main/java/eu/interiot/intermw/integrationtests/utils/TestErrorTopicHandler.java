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
package eu.interiot.intermw.integrationtests.utils;

import eu.interiot.intermw.comm.control.ControlComponent;
import eu.interiot.intermw.comm.control.abstracts.AbstractControlComponent;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestErrorTopicHandler extends AbstractControlComponent {
    private static final Logger logger = LoggerFactory.getLogger(TestErrorTopicHandler.class);
    private int numberOfErrorMessages;

    /**
     * The constructor. Creates a new instance of {@link ControlComponent}
     *
     * @throws MiddlewareException
     */
    public TestErrorTopicHandler() throws MiddlewareException {
        setUpListener();
    }

    public void setUpListener() throws MiddlewareException {

        subscribe(BrokerTopics.ERROR.getTopicName(), message -> {
            try {
                logger.info("New message received from ERROR topic:\n" + message.serializeToJSONLD());
                numberOfErrorMessages++;

            } catch (Exception e) {
                logger.error("Failed to handle error message: " + e.getMessage(), e);
            }

        }, Message.class);
    }

    public int getNumberOfErrorMessages() {
        return numberOfErrorMessages;
    }
}
