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
 * Class of all possible actions, performable within middleware
 *
 * @author matevzmarkovic
 */
public enum Actions {
    /**
     * Subscribe to a specific filter.
     * Subscription (request) to device observations (with filter).
     */
    SUBSCRIBE,
    /**
     * Query for values.
     * Query (request) to device observations (with filter). Query differs from subscription because is a
     * ‘one-off’, while subscription is continuous
     */
    QUERY,
    /**
     * Unsubscribe from a topic.
     */
    UNSUBSCRIBE,
    /**
     * Update the device - actuation and other updates.
     */

    THING_UPDATE,
    /**
     * Register a platform.
     * This action tells to the middleware to create an instance of the platform bridge, create channels (broker topics),
     * and register the platform in the Mw services (platform registry).
     */
    REGISTER_PLATFORM,
    /**
     * Unregisters a platform.
     * This action tells to the middleware un-register a platform (remove it from the registry, destroy topics and
     * undeploy the bridge.
     */
    UNREGISTER_PLATFORM,
    /**
     * Sent to the error topic in case of error in the middleware
     */
    ERROR,
    /**
     * The catch-all action, that does absolutely nothing; handled by the DefaultHandler.
     */
    UNKNOWN

}