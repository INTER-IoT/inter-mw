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
package eu.interiot.intermw.api;

import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class InterMWInitializer {
    private static final Logger logger = LoggerFactory.getLogger(InterMWInitializer.class);
    private static boolean isInitialized = false;

    private InterMWInitializer() {
    }

    public static void initialize() throws MiddlewareException {
        if (isInitialized) {
            return;
        }

        logger.debug("InterMW is initializing...");

        Reflections reflections = new Reflections("eu.interiot.intermw");
        Set<Class<?>> contextClasses = reflections
                .getTypesAnnotatedWith(eu.interiot.intermw.commons.annotations.Context.class);

        ClassLoader classLoader = InterMWInitializer.class.getClassLoader();

        for (Class<?> contextClass : contextClasses) {
            logger.info("Loading context " + contextClass.getName());
            try {
                Class loadedContextClass = classLoader.loadClass(contextClass.getName());
                if (loadedContextClass == null)
                    logger.error("Class not loaded");
                Class.forName(contextClass.getName());
            } catch (ClassNotFoundException e) {
                logger.error("Class not found", e);
            }
        }
        isInitialized = true;
        logger.debug("InterMW has been initialized successfully.");
    }
}
