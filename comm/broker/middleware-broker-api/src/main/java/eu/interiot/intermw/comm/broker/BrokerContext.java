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
import eu.interiot.intermw.commons.ContextHelper;
import eu.interiot.intermw.commons.exceptions.ContextException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Main entry point to get the {@link Broker} and {@link Configuration}
 * instances
 * <p>
 * It uses reflection to automatically search for classes annotated with
 * {@link eu.interiot.intermw.commons.annotations.Configuration} and
 * {@link eu.interiot.intermw.comm.broker.annotations.Broker} annotations
 * <p>
 * The {@link BrokerContext} class stores a map of {@link Broker} per
 * {@link Configuration}
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
@eu.interiot.intermw.commons.annotations.Context
public class BrokerContext {

    private final static Logger log = LoggerFactory.getLogger(BrokerContext.class);

    private final static String PROPERTIES_FILENAME = "intermw.properties";

    private static Map<String, Broker> brokers = new HashMap<>();

    private static Class<? extends Publisher<? extends Object>> defaultPublisherClass;
    private static Class<? extends Subscriber<? extends Object>> defaultSubscriberClass;

    //mappings broker types publisher/subscriber
    private static Map<String, Class<? extends Publisher<? extends Object>>> publishers = new HashMap<String, Class<? extends Publisher<? extends Object>>>();
    private static Map<String, Class<? extends Subscriber<? extends Object>>> subscribers = new HashMap<String, Class<? extends Subscriber<? extends Object>>>();


    private static Map<Class<? extends Topic<Object>>, Class<? extends Object>> topics = new HashMap<Class<? extends Topic<Object>>, Class<? extends Object>>();
    private static Map<Class<? extends Routing<Object>>, Class<? extends Object>> routings = new HashMap<Class<? extends Routing<Object>>, Class<? extends Object>>();
    private static Class<? extends Serializer<?>> serializerImpl;

    private static ContextHelper contextHelper;

    static {
        log.debug("Initializing Broker context...");
        try {
            contextHelper = new ContextHelper(PROPERTIES_FILENAME);
            registerServices(getConfiguration().getScanPackages());
            registerSerializer(getConfiguration().getScanPackages());
            registerTopics(getConfiguration().getScanPackages());
            registerRoutings(getConfiguration().getScanPackages());
            log.info("Broker context has been initialized successfully.");

        } catch (Exception e) {
            log.error("Failed to initialize Broker context: " + e.getMessage(), e);
        }
    }

    /**
     * Looks for a *.broker.properties file in the classpath or in the current
     * jar directory and creates a new {@link Configuration} instance
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

    /**
     * Creates a new {@link Broker} instance with the default
     * {@link Configuration} instance
     *
     * @return A {@link Broker} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          or {@link eu.interiot.intermw.comm.broker.annotations.Broker}
     *                          are found
     * @see #getConfiguration()
     */
    public static Broker getBroker() throws MiddlewareException {
        Configuration configuration = BrokerContext.getConfiguration();
        return BrokerContext.getBroker(configuration, null);
    }

    /**
     * Creates a new {@link Broker} instance with the default
     * {@link Configuration} instance and specific broker implementation
     *
     * @return A {@link Broker} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          or {@link eu.interiot.intermw.comm.broker.annotations.Broker}
     *                          are found
     * @see #getConfiguration()
     */
    public static Broker getBroker(String brokerImplementation) throws MiddlewareException {
        Configuration configuration = BrokerContext.getConfiguration();
        return BrokerContext.getBroker(configuration, brokerImplementation);
    }

    /**
     * Creates a new {@link Broker} instance given a {@link Configuration}
     *
     * @return A {@link Broker} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          or {@link eu.interiot.intermw.comm.broker.annotations.Broker}
     *                          are found
     * @see #getBroker()
     * @see #getConfiguration()
     */
    public static Broker getBroker(Configuration configuration) throws MiddlewareException {
        return BrokerContext.getBroker(configuration, null);

    }

    /**
     * Creates a new {@link Broker} instance given a {@link Configuration}
     *
     * @return A {@link Broker} instance
     * @throws ContextException Thrown when no Classes annotated with
     *                          {@link eu.interiot.intermw.commons.annotations.Configuration}
     *                          or {@link eu.interiot.intermw.comm.broker.annotations.Broker}
     *                          are found
     * @see #getBroker()
     * @see #getConfiguration()
     */
    public static Broker getBroker(Configuration configuration, String brokerImplementation) throws MiddlewareException {
        if (brokerImplementation == null) {
            brokerImplementation = configuration.getBrokerType();
            if (brokerImplementation == null || brokerImplementation.equals(""))
                throw new MiddlewareException("broker-type configuration property is not set.");
        }
        String key = new Integer(configuration.hashCode()).toString() + "#" + brokerImplementation;
        Broker broker = brokers.get(key);
        if (broker == null) {
            broker = BrokerContext.loadBroker(configuration, brokerImplementation);
            brokers.put(key, broker);
        }

        return broker;
    }

    /**
     * gets the {@link Publisher} class registered
     *
     * @return
     */
    public static Class<? extends Publisher<? extends Object>> getDefaultPublisherClass() {
        return defaultPublisherClass;
    }

    /**
     * gets the {@link Publisher} class registered
     *
     * @return
     */
    public static Class<? extends Publisher<? extends Object>> getPublisherClass(String brokerImplementation) throws BrokerException {
        if (!publishers.containsKey(brokerImplementation)) {
            throw new BrokerException(String.format(
                    "No publisher available for broker implementation '%s'.", brokerImplementation));
        }
        return publishers.get(brokerImplementation);
    }

    /**
     * sets the {@link Publisher} class registered. This is not necessary if you
     * annotate your {@link Publisher} with the
     * {@link eu.interiot.intermw.comm.broker.annotations.Publisher} annotation
     * since it is automatically registered by the {@link BrokerContext}
     *
     * @param defaultPublisherClass
     */
    public static void setDefaultPublisherClass(Class<? extends Publisher<? extends Object>> defaultPublisherClass) {
        BrokerContext.defaultPublisherClass = defaultPublisherClass;
    }

    /**
     * gets the {@link Subscriber} class registered
     *
     * @return
     */
    public static Class<? extends Subscriber<? extends Object>> getDefaultSubscriberClass() {
        return defaultSubscriberClass;
    }

    /**
     * gets the {@link Subscriber} class registered
     *
     * @return
     */
    public static Class<? extends Subscriber<? extends Object>> getSubscriberClass(String brokerImplementation) {
        return subscribers.get(brokerImplementation);
    }

    /**
     * sets the {@link Subscriber} class registered. This is not necessary if
     * you annotate your {@link Subscriber} with the
     * {@link eu.interiot.intermw.comm.broker.annotations.Subscriber} annotation
     * since it is automatically registered by the {@link BrokerContext}
     *
     * @param defaultSubscriberClass
     */
    public static void setDefaultSubscriberClass(Class<? extends Subscriber<? extends Object>> defaultSubscriberClass) {
        BrokerContext.defaultSubscriberClass = defaultSubscriberClass;
    }

    /**
     * gets the {@link Serializer} class registered
     *
     * @return
     */
    public static Class<? extends Serializer<?>> getSerializer() {
        return serializerImpl;
    }

    /**
     * sets the {@link Serializer} class registered. This is not necessary if
     * you annotate your {@link Serializer} with the
     * {@link eu.interiot.intermw.comm.broker.annotations.Serializer} annotation
     * since it is automatically registered by the {@link BrokerContext}
     *
     * @param serializerImpl
     */
    public static void setSerializer(Class<? extends Serializer<?>> serializerImpl) {
        BrokerContext.serializerImpl = serializerImpl;
    }

    /**
     * gets the {@link Topic} classes registered and the message class they are
     * tied with
     *
     * @return
     */
    public static Map<Class<? extends Topic<Object>>, Class<? extends Object>> getTopics() {
        return topics;
    }

    /**
     * sets the {@link Topic} classes registered and their messages classes.
     * This is not necessary if you annotate your {@link Topic} classes with the
     * {@link eu.interiot.intermw.comm.broker.annotations.Topic} annotation
     * since they are automatically registered by the {@link BrokerContext}
     *
     * @param topics
     */
    public static void setTopics(Map<Class<? extends Topic<Object>>, Class<? extends Object>> topics) {
        BrokerContext.topics = topics;
    }

    private static Broker loadBroker(Configuration configuration, String brokerImplementation) throws MiddlewareException {
        Class<?> _Class = contextHelper
                .getFirstClassAnnotatedWith(eu.interiot.intermw.comm.broker.annotations.Broker.class);

        log.debug("Creating a new instance of " + _Class);
        Broker broker = null;
        try {
            broker = (Broker) _Class.getConstructor(Configuration.class, String.class).newInstance(configuration, brokerImplementation);

        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw new ContextException(String.format("Cannot create an instance of the class %s: %s.",
                    _Class.getCanonicalName(), targetException.getMessage()), targetException);

        } catch (Exception e) {
            throw new ContextException(String.format("Cannot create an instance of the class %s: %s.",
                    _Class.getCanonicalName(), e.getMessage()), e);
        }

        return broker;
    }

    @SuppressWarnings({"unchecked"})
    private static void registerServices(List<String> packagesNames) throws MiddlewareException {
        String defaultBroker = getConfiguration().getBrokerType();
        if (defaultBroker == null || defaultBroker.equals(""))
            throw new MiddlewareException("broker-type configuration property is not set.");

        for (String packageName : packagesNames) {
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> publisherClasses = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.comm.broker.annotations.Publisher.class);
            Iterator<Class<?>> it = publisherClasses.iterator();

            Class<?> publisherClass;
            while (it.hasNext()) {
                publisherClass = it.next();
                String broker = publisherClass.getAnnotation(eu.interiot.intermw.comm.broker.annotations.Publisher.class).broker();
                log.info("Found publisher class for " + broker);
                BrokerContext.publishers.put(broker, (Class<? extends Publisher<? extends Object>>) publisherClass);
                if (broker.equals(defaultBroker)) {
                    log.info("Set default publisher class: " + defaultBroker);
                    BrokerContext.setDefaultPublisherClass((Class<? extends Publisher<? extends Object>>) publisherClass);
                }
            }

            Set<Class<?>> subscriberClasses = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.comm.broker.annotations.Subscriber.class);
            it = subscriberClasses.iterator();

            Class<?> subscriberClass;
            while (it.hasNext()) {
                subscriberClass = it.next();
                String broker = subscriberClass.getAnnotation(eu.interiot.intermw.comm.broker.annotations.Subscriber.class).broker();
                log.info("Found subscriber class for " + broker);
                BrokerContext.subscribers.put(broker, (Class<? extends Subscriber<? extends Object>>) subscriberClass);
                if (broker.equals(defaultBroker)) {
                    log.info("Set default subscriber class: " + defaultBroker);
                    BrokerContext.setDefaultSubscriberClass((Class<? extends Subscriber<? extends Object>>) subscriberClass);
                }
            }

        }
        if (defaultSubscriberClass == null)
            throw new MiddlewareException(String.format("defaultSubscriberClass for %s not found.", defaultBroker));
        if (defaultPublisherClass == null)
            throw new MiddlewareException(String.format("defaultPublisherClass for %s not found.", defaultBroker));

    }

    @SuppressWarnings({"unchecked"})
    private static void registerSerializer(List<String> packagesNames) throws MiddlewareException {
        Class<? extends Serializer<?>> serializerClass = (Class<? extends Serializer<?>>) contextHelper
                .getDefaultAnnotatedClass(packagesNames, eu.interiot.intermw.comm.broker.annotations.Serializer.class);

        if (serializerClass != null) {
            BrokerContext.setSerializer(serializerClass);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static void registerTopics(List<String> packagesNames) {
        for (String packageName : packagesNames) {
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> topicsClasses = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.comm.broker.annotations.Topic.class);

            Iterator<Class<?>> it = topicsClasses.iterator();

            Class<?> topicClass;
            while (it.hasNext()) {
                topicClass = it.next();
                BrokerContext.topics.put((Class<? extends Topic<Object>>) topicClass,
                        BrokerContext.getMessageClassName(topicClass));
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private static void registerRoutings(List<String> packagesNames) {
        for (String packageName : packagesNames) {
            Reflections reflections = new Reflections(
                    new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageName))
                            .setScanners(new ResourcesScanner(), new SubTypesScanner(), new TypeAnnotationsScanner()));
            Set<Class<?>> classes = reflections
                    .getTypesAnnotatedWith(eu.interiot.intermw.comm.broker.annotations.Routing.class);

            Iterator<Class<?>> it = classes.iterator();

            Class<?> rClass;
            while (it.hasNext()) {
                rClass = it.next();
                BrokerContext.routings.put((Class<? extends Routing<Object>>) rClass, BrokerContext.getMessageClassName(rClass));
            }
        }
    }

    /**
     * Given a <topicClass> it looks for the generic type description of the
     * {@link Topic} to get the message class name
     * <p>
     * When using a {@link Topic} annotated as <isDefault> this is not necessary
     *
     * @param topicClass The {@link Topic} instance class
     * @return The message class this {@link Topic} is tied to
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Object> getMessageClassName(Class<?> topicClass) {
        Class<? extends Object> result = null;
        Type type = topicClass.getGenericSuperclass();

        if (type.toString().equals("class java.lang.Object")) {
            Type[] interfaceTypes = topicClass.getGenericInterfaces();
            for (Type interfaceType : interfaceTypes) {
                if ((interfaceType instanceof ParameterizedType)) {
                    Type[] args = ((ParameterizedType) interfaceType).getActualTypeArguments();
                    result = (Class<? extends Object>) args[0];
                    break;
                }
            }
        } else {
            while (!(type instanceof ParameterizedType) && topicClass != Object.class) {
                topicClass = topicClass.getSuperclass();
                type = topicClass.getGenericSuperclass();
            }

            try {
                ParameterizedType pType = ((ParameterizedType) type);
                Type[] types = pType.getActualTypeArguments();

                result = (Class<? extends Object>) types[0];
            } catch (Exception e) {
                log.trace("Exception " + e.getClass().toString() + " while looking for the message class of the topic: "
                        + topicClass.toString());
                result = null;
            }
        }

        return result;
    }
}
