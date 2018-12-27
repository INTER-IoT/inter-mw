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
package eu.interiot.intermw.bridge.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes annotated with the {@link BridgeController} annotation manage data
 * flow with {@link Bridge} instances
 *
 * A default implementation of
 * {@link eu.interiot.intermw.bridge.BridgeController} must exist in the
 * classpath but they can be customised for a specific platform type by
 * adding additional instances and annotating them with this annotation
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 * @see eu.interiot.intermw.bridge.BridgeController
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BridgeController {

    /**
     * one or more platform types where the
     * {@link eu.interiot.intermw.bridge.BridgeController} will be used
     *
     * default is all platform types
     *
     * @return The list of {platform types that the
     * {@link eu.interiot.intermw.bridge.BridgeController} instance is
     * able to deal with If no platforms are indicated then the
     * {@link eu.interiot.intermw.bridge.BridgeController} is considered
     * to be able to deal with all the platform type available
     */
    String[] platforms() default {};
}