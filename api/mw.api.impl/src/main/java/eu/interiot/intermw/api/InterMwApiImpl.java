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

import eu.interiot.intermw.api.exception.BadRequestException;
import eu.interiot.intermw.api.exception.ConflictException;
import eu.interiot.intermw.api.exception.NotFoundException;
import eu.interiot.intermw.api.model.*;
import eu.interiot.intermw.comm.arm.ARMContext;
import eu.interiot.intermw.comm.arm.ApiRequestManager;
import eu.interiot.intermw.comm.arm.HttpPushApiCallback;
import eu.interiot.intermw.comm.arm.RabbitMQApiCallback;
import eu.interiot.intermw.comm.broker.rabbitmq.QueueImpl;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.ApiCallback;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.*;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.intermw.commons.model.enums.QueryType;
import eu.interiot.intermw.commons.requests.*;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import eu.interiot.message.Message;
import eu.interiot.message.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * FIXME add javadoc
 *
 * @author aromeu
 */
public class InterMwApiImpl implements InterMwApi {
    private static final Logger logger = LoggerFactory.getLogger(InterMwApiImpl.class);
    private Configuration configuration;
    private static ApiRequestManager apiRequestManager;
    private static QueueImpl queue;
    private ParliamentRegistry registry;

    public InterMwApiImpl(Configuration configuration) throws MiddlewareException {
        this.configuration = configuration;
        InterMWInitializer.initialize();
        apiRequestManager = ARMContext.getApiRequestManager();
        queue = new QueueImpl();
        registry = new ParliamentRegistry(configuration);
    }

    @Override
    public Client getClient(String clientId) throws MiddlewareException {
        return getRegistry().getClientById(clientId);
    }

    @Override
    public List<Client> listClients() throws MiddlewareException {
        return getRegistry().listClients();
    }

    @Override
    public void registerClient(Client client) throws MiddlewareException, ConflictException, BadRequestException {
        registerClient(client, null);
    }

    @Override
    public void registerClient(Client client, ApiCallback<Message> apiCallback) throws MiddlewareException, ConflictException, BadRequestException {
        String clientId = client.getClientId();
        if (getRegistry().isClientRegistered(clientId)) {
            throw new ConflictException(String.format("Client '%s' is already registered.", clientId));
        }

        if (client.getResponseDelivery() == null && apiCallback == null ||
                client.getResponseDelivery() != null && apiCallback != null) {
            throw new BadRequestException("Either response delivery or apiCallback must be given.");
        }

        if (client.getResponseDelivery() == Client.ResponseDelivery.SERVER_PUSH) {
            if (client.getCallbackUrl() == null) {
                throw new BadRequestException("CallbackUrl attribute must be specified when using SERVER_PUSH response delivery.");
            }
        }

        if (client.getReceivingCapacity() == null) {
            client.setReceivingCapacity(configuration.getClientReceivingCapacityDefault());
        }

        if (client.getReceivingCapacity() < 1) {
            throw new BadRequestException("Invalid receivingCapacity: must be greater than or equal to 1.");
        }

        try {
            logger.debug("Registering client {}...", client.getClientId());

            if (apiCallback == null) {
                switch (client.getResponseDelivery()) {
                    case CLIENT_PULL:
                        apiCallback = new RabbitMQApiCallback(clientId, queue);
                        break;
                    case SERVER_PUSH:
                        apiCallback = new HttpPushApiCallback(client, queue, configuration);
                        break;
                    default:
                        throw new BadRequestException(String.format("Response delivery %s is not supported.", client.getResponseDelivery()));
                }
            }

            getRegistry().registerClient(client);
            apiRequestManager.registerCallback(client.getClientId(), apiCallback);
            logger.debug("Client {} has been registered successfully.", client.getClientId());

        } catch (Exception e) {
            throw new MiddlewareException("Failed to register client.", e);
        }
    }

    @Override
    public void updateClient(String clientId, UpdateClientInput input) throws MiddlewareException, NotFoundException, BadRequestException {
        Client client = getRegistry().getClientById(clientId);
        if (client == null) {
            throw new NotFoundException(String.format("Client '%s' does not exist.", clientId));
        }

        if (input.getCallbackUrl() != null) {
            try {
                client.setCallbackUrl(new URL(input.getCallbackUrl()));
            } catch (MalformedURLException e) {
                throw new BadRequestException(String.format("Invalid callbackURL: %s", input.getCallbackUrl()));
            }
        }
        if (input.getReceivingCapacity() != null) {
            client.setReceivingCapacity(input.getReceivingCapacity());
        }
        if (input.getResponseDelivery() != null) {
            client.setResponseDelivery(input.getResponseDelivery());
        }
        if (input.getResponseFormat() != null) {
            client.setResponseFormat(input.getResponseFormat());
        }

        getRegistry().updateClient(client);

        apiRequestManager.updateCallback(client);
    }

    @Override
    public void removeClient(String clientId) throws MiddlewareException, NotFoundException {
        if (!getRegistry().isClientRegistered(clientId)) {
            throw new NotFoundException(String.format("Client '%s' does not exist.", clientId));
        }

        try {
            logger.debug("Removing client {}...", clientId);
            getRegistry().removeClient(clientId);
            apiRequestManager.unregisterCallback(clientId);
            logger.debug("Client {} has been removed successfully.", clientId);
        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to remove client %s.", clientId), e);
        }
    }

    @Override
    public Message retrieveResponseMessage(String clientId, long timeoutMillis) throws MiddlewareException {
        try {
            return queue.consumeMessage(clientId, timeoutMillis);
        } catch (Exception e) {
            throw new MiddlewareException("Failed to retrieve response messages.", e);
        }
    }

    @Override
    public List<Message> retrieveResponseMessages(String clientId) throws MiddlewareException {
        Client client = getRegistry().getClientById(clientId);
        try {
            List<String> messageStrings = queue.consumeMessages(clientId, client.getReceivingCapacity());
            List<Message> messages = new ArrayList<>();
            for (String messageString : messageStrings) {
                messages.add(new Message(messageString));
            }
            return messages;

        } catch (Exception e) {
            throw new MiddlewareException("Failed to retrieve response messages.", e);
        }
    }

    @Override
    public String listPlatformTypes(String clientId) throws MiddlewareException {
        ListPlatformTypesReq req = new ListPlatformTypesReq();
        req.setClientId(clientId);

        return sendToARM(req.toMessage());
    }

    @Override
    public List<Platform> listPlatforms() throws MiddlewareException {
        return getRegistry().listPlatforms();
    }

    @Override
    public String registerPlatform(Platform platform) throws MiddlewareException, ConflictException, BadRequestException {
        if (getRegistry().getPlatformById(platform.getPlatformId()) != null) {
            throw new ConflictException(String.format("Platform %s is already registered.", platform.getPlatformId()));
        }

        validateAlignmentsData(platform);

        List<Message> messageList = new ArrayList<>();
        RegisterPlatformReq registerPlatformReq = new RegisterPlatformReq(platform);
        try {
            messageList.add(registerPlatformReq.toMessage());
            ListDevicesReq listDevicesReq = new ListDevicesReq(platform.getClientId(), platform.getPlatformId());
            messageList.add(listDevicesReq.toMessage());
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a message.", e);
        }
        return sendToARM(messageList).get(0);
    }

    @Override
    public Platform getPlatform(String platformId) throws MiddlewareException {
        return getRegistry().getPlatformById(platformId);
    }

    @Override
    public String updatePlatform(Platform platform) throws MiddlewareException, BadRequestException, NotFoundException {
        Platform existingPlatform = getRegistry().getPlatformById(platform.getPlatformId());
        if (existingPlatform == null) {
            throw new NotFoundException(String.format("Platform with ID %s cannot be found.", platform.getPlatformId()));
        }
        if (platform.getType() == null) {
            platform.setType(existingPlatform.getType());

        } else if (!existingPlatform.getType().equals(platform.getType())) {
            throw new BadRequestException("Platform type cannot be changed.");
        }
        platform.setTimeCreated(existingPlatform.getTimeCreated());

        validateAlignmentsData(platform);

        UpdatePlatformReq req = new UpdatePlatformReq(platform);

        Message message;
        try {
            message = req.toMessage();
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a message.", e);
        }
        return sendToARM(message);
    }

    @Override
    public String updatePlatform(String clientId, String platformId, UpdatePlatformInput input) throws NotFoundException, MiddlewareException, BadRequestException {
        Platform platform = getRegistry().getPlatformById(platformId);
        if (platform == null) {
            throw new NotFoundException(String.format("Platform with ID %s does not exist.", platformId));
        }

        platform.setClientId(clientId);

        if (input.getBaseEndpoint() != null) {
            try {
                platform.setBaseEndpoint(new URL(input.getBaseEndpoint()));
            } catch (MalformedURLException e) {
                throw new BadRequestException(String.format("Invalid baseEndpoint: %s.", input.getBaseEndpoint()));
            }
        }
        if (input.getLocation() != null) {
            platform.setLocationId(input.getLocation());
        }
        if (input.getName() != null) {
            platform.setName(input.getName());
        }
        if (input.getUsername() != null) {
            platform.setUsername(input.getUsername());
        }
        if (input.getEncryptedPassword() != null) {
            platform.setEncryptedPassword(input.getEncryptedPassword());
        }
        if (input.getEncryptionAlgorithm() != null) {
            platform.setEncryptionAlgorithm(input.getEncryptionAlgorithm());
        }
        if (input.getDownstreamInputAlignmentName() != null) {
            platform.setDownstreamInputAlignmentName(input.getDownstreamInputAlignmentName());
        }
        if (input.getDownstreamInputAlignmentVersion() != null) {
            platform.setDownstreamInputAlignmentVersion(input.getDownstreamInputAlignmentVersion());
        }
        if (input.getDownstreamOutputAlignmentName() != null) {
            platform.setDownstreamOutputAlignmentName(input.getDownstreamOutputAlignmentName());
        }
        if (input.getDownstreamOutputAlignmentVersion() != null) {
            platform.setDownstreamOutputAlignmentVersion(input.getDownstreamOutputAlignmentVersion());
        }
        if (input.getUpstreamInputAlignmentName() != null) {
            platform.setUpstreamInputAlignmentName(input.getUpstreamInputAlignmentName());
        }
        if (input.getUpstreamInputAlignmentVersion() != null) {
            platform.setUpstreamInputAlignmentVersion(input.getUpstreamInputAlignmentVersion());
        }
        if (input.getUpstreamOutputAlignmentName() != null) {
            platform.setUpstreamOutputAlignmentName(input.getUpstreamOutputAlignmentName());
        }
        if (input.getUpstreamOutputAlignmentVersion() != null) {
            platform.setUpstreamOutputAlignmentVersion(input.getUpstreamOutputAlignmentVersion());
        }

        validateAlignmentsData(platform);

        UpdatePlatformReq req = new UpdatePlatformReq(platform);

        Message message;
        try {
            message = req.toMessage();
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a message.", e);
        }
        return sendToARM(message);
    }

    @Override
    public String removePlatform(String clientId, String platformId) throws MiddlewareException, NotFoundException, BadRequestException {
        Platform platform = getRegistry().getPlatformById(platformId);
        if (platform == null) {
            throw new NotFoundException(String.format("Platform with ID %s doesn't exist.", platformId));
        }

        UnregisterPlatformReq req = new UnregisterPlatformReq(clientId, platformId);
        Message message;
        try {
            message = req.toMessage();
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a message.", e);
        }

        return sendToARM(message);
    }

    @Override
    public String platformCreateDevices(String clientId, List<IoTDevice> devices) throws MiddlewareException, BadRequestException, ConflictException {
        if (devices.isEmpty()) {
            throw new BadRequestException("At least one device must be specified.");
        }

        // check if any of specified devices is already registered
        List<String> deviceIds = new ArrayList<>();
        for (IoTDevice device : devices) {
            deviceIds.add(device.getDeviceId());
        }
        List<IoTDevice> alreadyRegisteredDevices = getRegistry().getDevices(deviceIds);
        if (!alreadyRegisteredDevices.isEmpty()) {
            List<String> alreadyRegisteredDeviceIds = new ArrayList<>();
            for (IoTDevice device : alreadyRegisteredDevices) {
                alreadyRegisteredDeviceIds.add(device.getDeviceId());
            }
            throw new ConflictException(String.format("Following devices are already registered: %s.", alreadyRegisteredDeviceIds));
        }

        Map<String, List<IoTDevice>> devicesPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!devicesPerPlatform.containsKey(device.getHostedBy())) {
                devicesPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            devicesPerPlatform.get(device.getHostedBy()).add(device);
        }

        String conversationId = MessageUtils.generateConversationID();

        List<Message> messages = new ArrayList<>();
        for (String platformId : devicesPerPlatform.keySet()) {
            PlatformCreateDeviceReq req = new PlatformCreateDeviceReq(platformId, devicesPerPlatform.get(platformId), clientId);
            Message message;
            try {
                message = req.toMessage(conversationId);
            } catch (Exception e) {
                throw new BadRequestException("Failed to convert request to a message.", e);
            }

            messages.add(message);
        }

        for (Message message : messages) {
            sendToARM(message);
        }

        return conversationId;
    }


    @Override
    public String platformUpdateDevice(String clientId, IoTDevice device) throws MiddlewareException, BadRequestException {
        return platformUpdateDevices(clientId, Collections.singletonList(device));
    }

    @Override
    public String platformUpdateDevices(String clientId, List<IoTDevice> devices) throws MiddlewareException, BadRequestException {
        if (devices.isEmpty()) {
            throw new BadRequestException("At least one device must be specified.");
        }

        List<String> deviceIds = new ArrayList<>();
        for (IoTDevice device : devices) {
            deviceIds.add(device.getDeviceId());
        }

        List<IoTDevice> existingDevices = getRegistry().getDevices(deviceIds);
        if (existingDevices.size() < devices.size()) {
            throw new BadRequestException("One or more devices are not registered within INTER-MW.");
        }

        Map<String, List<IoTDevice>> devicesPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!devicesPerPlatform.containsKey(device.getHostedBy())) {
                devicesPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            devicesPerPlatform.get(device.getHostedBy()).add(device);
        }

        String conversationId = MessageUtils.generateConversationID();

        for (String platformId : devicesPerPlatform.keySet()) {
            PlatformUpdateDeviceReq req = new PlatformUpdateDeviceReq(platformId, devicesPerPlatform.get(platformId), clientId);
            sendToARM(req.toMessage(conversationId));
        }

        return conversationId;
    }

    @Override
    public String platformDeleteDevices(String clientId, List<String> deviceIds) throws MiddlewareException, BadRequestException {
        if (deviceIds.isEmpty()) {
            throw new BadRequestException("At least one device must be specified.");
        }

        // check if all devices are registered
        List<IoTDevice> devices = getRegistry().getDevices(deviceIds);
        if (devices.size() < deviceIds.size()) {
            throw new BadRequestException("One or more devices are not registered within INTER-MW.");
        }

        String conversationId = MessageUtils.generateConversationID();

        Map<String, List<String>> devicesPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!devicesPerPlatform.containsKey(device.getHostedBy())) {
                devicesPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            devicesPerPlatform.get(device.getHostedBy()).add(device.getDeviceId());
        }

        for (String platformId : devicesPerPlatform.keySet()) {
            PlatformDeleteDeviceReq req = new PlatformDeleteDeviceReq(platformId, devicesPerPlatform.get(platformId), clientId);
            sendToARM(req.toMessage(conversationId));
        }

        return conversationId;
    }

    @Override
    public List<IoTDevice> listDevices(String clientId, String platformId) throws MiddlewareException {
        List<String> platformDeviceGraphs = getRegistry().getDeviceIds(platformId);
        return getRegistry().getDevices(platformDeviceGraphs);
    }

    @Override
    public List<IoTDevice> deviceDiscoveryQuery(String clientId, IoTDeviceFilter query) throws MiddlewareException {
        return getRegistry().deviceDiscoveryQuery(query);
    }

    @Override
    public String syncDevices(String clientId, String platformId) throws MiddlewareException {
        String conversationId = MessageUtils.generateConversationID();
        SyncDevicesReq req = new SyncDevicesReq(platformId);
        req.setClientId(clientId);
        sendToARM(req.toMessage(conversationId));

        return conversationId;
    }

    @Override
    public String subscribe(String clientId, List<String> deviceIds) throws MiddlewareException, BadRequestException {
        List<IoTDevice> devices = getRegistry().getDevices(deviceIds);
        if (devices.size() == 0) {
            throw new BadRequestException("Specified devices are not registered with Inter MW.");
        } else if (devices.size() < deviceIds.size()) {
            throw new BadRequestException("Not all devices are registered with Inter MW.");
        }

        String conversationId = MessageUtils.generateConversationID();
        String subscriptionId = conversationId;

        Subscription subscription = new Subscription();
        subscription.setConversationId(subscriptionId);
        subscription.setClientId(clientId);
        subscription.setDeviceIds(deviceIds);

        getRegistry().subscribe(subscription);

        Map<String, List<IoTDevice>> devicesPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!devicesPerPlatform.containsKey(device.getHostedBy())) {
                devicesPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            devicesPerPlatform.get(device.getHostedBy()).add(device);
        }

        List<Message> messages = new ArrayList<>();
        for (String platformId : devicesPerPlatform.keySet()) {
            SubscribeReq req = new SubscribeReq(subscriptionId, clientId,
                    platformId, devicesPerPlatform.get(platformId));
            Message message;
            try {
                message = req.toMessage(conversationId);
            } catch (Exception e) {
                throw new BadRequestException("Failed to convert request to a message.", e);
            }

            messages.add(message);
        }

        for (Message message : messages) {
            sendToARM(message);
        }

        return conversationId;
    }

    @Override
    public List<Subscription> listSubscriptions() throws MiddlewareException {
        return getRegistry().listSubcriptions();
    }

    @Override
    public List<Subscription> listSubscriptions(String clientId) throws MiddlewareException {
        return getRegistry().listSubcriptions(clientId);
    }

    @Override
    public String unsubscribe(String clientId, String subscriptionId) throws MiddlewareException, NotFoundException, BadRequestException {
        Subscription subscription = getRegistry().findSubscription(clientId, subscriptionId);

        if (subscription == null) {
            throw new NotFoundException(String.format("Subscription with ID %s doesn't exist.", subscriptionId));
        }

        getRegistry().deleteSubscription(subscriptionId);

        List<IoTDevice> devices = getRegistry().getDevices(subscription.getDeviceIds());

        Map<String, List<String>> deviceIdsPerPlatform = new HashMap<>();
        for (IoTDevice device : devices) {
            if (!deviceIdsPerPlatform.containsKey(device.getHostedBy())) {
                deviceIdsPerPlatform.put(device.getHostedBy(), new ArrayList<>());
            }
            deviceIdsPerPlatform.get(device.getHostedBy()).add(device.getDeviceId());
        }

        String conversationId = MessageUtils.generateConversationID();
        List<Message> messages = new ArrayList<>();
        for (String platformId : deviceIdsPerPlatform.keySet()) {
            UnsubscribeReq req = new UnsubscribeReq(subscriptionId, platformId, clientId);
            req.setConversationId(conversationId);
            req.setDeviceIds(deviceIdsPerPlatform.get(platformId));
            Message message;
            try {
                message = req.toMessage();
            } catch (Exception e) {
                throw new BadRequestException("Failed to convert request to a message.", e);
            }

            messages.add(message);
        }

        for (Message message : messages) {
            sendToARM(message);
        }

        return conversationId;
    }

    @Override
    public String subscribePlat2Plat(String clientId, Plat2PlatSubscribeInput input) throws MiddlewareException, BadRequestException, ConflictException {
        List<IoTDevice> devices = getRegistry().getDevices(Arrays.asList(input.getTargetDeviceId(), input.getSourceDeviceId()));
        Map<String, IoTDevice> devicesMap = devices.stream().collect(Collectors.toMap(IoTDevice::getDeviceId, device -> device));
        if (!devicesMap.containsKey(input.getSourceDeviceId())) {
            throw new BadRequestException(String.format("Source device %s is not registered within INTER-MW.",
                    input.getSourceDeviceId()));
        }
        if (!devicesMap.containsKey(input.getTargetDeviceId())) {
            throw new BadRequestException(String.format("Target device %s is not registered within INTER-MW.",
                    input.getTargetDeviceId()));
        }

        Plat2PlatSubscription existingSub =
                registry.findPlat2PlatSubscription(input.getSourceDeviceId(), input.getTargetDeviceId());
        if (existingSub != null) {
            throw new ConflictException(String.format(
                    "Specified source device is already subscribed to specified target device (conversationId %s).",
                    existingSub.getConversationId()));
        }

        String conversationId = MessageUtils.generateConversationID();
        IoTDevice sourceDevice = devicesMap.get(input.getSourceDeviceId());
        IoTDevice targetDevice = devicesMap.get(input.getTargetDeviceId());

        Plat2PlatSubscribeReq req = new Plat2PlatSubscribeReq();
        req.setClientId(clientId);
        req.setConversationId(conversationId);
        req.setSubscriptionId(conversationId);
        req.setSourceDeviceId(sourceDevice.getDeviceId());
        req.setSourcePlatformId(sourceDevice.getHostedBy());
        req.setTargetDeviceId(targetDevice.getDeviceId());
        req.setTargetPlatformId(targetDevice.getHostedBy());

        Message message;
        try {
            message = req.toMessage();
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a VIRTUAL_SUBSCRIBE message.", e);
        }

        return sendToARM(message);
    }

    @Override
    public List<Plat2PlatSubscription> listPlat2PlatSubscriptions() throws MiddlewareException {
        return getRegistry().listPlat2PlatSubscriptions();
    }

    @Override
    public List<Plat2PlatSubscription> listPlat2PlatSubscriptions(String clientId) throws MiddlewareException {
        return getRegistry().listPlat2PlatSubscriptions(clientId);
    }

    @Override
    public String unsubscribePlat2Plat(String clientId, String conversationId) throws MiddlewareException, NotFoundException, BadRequestException {
        Plat2PlatSubscription subscription = getRegistry().getPlat2PlatSubscription(conversationId);

        if (subscription == null) {
            throw new NotFoundException(String.format("Platform-to-platform subscription %s doesn't exist.", conversationId));
        }

        Plat2PlatUnsubscribeReq req = new Plat2PlatUnsubscribeReq();
        req.setClientId(clientId);
        req.setSubscriptionId(conversationId);
        req.setConversationId(MessageUtils.generateConversationID());

        Message message;
        try {
            message = req.toMessage();
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert request to a VIRTUAL_UNSUBSCRIBE message.", e);
        }

        return sendToARM(message);
    }

    @Override
    public String actuate(String clientId, String deviceId, ActuationInput input) throws MiddlewareException, NotFoundException {
        String conversationId = MessageUtils.generateConversationID();

        List<IoTDevice> devices = getRegistry().getDevices(Collections.singletonList(deviceId));

        if (devices.isEmpty()) {
            throw new NotFoundException(String.format("Device with ID %s does not exist", deviceId));
        }
        IoTDevice actuatorDevice = devices.iterator().next();

        Actuation actuation = new Actuation();
        actuation.setClientId(clientId);
        actuation.setPlatformId(actuatorDevice.getHostedBy());
        // If deviceId hosts actuator, then we use madeByActuator, if the device IS the actuator, then we use deviceId
        actuation.setMadeByActuator(input.getActuatorId());
        actuation.setMadeByActuatorLocalId(input.getActuatorLocalId());
        actuation.setDeviceId(deviceId);
        actuation.setActuationResults(input.getActuationResultSet());

        ActuationReq request = new ActuationReq(actuation);
        Message message = request.toMessage(conversationId);

        sendToARM(message);

        return conversationId;
    }

    @Override
    public Message getAllSensorData(String clientId, String platformId) throws MiddlewareException, ExecutionException, InterruptedException, TimeoutException {
        return getSensorDataForDevices(clientId, platformId, null);
    }

    @Override
    public Message getSensorDataForDevices(String clientId, String platformId, @Nullable List<String> deviceIds) throws MiddlewareException, InterruptedException, ExecutionException, TimeoutException {
        List<IoTDevice> sensors;
        if (deviceIds == null) {
            sensors = getRegistry().getDevicesByType(IoTDeviceType.SENSOR, platformId);
        } else {
            sensors = new ArrayList<>();
            for (String deviceId : deviceIds) {
                sensors.add(new IoTDevice(deviceId));
            }
        }
        return querySensorValues(clientId, platformId, sensors);
    }

    @Override
    public String sendMessage(String clientId, String content) throws BadRequestException, MiddlewareException {
        Message message;
        try {
            message = new Message(content);
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON-LD message.", e);
        }
        return sendToARM(message);
    }

    @Override
    public String executeQuery(String query) {
        QueryType queryType = QueryType.extractFromQuery(query);
        QueryReturn queryReturn = getRegistry().executeQuery(query, queryType);
        return queryReturn.resultAsString();
    }

    @Override
    public void registerLocationArea(LocationAreaInput input) throws ConflictException, MiddlewareException {
        if (getRegistry().getLocationArea(input.getPlatformId(), input.getLocationId()) != null) {
            throw new ConflictException(String.format("Static location with id %s already exists", input.getLocationId()));
        }

        getRegistry().registerLocationArea(new LocationArea(
                input.getLocationId(),
                input.getPlatformId(),
                input.getDescription(),
                input.getWktPolygon()
        ));
    }

    @Override
    public void registerLocationPoint(LocationPointInput input) throws ConflictException, MiddlewareException {
        if (getRegistry().getLocationPoint(input.getLocationId()) != null) {
            throw new ConflictException(String.format("Static location with id %s already exists", input.getLocationId()));
        }

        getRegistry().registerLocationPoint(new LocationPoint(
                input.getLocationId(),
                input.getPlatformId(),
                input.getDescription(),
                input.getWktPoint()
        ));
    }

    @Override
    public List<LocationArea> getLocationAreas(String platformId) throws MiddlewareException {
        return getRegistry().getLocationAreas(platformId);
    }

    @Override
    public LocationArea getLocationArea(String platformId, String locationId) throws MiddlewareException, NotFoundException {
        LocationArea locationArea = getRegistry().getLocationArea(platformId, locationId);
        if (locationArea != null) {
            return locationArea;
        } else {
            throw new NotFoundException(String.format("Location area %s is not registered", locationId));
        }

    }

    @Override
    public LocationPoint getLocationPoint(String platformId, String locationId) throws MiddlewareException, NotFoundException {
        LocationPoint locationPoint = getRegistry().getLocationPoint(locationId);
        if (locationPoint != null) {
            return locationPoint;
        } else {
            throw new NotFoundException(String.format("Location point %s is not registered", locationId));
        }
    }

    @Override
    public List<LocationPoint> getLocationPoints(String platformId) throws MiddlewareException {
        return getRegistry().getLocationPoints(platformId);
    }

    @Override
    public void updateLocationArea(String locationId, LocationAreaInput input) throws NotFoundException, MiddlewareException {
        if (getRegistry().getLocationArea(input.getPlatformId(), locationId) != null) {
            getRegistry().updateLocationArea(locationId, new LocationArea(
                    input.getLocationId(),
                    input.getPlatformId(),
                    input.getDescription(),
                    input.getWktPolygon()
            ));
        } else {
            throw new NotFoundException(String.format("Trying to update location %s that is not registered.", locationId));
        }
    }

    @Override
    public void updateLocationPoint(String locationId, LocationPointInput input) throws NotFoundException, MiddlewareException {
        if (getRegistry().getLocationPoint(locationId) != null) {
            getRegistry().updateLocationPoint(locationId, new LocationPoint(
                    input.getLocationId(),
                    input.getPlatformId(),
                    input.getDescription(),
                    input.getWktPoint()
            ));
        } else {
            throw new NotFoundException(String.format("Trying to update location %s that is not registered", locationId));
        }
    }

    @Override
    public void deleteLocationArea(String platformId, String locationId) throws NotFoundException, MiddlewareException {
        if (getRegistry().getLocationArea(platformId, locationId) != null) {
            getRegistry().deleteLocationArea(locationId);
        } else {
            throw new NotFoundException(String.format("Trying to update location %s that is not registered.", locationId));
        }
    }

    @Override
    public void deleteLocationPoint(String locationId) throws NotFoundException, MiddlewareException {
        if (getRegistry().getLocationPoint(locationId) != null) {
            getRegistry().deleteLocationPoint(locationId);
        } else {
            throw new NotFoundException(String.format("Trying to update location %s that is not registered.", locationId));
        }
    }

    @Override
    public List<String> getLocationPointsInsideArea(String platformId, String areaLocationId) throws NotFoundException, MiddlewareException {
        if (getRegistry().getLocationArea(platformId, areaLocationId) != null) {
            return getRegistry().getLocationPointsInsideArea(areaLocationId);
        } else {
            throw new NotFoundException(String.format("Trying to query for location area {} that is not registered.", areaLocationId));
        }
    }

    String sendToARM(Message message) throws MiddlewareException {
        return sendToARM(Collections.singletonList(message)).get(0);
    }

    private List<String> sendToARM(List<Message> messages) throws MiddlewareException {
        List<String> conversationIds = new ArrayList<>();
        for (Message message : messages) {
            String conversationId = apiRequestManager.processDownstream(message);
            conversationIds.add(conversationId);
        }
        return conversationIds;
    }

    private void validateAlignmentsData(Platform platform) throws BadRequestException {
        if (platform.getDownstreamInputAlignmentName() == null && platform.getDownstreamInputAlignmentVersion() == null &&
                platform.getDownstreamOutputAlignmentName() == null && platform.getDownstreamOutputAlignmentVersion() == null &&
                platform.getUpstreamInputAlignmentName() == null && platform.getUpstreamInputAlignmentVersion() == null &&
                platform.getUpstreamOutputAlignmentName() == null && platform.getUpstreamOutputAlignmentVersion() == null) {

            platform.setDownstreamInputAlignmentName("");
            platform.setDownstreamInputAlignmentVersion("");
            platform.setDownstreamOutputAlignmentName("");
            platform.setDownstreamOutputAlignmentVersion("");

            platform.setUpstreamInputAlignmentName("");
            platform.setUpstreamInputAlignmentVersion("");
            platform.setUpstreamOutputAlignmentName("");
            platform.setUpstreamOutputAlignmentVersion("");
        }

        if (platform.getDownstreamInputAlignmentName() == null || platform.getDownstreamInputAlignmentVersion() == null ||
                platform.getDownstreamOutputAlignmentName() == null || platform.getDownstreamOutputAlignmentVersion() == null ||
                platform.getUpstreamInputAlignmentName() == null || platform.getUpstreamInputAlignmentVersion() == null ||
                platform.getUpstreamOutputAlignmentName() == null || platform.getUpstreamOutputAlignmentVersion() == null) {

            throw new BadRequestException("Alignments are not set correctly: one or more fields are null.");
        }

        checkIfOnlyOneSpecified(platform.getDownstreamInputAlignmentName(), platform.getDownstreamInputAlignmentVersion());
        checkIfOnlyOneSpecified(platform.getDownstreamOutputAlignmentName(), platform.getDownstreamOutputAlignmentVersion());
        checkIfOnlyOneSpecified(platform.getUpstreamInputAlignmentName(), platform.getUpstreamInputAlignmentVersion());
        checkIfOnlyOneSpecified(platform.getUpstreamOutputAlignmentName(), platform.getUpstreamOutputAlignmentVersion());
    }

    private void checkIfOnlyOneSpecified(String a, String b) throws BadRequestException {
        if (a.isEmpty() && !b.isEmpty() ||
                !a.isEmpty() && b.isEmpty()) {
            throw new BadRequestException("Alignment name and version must be specified both or none.");
        }
    }

    private Message querySensorValues(String clientId, String platformId, List<IoTDevice> ioTDevicesFilter) throws MiddlewareException, InterruptedException, ExecutionException, TimeoutException {
        SensorQueryReq request = new SensorQueryReq(ioTDevicesFilter, clientId, platformId);

        Message requestMessage = request.toMessage();
        String conversationIdFromArm = sendToARM(requestMessage);

        QueryMessageFetcher messageFetcher = new QueryMessageFetcher(conversationIdFromArm);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Message> futureMessage = service.submit(messageFetcher);

        return futureMessage.get(configuration.getQueryResponseTimeout(), TimeUnit.SECONDS);
    }

    public ParliamentRegistry getRegistry() {
        return registry;
    }

    private class QueryMessageFetcher implements Callable<Message> {
        private String conversationId;

        QueryMessageFetcher(String conversationId) {
            this.conversationId = conversationId;
        }

        @Override
        public Message call() throws Exception {
            Message message = null;
            while (message == null) {
                message = apiRequestManager.getQueryResponseMessage(conversationId);
                Thread.sleep(500);
            }

            return message;
        }
    }
}
