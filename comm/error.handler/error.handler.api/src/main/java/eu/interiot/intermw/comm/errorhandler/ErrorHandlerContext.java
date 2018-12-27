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
package eu.interiot.intermw.comm.errorhandler;

import eu.interiot.intermw.commons.ContextHelper;
import eu.interiot.intermw.commons.exceptions.ContextException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * Main entry point to get the {@link ErrorHandler} and {@link Configuration}
 * instances
 * <p>
 * It uses reflection to automatically search for classes annotated with
 * {@link eu.interiot.intermw.comm.errorhandler.annotations.ErrorHandler} annotations
 * <p>
 * The {@link ErrorHandlerContext} class stores a map of {@link ErrorHandler} per
 * {@link Configuration}. Currently only one configuration is supported.
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 */
//@eu.interiot.intermw.commons.annotations.Context
// TODO: are ErrorHandler and ErrorHandlerContext really needed?
public class ErrorHandlerContext {

    private final static Logger log = LoggerFactory.getLogger(ErrorHandlerContext.class);
    private final static String PROPERTIES_FILENAME = "intermw.properties";

    private static ContextHelper contextHelper;

    private static Map<Integer, ErrorHandler> errorHandlers = new HashMap<>();

    static {
        log.debug("Initializing ErrorHandler context...");
        try {
            contextHelper = new ContextHelper(PROPERTIES_FILENAME);
            if (getErrorHandler() == null) {
                throw new MiddlewareException("Cannot create ErrorHandler instance.");
            }
            log.info("ErrorHandler context has been initialized successfully.");

        } catch (Exception e) {
            log.error("Failed to initialize ErrorHandler context: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the default {@link Configuration} instance from the {@link ContextHelper}
     *
     * @return A {@link Configuration} instance
     * @throws ContextException Thrown when no Class annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          is found
     */
    public static Configuration getConfiguration() throws MiddlewareException {
        return contextHelper.getConfiguration();
    }

    /**
     * Creates a new {@link ErrorHandler} instance with the default
     * {@link Configuration} instance
     *
     * @return A {@link ErrorHandler} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          or
     *                          {@link eu.interiot.intermw.comm.errorhandler.annotations.ErrorHandler}
     *                          are found
     * @see #getConfiguration()
     */
    public static ErrorHandler getErrorHandler() throws MiddlewareException {
        Configuration configuration = ErrorHandlerContext.getConfiguration();
        return ErrorHandlerContext.getErrorHandler(configuration);
    }

    /**
     * Creates a new {@link ErrorHandler} instance given a {@link Configuration}
     *
     * @return A {@link ErrorHandler} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link Configuration}
     *                          or
     *                          {@link eu.interiot.intermw.comm.errorhandler.annotations.ErrorHandler}
     *                          are found
     * @see #getErrorHandler() ()
     * @see #getConfiguration()
     */
    public static ErrorHandler getErrorHandler(Configuration configuration) throws MiddlewareException {
        Integer configurationKey = configuration.hashCode();
        ErrorHandler errorHandler = errorHandlers.get(configurationKey);
        if (errorHandler == null) {
            errorHandler = ErrorHandlerContext.loadErrorHandler(configuration);
            errorHandlers.put(configurationKey, errorHandler);
        }

        return errorHandler;
    }

    private static ErrorHandler loadErrorHandler(Configuration configuration) throws MiddlewareException {
        Class<?> _Class = contextHelper.getFirstClassAnnotatedWith(
                eu.interiot.intermw.comm.errorhandler.annotations.ErrorHandler.class);

        log.debug("Creating a new instance of " + _Class);
        ErrorHandler errorHandler;
        try {
            errorHandler = (ErrorHandler) _Class.getConstructor(Configuration.class).newInstance(configuration);

        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw new ContextException(String.format("Cannot create an instance of the class %s.",
                    _Class.getCanonicalName()), targetException);

        } catch (Exception e) {
            throw new ContextException(String.format("Cannot create an instance of the class %s.",
                    _Class.getCanonicalName()), e);
        }

        return errorHandler;
    }


}
