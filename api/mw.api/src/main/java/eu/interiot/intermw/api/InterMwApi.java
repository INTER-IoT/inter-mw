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
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.ApiCallback;
import eu.interiot.intermw.commons.model.*;
import eu.interiot.message.Message;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * General API for the Intermw module. It provides functionalities to let IoT
 * Open Platforms interoperate.
 *
 * @author aromeu
 */
public interface InterMwApi {

    Client getClient(String clientId) throws MiddlewareException;

    List<Client> listClients() throws MiddlewareException;

    void registerClient(Client client) throws MiddlewareException, ConflictException, BadRequestException;

    void registerClient(Client client, ApiCallback<Message> apiCallback) throws MiddlewareException, ConflictException, BadRequestException;

    void updateClient(String clientId, UpdateClientInput input) throws MiddlewareException, NotFoundException, BadRequestException;

    void removeClient(String clientId) throws MiddlewareException, NotFoundException;

    Message retrieveResponseMessage(String clientId, long timeoutMillis) throws MiddlewareException;

    List<Message> retrieveResponseMessages(String clientId) throws MiddlewareException;

    String listPlatformTypes(String clientId) throws MiddlewareException;

    List<Platform> listPlatforms() throws MiddlewareException;

    String registerPlatform(Platform platform) throws MiddlewareException, ConflictException, BadRequestException;

    Platform getPlatform(String platformId) throws MiddlewareException;

    String updatePlatform(Platform platform) throws MiddlewareException, BadRequestException, NotFoundException;

    String updatePlatform(String clientId, String platformId, UpdatePlatformInput input) throws MiddlewareException, BadRequestException, NotFoundException;

    String removePlatform(String clientId, String platformId) throws MiddlewareException, NotFoundException, BadRequestException;

    String platformCreateDevices(String clientId, List<IoTDevice> devices) throws MiddlewareException, BadRequestException, ConflictException;

    String platformUpdateDevice(String clientId, IoTDevice device) throws MiddlewareException, BadRequestException;

    String platformUpdateDevices(String clientId, List<IoTDevice> devices) throws MiddlewareException, BadRequestException;

    String platformDeleteDevices(String clientId, List<String> deviceIds) throws MiddlewareException, NotFoundException, BadRequestException;

    List<IoTDevice> listDevices(String clientId, String platformId) throws MiddlewareException;

    List<IoTDevice> deviceDiscoveryQuery(String clientId, IoTDeviceFilter query) throws MiddlewareException;

    String syncDevices(String clientId, String platformId) throws MiddlewareException;

    String subscribe(String clientId, List<String> deviceIds) throws MiddlewareException, BadRequestException;

    List<Subscription> listSubscriptions() throws MiddlewareException;

    List<Subscription> listSubscriptions(String clientId) throws MiddlewareException;

    String unsubscribe(String clientId, String subscriptionId) throws MiddlewareException, NotFoundException, BadRequestException;

    String subscribePlat2Plat(String clientId, Plat2PlatSubscribeInput input) throws MiddlewareException, BadRequestException, ConflictException;

    List<Plat2PlatSubscription> listPlat2PlatSubscriptions() throws MiddlewareException;

    List<Plat2PlatSubscription> listPlat2PlatSubscriptions(String clientId) throws MiddlewareException;

    String unsubscribePlat2Plat(String clientId, String conversationId) throws MiddlewareException, NotFoundException, BadRequestException;

    String actuate(String clientId, String actuatorId, ActuationInput input) throws MiddlewareException, NotFoundException;

    Message getAllSensorData(String clientId, String platformId) throws MiddlewareException, ExecutionException, InterruptedException, IOException, TimeoutException;

    Message getSensorDataForDevices(String clientId, String platformId, @Nullable List<String> deviceIds) throws MiddlewareException, InterruptedException, ExecutionException, TimeoutException;

    String sendMessage(String clientId, String content) throws BadRequestException, MiddlewareException;

    String executeQuery(String query);

    void registerLocationArea(LocationAreaInput input) throws ConflictException, MiddlewareException;

    void registerLocationPoint(LocationPointInput input) throws ConflictException, MiddlewareException;

    List<LocationArea> getLocationAreas(String platformId) throws MiddlewareException;

    LocationArea getLocationArea(String platformId, String locationId) throws MiddlewareException, NotFoundException;

    LocationPoint getLocationPoint(String platformId, String locationId) throws MiddlewareException, NotFoundException;

    List<LocationPoint> getLocationPoints(String platformId) throws MiddlewareException;

    void updateLocationArea(String locationId, LocationAreaInput input) throws MiddlewareException, NotFoundException;

    void updateLocationPoint(String locationId, LocationPointInput input) throws MiddlewareException, NotFoundException;

    void deleteLocationArea(String platformId, String locationId) throws NotFoundException, MiddlewareException;

    void deleteLocationPoint(String locationId) throws NotFoundException, MiddlewareException;

    List<String> getLocationPointsInsideArea(String platformId, String locationId) throws NotFoundException, MiddlewareException;
}
