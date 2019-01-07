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
package eu.interiot.intermw.api.rest.resource;

/**
 * Created by flavio_fuart on 27-Jun-17.
 */

import eu.interiot.intermw.api.InterMwApiImpl;
import eu.interiot.intermw.api.exception.BadRequestException;
import eu.interiot.intermw.api.exception.ConflictException;
import eu.interiot.intermw.api.exception.NotFoundException;
import eu.interiot.intermw.api.model.*;
import eu.interiot.intermw.api.rest.model.MwAsyncResponse;
import eu.interiot.intermw.comm.arm.ResponseMessageParser;
import eu.interiot.intermw.commons.Context;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.*;
import eu.interiot.message.Message;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@SwaggerDefinition(
        info = @Info(
                description = "INTER-IoT Middleware Layer Interoperability Components",
                version = "2.5.1-SNAPSHOT",
                title = "MW2MW",
                termsOfService = "http://www.inter-iot-project.eu/",
                contact = @Contact(
                        name = "Interiot contact",
                        email = "coordinator@inter-iot.eu",
                        url = "http://www.inter-iot-project.eu/"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        basePath = "/api",
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        externalDocs = @ExternalDocs(value = "Project deliverables", url = "http://www.inter-iot-project.eu/deliverables"),
        securityDefinition = @SecurityDefinition(
                apiKeyAuthDefinitions = {
                        @ApiKeyAuthDefinition(in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER, name = "Authorization",
                                key = "OAuth2 authorization using WSO2 Identity Server")
                },
                basicAuthDefinitions = {
                        @BasicAuthDefinition(key = "Basic authorization")
                }
        )
)
@Path("/mw2mw")
@Api(tags = {"mw2mw"}, authorizations = {
        @Authorization(value = "OAuth2 authorization using WSO2 Identity Server"),
        @Authorization(value = "Basic authorization")
})
public class InterMwApiREST {

    private static final Logger logger = LoggerFactory.getLogger(InterMwApiREST.class);
    private InterMwApiImpl interMwApi;

    @Inject
    @javax.ws.rs.core.Context
    private SecurityContext securityContext;

    public InterMwApiREST() throws MiddlewareException {
        interMwApi = new InterMwApiImpl(Context.getConfiguration());
    }

    @GET
    @Path("/clients")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List all clients",
            tags = {"Clients"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Client.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listClients() throws MiddlewareException {

        List<Client> clients = interMwApi.listClients();
        return Response.ok(clients).build();
    }

    @POST
    @Path("/clients")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Register a new client",
            tags = {"Clients"})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Success.", response = Client.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 409, message = "Client is already registered.")})
    public Response registerClient(RegisterClientInput input,
                                   @javax.ws.rs.core.Context UriInfo uriInfo) throws MiddlewareException, ConflictException, BadRequestException {

        Client client = convertRegisterClientInput(input);

        interMwApi.registerClient(client);
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.path("{clientId}");
        URI location = uriBuilder.build(client.getClientId());
        return Response.created(location).entity(client).build();
    }

    @GET
    @Path("/clients/{clientId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve specified client",
            tags = {"Clients"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Client.class),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified client does not exist.")})
    public Response getClient(@PathParam("clientId") String clientId) throws MiddlewareException, NotFoundException {

        Client client = interMwApi.getClient(clientId);
        if (client == null) {
            throw new NotFoundException(String.format("Client %s does not exist.", clientId));
        }
        return Response.ok(client).build();
    }

    @PUT
    @Path("/clients/{clientId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update specified client",
            tags = {"Clients"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Client.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified client does not exist.")})
    public Response updateClient(@PathParam("clientId") String clientId,
                                 UpdateClientInput input) throws MiddlewareException, BadRequestException, NotFoundException {

        interMwApi.updateClient(clientId, input);

        Client updatedClient = interMwApi.getClient(clientId);
        return Response.ok(updatedClient).build();
    }

    @DELETE
    @Path("/clients/{clientId}")
    @ApiOperation(value = "Remove specified client",
            tags = {"Clients"})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Success."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified client does not exist.")})
    public Response removeClient(@PathParam("clientId") String clientId) throws MiddlewareException, NotFoundException {

        interMwApi.removeClient(clientId);
        return Response.noContent().build();
    }

    @GET
    @Path("/platform-types")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List all supported platform types",
            notes = "List all platform types for which corresponding bridge is available and loaded by INTER-MW.",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listPlatformTypes() throws MiddlewareException {

        String conversationId = interMwApi.listPlatformTypes(getClientId());
        return createMyAsyncResponse(conversationId);
    }

    @GET
    @Path("/platforms")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List all platforms registered with InterIoT",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Platform.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listPlatforms() throws MiddlewareException {

        List<Platform> platforms = interMwApi.listPlatforms();
        return Response.ok(platforms).build();
    }

    @GET
    @Path("/platforms/{platformId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve specified platform",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Platform.class),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified platform does not exist.")})
    public Response getPlatform(@PathParam("platformId") String platformId) throws MiddlewareException, NotFoundException {

        Platform platform = interMwApi.getPlatform(platformId);
        if (platform == null) {
            throw new NotFoundException(String.format("Platform %s does not exist.", platformId));
        }
        return Response.ok(platform).build();
    }

    @POST
    @Path("/platforms")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Register a new platform instance",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 409, message = "Platform is already registered by given client.")})
    public Response registerPlatform(RegisterPlatformInput input)
            throws MiddlewareException, ConflictException, BadRequestException {

        Platform platform = new Platform();
        platform.setClientId(getClientId());
        platform.setEncryptedPassword(input.getEncryptedPassword());
        platform.setEncryptionAlgorithm(input.getEncryptionAlgorithm());
        platform.setLocationId(input.getLocationId());
        platform.setName(input.getName());
        platform.setPlatformId(input.getPlatformId());
        platform.setType(input.getType());
        platform.setUsername(input.getUsername());

        platform.setDownstreamInputAlignmentName(input.getDownstreamInputAlignmentName());
        platform.setDownstreamInputAlignmentVersion(input.getDownstreamInputAlignmentVersion());
        platform.setDownstreamOutputAlignmentName(input.getDownstreamOutputAlignmentName());
        platform.setDownstreamOutputAlignmentVersion(input.getDownstreamOutputAlignmentVersion());

        platform.setUpstreamInputAlignmentName(input.getUpstreamInputAlignmentName());
        platform.setUpstreamInputAlignmentVersion(input.getUpstreamInputAlignmentVersion());
        platform.setUpstreamOutputAlignmentName(input.getUpstreamOutputAlignmentName());
        platform.setUpstreamOutputAlignmentVersion(input.getUpstreamOutputAlignmentVersion());

        try {
            platform.setBaseEndpoint(new URL(input.getBaseEndpoint()));
        } catch (MalformedURLException e) {
            throw new BadRequestException(String.format("Invalid baseEndpoint: %s.", input.getBaseEndpoint()));
        }

        String conversationId = interMwApi.registerPlatform(platform);
        return createMyAsyncResponse(conversationId);
    }

    @PUT
    @Path("/platforms/{platformId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update specified platform instance",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified platform does not exist.")})
    public Response updatePlatform(@PathParam("platformId") String platformId, UpdatePlatformInput input)
            throws MiddlewareException, BadRequestException, NotFoundException {

        String conversationId = interMwApi.updatePlatform(getClientId(), platformId, input);
        return createMyAsyncResponse(conversationId);
    }

    @DELETE
    @Path("/platforms/{platformId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Remove specified platform instance",
            notes = "Removes specified platform from the registry and undeploys the bridge.",
            tags = {"Platforms"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Platform does not exist.")})
    public Response removePlatform(@PathParam("platformId") String platformId)
            throws MiddlewareException, NotFoundException, BadRequestException {

        String conversationId = interMwApi.removePlatform(getClientId(), platformId);
        return createMyAsyncResponse(conversationId);
    }

    @GET
    @Path("/devices")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List all devices registered with InterIoT according to the specified filter",
            tags = {"Devices"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = IoTDevice.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listDevices(@QueryParam("platformId") String platformId) throws MiddlewareException {

        List<IoTDevice> devices = interMwApi.listDevices(getClientId(), platformId);
        return Response.ok(devices).build();
    }

    @POST
    @Path("/devices")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Register (start managing) devices",
            tags = {"Devices"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 409, message = "One or more devices are already registered.")})
    public Response platformCreateDevices(PlatformCreateDeviceInput input)
            throws MiddlewareException, BadRequestException, ConflictException {

        String conversationId = interMwApi.platformCreateDevices(getClientId(), input.getDevices());
        return createMyAsyncResponse(conversationId);
    }

    @PUT
    @Path("/devices/{deviceId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update specified device",
            tags = {"Devices"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 404, message = "Device does not exist.")})
    public Response platform2Device(@PathParam("deviceId") String deviceId,
                                    IoTDevice device) throws MiddlewareException, BadRequestException {

        if (!deviceId.equals(device.getDeviceId())) {
            throw new BadRequestException("DeviceID in path and body doesn't match.");
        }
        String conversationId = interMwApi.platformUpdateDevice(getClientId(), device);
        return createMyAsyncResponse(conversationId);
    }

    @DELETE
    @Path("/devices/{deviceId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Delete specified device",
            tags = {"Devices"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 404, message = "Device does not exist.")})
    public Response platformDeleteDevice(@PathParam("deviceId") String deviceId) throws MiddlewareException, BadRequestException {
        List<String> devices = new ArrayList<>();
        devices.add(deviceId);
        String conversationId = interMwApi.platformDeleteDevices(getClientId(), devices);
        return createMyAsyncResponse(conversationId);
    }

    @POST
    @Path("/devices/query")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query for devices",
            tags = {"Queries"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class)})
    public Response queryDevices(IoTDeviceFilter ioTDeviceFilter) throws MiddlewareException {
        List<IoTDevice> devices = interMwApi.deviceDiscoveryQuery(getClientId(), ioTDeviceFilter);
        return Response.ok(devices).build();
    }


    @POST
    @Path("/query")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @ApiOperation(value = "Execute SELECT, CONSTRUCT or ASK sparql queries directly on the intermw persistence store",
            tags = {"Queries"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The request has been executed successfully.", response = Response.class)})
    public Response queryAll(String query) {
        String result = interMwApi.executeQuery(query);
        return Response.ok(result).build();
    }

    @POST
    @Path("/subscriptions")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Subscribe to specified devices",
            tags = {"Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 409, message = "Client is already subscribed to specified devices.")})
    public Response subscribe(SubscribeInput input)
            throws MiddlewareException, BadRequestException {

        String conversationId = interMwApi.subscribe(getClientId(), input.getDeviceIds());
        return createMyAsyncResponse(conversationId);
    }

    @GET
    @Path("/subscriptions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions",
            tags = {"Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Subscription.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listSubscriptions(@QueryParam("clientId") String clientId) throws MiddlewareException {

        List<Subscription> subscriptions = interMwApi.listSubscriptions(clientId);
        return Response.ok(subscriptions).build();
    }

    @DELETE
    @Path("/subscriptions/{conversationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Unsubscribe from specified conversation",
            tags = {"Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified conversation doesn't exist.")})
    public Response unsubscribe(@PathParam("conversationId") String cnversationId)
            throws MiddlewareException, NotFoundException, BadRequestException {

        String unsubscribeConversationId = interMwApi.unsubscribe(getClientId(), cnversationId);
        return createMyAsyncResponse(unsubscribeConversationId);
    }

    @POST
    @Path("/subscriptions-platform-to-platform")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Subscribe specified target device to observations from specified source device",
            tags = {"Platform-to-Platform Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 409, message = "Subscription already exist - specified target device is already subscribed to specified source device.")})
    public Response subscribePlat2Plat(Plat2PlatSubscribeInput input) throws MiddlewareException, BadRequestException, ConflictException {

        String conversationId = interMwApi.subscribePlat2Plat(getClientId(), input);
        return createMyAsyncResponse(conversationId);
    }

    @GET
    @Path("/subscriptions-platform-to-platform")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List registered platform-to-platform subscriptions",
            tags = {"Platform-to-Platform Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.", response = Plat2PlatSubscription.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response listPlat2PlatSubscriptions(@QueryParam("clientId") String clientId) throws MiddlewareException {

        List<Plat2PlatSubscription> subscriptions = interMwApi.listPlat2PlatSubscriptions(clientId);
        return Response.ok(subscriptions).build();
    }

    @DELETE
    @Path("/subscriptions-platform-to-platform/{conversationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Cancel (unsubscribe from) specified platform-to-platform subscription",
            tags = {"Platform-to-Platform Subscriptions"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized."),
            @ApiResponse(code = 404, message = "Specified subscription doesn't exist.")})
    public Response unsubscribePlat2Plat(@PathParam("conversationId") String conversationId)
            throws MiddlewareException, NotFoundException, BadRequestException {

        String unsubscribeConversationId = interMwApi.unsubscribePlat2Plat(getClientId(), conversationId);
        return createMyAsyncResponse(unsubscribeConversationId);
    }

    @POST
    @Path("/devices/{deviceId}/actuation")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Send actuation message",
            notes = "Send actuation message to actuators",
            tags = {"Actuation"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request.")
    })
    public Response actuation(@PathParam("deviceId") String deviceId, ActuationInput input) throws MiddlewareException, NotFoundException {

        String conversationId = interMwApi.actuate(getClientId(), deviceId, input);
        return createMyAsyncResponse(conversationId);
    }

    @GET
    @Path("/devices/data")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query sensors for data",
            notes = "Query all intermw sensors for data",
            tags = "Queries")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request.")
    })
    public Response queryAllPlatformDeviceSensorData(@QueryParam("platformId") String platformId) throws MiddlewareException, InterruptedException, ExecutionException, TimeoutException {
        Message message = interMwApi.getAllSensorData(getClientId(), platformId);
        return Response.ok(message).build();
    }

    @GET
    @Path("/devices/data/{deviceId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query one device sensor data",
            notes = "Query one intermw sensor for data",
            tags = "Queries")
    public Response queryDeviceSensorData(@QueryParam("platformId") String platformId, @PathParam("deviceId") String deviceId) throws InterruptedException, MiddlewareException, TimeoutException, ExecutionException {
        Message message = interMwApi.getSensorDataForDevices(getClientId(), platformId, Collections.singletonList(deviceId));
        return Response.ok(message).build();
    }

    @POST
    @Path("/location/areas")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Register a location area",
            notes = "Register a location area, which is represented by a WKT polygon. " +
                    "The list must begin and end on the same area",
            tags = "Locations")
    public Response registerLocationArea(LocationAreaInput input) throws ConflictException, MiddlewareException {
        interMwApi.registerLocationArea(input);
        return Response.ok().build();
    }

    @POST
    @Path("/location/points")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Register a location point",
            notes = "Register a location point, which is represented by a WKT point.",
            tags = "Locations")
    public Response registerLocationPoint(LocationPointInput input) throws ConflictException, MiddlewareException {
        interMwApi.registerLocationPoint(input);
        return Response.ok().build();
    }

    @GET
    @Path("/location/areas")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all location areas",
            notes = "Get all location areas",
            tags = "Locations")
    public Response getLocationAreas(@QueryParam("platformId") String platformId) throws MiddlewareException {
        return Response.ok(interMwApi.getLocationAreas(platformId)).build();


    }

    @GET
    @Path("/location/areas/{locationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query one location area",
            notes = "Query one location area, which is represented by a WKT polygon.",
            tags = "Locations")
    public Response getLocationArea(@QueryParam("platformId") String platformId, @PathParam("locationId") String locationId) throws MiddlewareException, NotFoundException {
        return Response.ok(interMwApi.getLocationArea(platformId, locationId)).build();
    }

    @GET
    @Path("/location/points")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query all dynamic locations",
            notes = "Query all location points, which are represented by WKT points.",
            tags = "Locations")
    public Response getLocationPoints(@QueryParam("platformId") String platformId) throws MiddlewareException {
        return Response.ok(interMwApi.getLocationPoints(platformId)).build();
    }


    @GET
    @Path("/location/points/{locationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Query one dynamic location",
            notes = "Query one location point, which is represented by WKT point.",
            tags = "Locations")
    public Response getLocationPoint(@QueryParam("platformId") String platformId, @PathParam("locationId") String locationId) throws MiddlewareException, NotFoundException {
        return Response.ok(interMwApi.getLocationPoint(platformId, locationId)).build();
    }

    @PUT
    @Path("/location/areas/{locationId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update one location area",
            notes = "Update one location area, which is represented by a WKT polygon.",
            tags = "Locations")
    public Response updateLocationArea(@PathParam("locationId") String locationId, LocationAreaInput input) throws BadRequestException, NotFoundException, MiddlewareException {
        if (!locationId.equals(input.getLocationId())) {
            throw new BadRequestException("LocationID in path and body doesn't match.");
        }

        interMwApi.updateLocationArea(locationId, input);
        return Response.ok().build();
    }

    @PUT
    @Path("/location/points/{locationId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update one location point",
            notes = "Update one location point, which is represented by WKT point.",
            tags = "Locations")
    public Response updateLocationPoint(@PathParam("locationId") String locationId, LocationPointInput input) throws BadRequestException, NotFoundException, MiddlewareException {
        if (!locationId.equals(input.getLocationId())) {
            throw new BadRequestException("LocationID in path and body doesn't match.");
        }

        interMwApi.updateLocationPoint(locationId, input);
        return Response.ok().build();
    }

    @DELETE
    @Path("/location/areas/{locationId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Delete one static location",
            notes = "Delete one location area",
            tags = "Locations")
    public Response deleteLocationArea(@QueryParam("platformId") String platformId, @PathParam("locationId") String locationId) throws NotFoundException, MiddlewareException {
        interMwApi.deleteLocationArea(platformId, locationId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/location/points/{locationId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Delete one location point",
            notes = "Delete one location point",
            tags = "Locations")
    public Response deleteLocationPoint(@PathParam("locationId") String locationId) throws NotFoundException, MiddlewareException {
        interMwApi.deleteLocationPoint(locationId);
        return Response.ok().build();
    }

    @GET
    @Path("/location/areas/{locationId}/points")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Get ids of points that are inside a location area",
            notes = "Get ids of points that are inside a location area",
            tags = "Locations")
    public Response getLocationPointsInsideArea(@QueryParam("platformId") String platformId, @PathParam("locationId") String locationId) throws NotFoundException, MiddlewareException {
        List<String> locationPointsInsideArea = interMwApi.getLocationPointsInsideArea(platformId, locationId);
        return Response.ok(locationPointsInsideArea).build();
    }


    @POST
    @Path("/responses")
    @Produces({"application/ld+json", "application/json"})
    @ApiOperation(value = "Retrieve response messages concerning the client",
            notes = "Retrieves response messages concerning the client waiting in the queue, if any. " +
                    "Maximum number of messages returned is specified at client registration. Returns array of messages in JSON-LD format " +
                    "or empty array if none is available.",
            tags = {"Messages"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success."),
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response retrieveResponseMessages()
            throws MiddlewareException, BadRequestException {

        String clientId = getClientId();
        Client client = interMwApi.getClient(clientId);
        List<Message> messages = interMwApi.retrieveResponseMessages(clientId);

        String mediaType;
        switch (client.getResponseFormat()) {
            case JSON_LD:
                mediaType = "application/ld+json";
                break;
            case JSON:
                mediaType = APPLICATION_JSON;
                break;
            default:
                throw new BadRequestException(String.format("Unsupported response format: %s.", client.getResponseFormat()));
        }

        String responseEntity = ResponseMessageParser.convertMessages(client.getResponseFormat(), messages);
        return Response.ok(responseEntity, mediaType).build();
    }

    @POST
    @Path("/requests")
    @Consumes("application/ld+json")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Send given JSON-LD message downstream towards the platform",
            notes = "A raw method for sending message in JSON-LD format downstream.",
            tags = {"Messages"})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The request has been accepted for processing.", response = MwAsyncResponse.class),
            @ApiResponse(code = 400, message = "Invalid request (invalid JSON-LD message)."),
            @ApiResponse(code = 401, message = "Unauthorized.")})
    public Response sendMessage(String content) throws MiddlewareException, BadRequestException {

        String conversationId = interMwApi.sendMessage(getClientId(), content);
        return createMyAsyncResponse(conversationId);
    }

    private Client convertRegisterClientInput(RegisterClientInput input) throws BadRequestException {
        Client client = new Client();
        client.setClientId(input.getClientId());
        client.setReceivingCapacity(input.getReceivingCapacity());
        client.setResponseDelivery(input.getResponseDelivery());
        client.setResponseFormat(input.getResponseFormat());

        if (input.getResponseDelivery() == Client.ResponseDelivery.SERVER_PUSH && input.getCallbackUrl() == null) {
            throw new BadRequestException("CallbackUrl attribute must be specified when using SERVER_PUSH response delivery.");
        }
        if (input.getCallbackUrl() != null) {
            try {
                client.setCallbackUrl(new URL(input.getCallbackUrl()));
            } catch (MalformedURLException e) {
                throw new BadRequestException(String.format("Invalid callbackURL: %s.", input.getCallbackUrl()));
            }
        }

        return client;
    }

    private String getClientId() {
        return securityContext.getUserPrincipal().getName();
    }

    private Response createMyAsyncResponse(String conversationId) {
        MwAsyncResponse response = new MwAsyncResponse(conversationId);
        return Response.accepted(response).build();
    }
}
