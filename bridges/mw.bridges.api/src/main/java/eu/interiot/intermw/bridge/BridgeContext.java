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
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.ContextHelper;
import eu.interiot.intermw.commons.exceptions.ContextException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

/**
 * Main entry point to get the {@link Bridge}, {@link BridgeController}
 * <p>
 * It uses reflection to automatically search for classes annotated with
 * Annotations from package <code>eu.interiot.intermw.bridge.annotations</code>
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
@eu.interiot.intermw.commons.annotations.Context
public class BridgeContext {
    private static final Logger log = LoggerFactory.getLogger(BridgeContext.class);

    /**
     * A {@link Configuration} instance is loaded taking the properties from a
     * file with an extension that matches this attribute
     */
    private final static String PROPERTIES_FILENAME = "intermw.properties";
    private static Configuration configuration;
    private static Map<String, Class<? extends Bridge>> registeredBridgeClasses = new HashMap<>();
    private static Map<String, Class<? extends BridgeController>> bridgeControllers = new HashMap<>();
    private static Class<? extends BridgeController> defaultBridgeController;

    private static ContextHelper contextHelper;

    //FIXME moved from mw.api. The list of deployed bridge instances associated with platform identifiers should be
    //kept in this package. Context may not be the best place for this - PRO guys, refactor if you think should go elsewherer
    private static Map<String, BridgeController> bridgesByPlatformId = new HashMap<String, BridgeController>();


    /**
     * Instances are loaded statically once this class is added to the classpath
     */
    static {
        log.debug("Initializing Bridge context...");
        try {
            contextHelper = new ContextHelper(PROPERTIES_FILENAME);
            configuration = getConfiguration();
            List<String> scanPackages = configuration.getScanPackages();
            registerBridges(scanPackages);
            registerBridgeControllers(scanPackages);

            URL callbackUrl = new URL(configuration.getBridgeCallbackUrl());
            Spark.port(callbackUrl.getPort());
            log.debug("Bridge callback port is set to {}.", callbackUrl.getPort());

            Spark.get("/", (request, response) -> {
                return "Bridge callback listener is running.";
            });
            log.info("Bridge context has been initialized successfully.");

        } catch (Exception e) {
            log.error("Failed to initialize Bridge context: " + e.getMessage(), e);
        }
    }


    /**
     * Creates a new instance of {@link Bridge} for the given platform
     * <p>
     * This method works by reflection. It looks for a {@link Bridge} instance
     * annotated with a <code>platform</code> attribute that matches the provided platform type
     */
    public static Bridge createBridgeInstance(Platform platform) throws MiddlewareException {
        String platformId = platform.getPlatformId();
        String platformType = platform.getType();
        try {
            log.debug("Creating bridge instance for the platform '{}' of type {}...", platformId, platformType);
            Class<? extends Bridge> bridgeClass = registeredBridgeClasses.get(platformType);
            if (bridgeClass == null) {
                throw new ContextException(String.format("No bridge is registered for platform type %s.", platformType));
            }
            log.debug("Found bridge registered for platform type {}: {}.", platformType, bridgeClass.getName());

            log.debug("Loading configuration for the bridge {} and platform {}...", bridgeClass.getSimpleName(), platformId);
            String bridgeConfFileName = bridgeClass.getSimpleName() + ".properties";
            BridgeConfiguration bridgeConfiguration = new BridgeConfiguration(bridgeConfFileName, platformId, configuration);

            log.debug("Creating an instance of the bridge {} for the platform {}...", bridgeClass.getName(), platformType);
            Bridge bridge = (Bridge) bridgeClass.getConstructor(BridgeConfiguration.class, Platform.class)
                    .newInstance(bridgeConfiguration, platform);
            log.info("Bridge instance for the platform '{}' has been created successfully.", platformId);
            return bridge;

        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            log.error(String.format("Failed to instantiate bridge class for platform of type %s.",
                    platformType), targetException);
            throw new MiddlewareException(String.format("Failed to instantiate bridge class for platform of type %s.",
                    platformType), targetException);

        } catch (Exception e) {
            log.error(String.format("Failed to instantiate bridge class for platform of type %s.",
                    platformType), e);
            throw new MiddlewareException(String.format("Failed to instantiate bridge class for platform of type %s.",
                    platformType), e);
        }
    }

    protected static BridgeController createBridgeController(Platform platform) throws MiddlewareException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Bridge bridge = createBridgeInstance(platform);
        Class<? extends BridgeController> bridgeControllerClass = bridgeControllers.get(platform.getType());
        if (bridgeControllerClass == null)
            bridgeControllerClass = defaultBridgeController;
        return bridgeControllerClass.getConstructor(Platform.class, Bridge.class)
                .newInstance(platform, bridge);
    }

    /**
     * Looks for a *.broker.properties file in the classpath or in the current
     * jar directory and creates a new {@link Configuration} instance
     *
     * @return A {@link Configuration} instance
     * @throws ContextException Thrown when no Class annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          is found
     * @see ContextHelper
     */
    public static Configuration getConfiguration() throws MiddlewareException {
        return contextHelper.getConfiguration();
    }

    /**
     * Creates a new {@link Configuration} instance given a <propertyFileName>
     *
     * @param propertyFileName The properties file name
     * @return A new {@link Configuration} instance
     * @throws ContextException Thrown when no Class annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          is found
     * @see #getConfiguration()
     */
    public static Configuration getConfiguration(String propertyFileName) throws MiddlewareException {
        return contextHelper.getConfiguration(propertyFileName);
    }

    @SuppressWarnings({"unchecked"})
    private static void registerBridges(List<String> packagesNames) {
        log.debug("Registering bridges...");
        for (String packageName : packagesNames) {
            log.debug("Scanning package {}...", packageName);
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> bridgesClasses = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.bridge.annotations.Bridge.class);
            log.debug("Following bridges have been found: {}", bridgesClasses);

            for (Class<?> bridgeClass : bridgesClasses) {
                String platformType = bridgeClass
                        .getAnnotation(eu.interiot.intermw.bridge.annotations.Bridge.class).platformType();
                BridgeContext.registeredBridgeClasses.put(platformType, (Class<? extends Bridge>) bridgeClass);
                log.debug("Bridge {} for platform type {} has been registered.", bridgeClass.getName(), platformType);
            }
        }
        log.debug("Bridge registration has finished successfully.");
    }

    @SuppressWarnings({"unchecked"})
    private static void registerBridgeControllers(List<String> packagesNames) {
        for (String packageName : packagesNames) {
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> bridgeControllersClasses = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.bridge.annotations.BridgeController.class);

            Iterator<Class<?>> it = bridgeControllersClasses.iterator();
            log.debug("Found {} bridge controllers", bridgeControllersClasses.size());

            Class<?> bridgeControllerClass;
            while (it.hasNext()) {
                bridgeControllerClass = it.next();
                log.debug("Class: {}", bridgeControllerClass.getName());
                String[] platformTypes = bridgeControllerClass
                        .getAnnotation(eu.interiot.intermw.bridge.annotations.BridgeController.class).platforms();

                if (platformTypes == null || platformTypes.length == 0) {
                    //this is the default bridge controller
                    defaultBridgeController = (Class<? extends BridgeController>) bridgeControllerClass;
                } else {
                    for (String platformType : platformTypes) {
                        BridgeContext.bridgeControllers.put(platformType, (Class<? extends BridgeController>) bridgeControllerClass);
                    }
                }
            }
        }
    }


    public static void createBridge(Platform platform) throws Exception {
        log.debug("Creating bridge controller for the platform {}...", platform.getPlatformId());
        if (bridgesByPlatformId.containsKey(platform.getPlatformId())) {
            throw new ContextException("Bridge is already registered for the platform " + platform.getPlatformId() + ".");
        }

        BridgeController controller = createBridgeController(platform);
        bridgesByPlatformId.put(platform.getPlatformId(), controller);

        log.debug("Bridge controller has been created successfully for the platform {}...", platform.getPlatformId());
    }

    public static void removeBridge(String platformId) throws BridgeException, ContextException, BrokerException {
        log.debug("Removing bridge controller for the platform {}...", platformId);
        if (!bridgesByPlatformId.containsKey(platformId)) {
            throw new ContextException(String.format("No bridge is registered for the platoform %s.", platformId));
        }

        BridgeController bridgeController = bridgesByPlatformId.get(platformId);
        bridgeController.destroy();
        bridgesByPlatformId.remove(platformId);
        log.debug("Bridge controller has been removed successfully for the platform {}...", platformId);
    }

    public static Collection<String> getSupportedPlatformTypes() {
        return registeredBridgeClasses.keySet();
    }
}
