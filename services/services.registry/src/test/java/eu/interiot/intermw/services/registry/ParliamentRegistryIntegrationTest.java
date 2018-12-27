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
package eu.interiot.intermw.services.registry;

import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.*;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ParliamentRegistryIntegrationTest {
    private static final String CLIENT_ID1 = "myclient1";
    private static final String CLIENT_ID2 = "myclient2";
    private static final String PLATFORM1_ID = "http://inter-iot.eu/example-platform1";
    private static final String PLATFORM2_ID = "http://inter-iot.eu/example-platform2";
    private static final String LOCATION1_ID = "http://inter-iot.eu/example-location1";
    private static final String LOCATION2_ID = "http://inter-iot.eu/example-location2";
    private static final String LOCATION_AREA_ID = "http://inter-iot.eu/example-location-area";
    private static final String LOCATION_AREA_ID2 = "http://inter-iot.eu/example-location-area-2";
    private static final String LOCATION_POINT_ID = "http://inter-iot.eu/example-location-point";
    private static final String LOCATION_POINT_ID2 = "http://inter-iot.eu/example-location-point-2";

    private static final String NAMESPACE = "http://inter-iot.eu/";
    private static final String DEVICE_ID_PREFIX = "http://inter-iot.eu/Mydevice_";
    public static final String DEVICE_NAME_PREFIX = "Mydevice_";
    //    private ParliamentRegistry registry;
    private ParliamentRegistry registry;

    @Before
    public void setUp() throws MiddlewareException {
        Configuration conf = new DefaultConfiguration("intermw-test.properties");

        try (RDFConnection conn = RDFConnectionFactory.connect(conf.getParliamentUrl(), "sparql", "sparql", "sparql")) {
            String allGraphsQuery = "SELECT DISTINCT ?g \n" +
                    "WHERE {\n" +
                    "  GRAPH ?g {}\n" +
                    "}";
            QueryExecution allGraphsQueryExecution = conn.query(allGraphsQuery);
            ResultSet allGraphsResultSet = allGraphsQueryExecution.execSelect();

            UpdateRequest updateRequest = new UpdateRequest();
            while (allGraphsResultSet.hasNext()) {
                QuerySolution next = allGraphsResultSet.next();
                String graphName = next.get("g").asResource().toString();

                if (graphName.contains("Default Graph") || graphName.contains("http://parliament.semwebcentral.org/parliament#MasterGraph")) {
                    continue;
                }

                updateRequest.add("DROP GRAPH <" + graphName + ">;");
            }
            if (!updateRequest.getOperations().isEmpty()) {
                conn.update(updateRequest);
            }
        }
//        registry = new ParliamentRegistry(conf);
        registry = new ParliamentRegistry(conf);
    }


    // TODO fix this
    @Test
    public void testParliamentRegistry() throws Exception {
        registerClient();
        registerPlatform();
        registerDevices();
        updateDevices();
        removeDevices();
        subscribe();
        removePlatform();
        removeClient();
    }


    @Test
    public void testListClients() throws Exception {
        Client client1 = new Client();
        client1.setClientId(CLIENT_ID1);
        client1.setReceivingCapacity(10);
        client1.setResponseDelivery(Client.ResponseDelivery.CLIENT_PULL);
        client1.setResponseFormat(Client.ResponseFormat.JSON_LD);
        registry.registerClient(client1);

        Client client2 = new Client();
        client2.setClientId(CLIENT_ID2);
        client1.setCallbackUrl(new URL("http://localhost:9090"));
        client2.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        client2.setResponseFormat(Client.ResponseFormat.JSON_LD);
        registry.registerClient(client2);

        List<Client> clients = registry.listClients();
        assertEquals(2, clients.size());
        assertTrue(clients.contains(client1));
        assertTrue(clients.contains(client2));
    }

    @Test
    public void testListSubscriptions() throws Exception {
        // subscription 1
        String conversationId1 = "conversationId1";
        Subscription subscription1 = new Subscription();
        subscription1.setClientId(CLIENT_ID1);
        subscription1.setConversationId(conversationId1);
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "1");
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "2");
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "3");
        registry.subscribe(subscription1);

        // subscription 2
        String conversationId2 = "conversationId2";
        Subscription subscription2 = new Subscription();
        subscription2.setClientId(CLIENT_ID2);
        subscription2.setConversationId(conversationId2);
        subscription2.addDeviceId(DEVICE_ID_PREFIX + "20");
        subscription2.addDeviceId(DEVICE_ID_PREFIX + "21");
        registry.subscribe(subscription2);

        List<Subscription> subscriptions = registry.listSubcriptions();
        assertEquals(subscriptions.size(), 2);
        List<Subscription> subscriptionsSorted = subscriptions.stream().sorted((o1, o2) -> o1.getConversationId().compareTo(o2.getConversationId()))
                .collect(Collectors.toList());
        assertEquals(subscriptionsSorted.get(0).getConversationId(), conversationId1);
        assertEquals(subscriptionsSorted.get(1).getConversationId(), conversationId2);
        List<String> deviceIds = subscriptionsSorted.get(0).getDeviceIds();
        assertEquals(deviceIds.size(), 3);
        List<String> deviceIdsSorted = deviceIds.stream().sorted(String::compareTo)
                .collect(Collectors.toList());
        assertEquals(deviceIdsSorted.get(0), subscription1.getDeviceIds().get(0));
        assertEquals(deviceIdsSorted.get(1), subscription1.getDeviceIds().get(1));
        assertEquals(deviceIdsSorted.get(2), subscription1.getDeviceIds().get(2));

        List<Subscription> subscriptions1 = registry.listSubcriptions(CLIENT_ID1);
        assertEquals(subscriptions1.size(), 1);
    }

    @Test
    public void testUpdatePlatform() throws Exception {
        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM1_ID);
        platform.setName("My Platform 1");
        platform.setType("ExamplePlatform");
        platform.setBaseEndpoint(new URL("http://localhost:4568"));
        platform.setLocationId(LOCATION1_ID);
        platform.setClientId(CLIENT_ID1);
        platform.setUsername("username");
        platform.setEncryptedPassword("password");
        platform.setEncryptionAlgorithm("SHA-256");
        platform.setDownstreamInputAlignmentName("");
        platform.setDownstreamInputAlignmentVersion("");
        platform.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
        platform.setDownstreamOutputAlignmentVersion("1.0");
        platform.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
        platform.setUpstreamInputAlignmentVersion("1.0");
        platform.setUpstreamOutputAlignmentName("");
        platform.setUpstreamOutputAlignmentVersion("");

        registry.registerPlatform(platform);

        Platform platformUpdate = registry.getPlatformById(PLATFORM1_ID);
        platformUpdate.setName("UPDATED");
        platformUpdate.setBaseEndpoint(new URL("http://localhost:4568/UPDATED"));
        platformUpdate.setLocationId("http://inter-iot.eu/UPDATED");
        platformUpdate.setClientId(CLIENT_ID1 + "UPDATED");
        platformUpdate.setUsername("UPDATED");
        platformUpdate.setEncryptedPassword("UPDATED");
        platformUpdate.setEncryptionAlgorithm("UPDATED");
        platformUpdate.setDownstreamInputAlignmentName("");
        platformUpdate.setDownstreamInputAlignmentVersion("");
        platformUpdate.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
        platformUpdate.setDownstreamOutputAlignmentVersion("1.0");
        platformUpdate.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
        platformUpdate.setUpstreamInputAlignmentVersion("1.0");
        platformUpdate.setUpstreamOutputAlignmentName("");
        platformUpdate.setUpstreamOutputAlignmentVersion("");

        registry.updatePlatform(platformUpdate);

        Platform platform1 = registry.getPlatformById(PLATFORM1_ID);
        assertEquals(platform1.getName(), platformUpdate.getName());
        assertEquals(platform1.getType(), platformUpdate.getType());
        assertEquals(platform1.getBaseEndpoint(), platformUpdate.getBaseEndpoint());
        assertEquals(platform1.getLocationId(), platformUpdate.getLocationId());
        assertEquals("http://inter-iot.eu/clients#myclient1UPDATED", platform1.getClientId());
        assertEquals(platform1.getUsername(), platformUpdate.getUsername());
        assertEquals(platform1.getEncryptedPassword(), platformUpdate.getEncryptedPassword());
        assertEquals(platform1.getEncryptionAlgorithm(), platformUpdate.getEncryptionAlgorithm());
    }


    @Test
    public void testGetSensors() throws MiddlewareException {
        List<IoTDevice> sensorDevices = generateIoTDevices(
                DEVICE_ID_PREFIX + "SensorDevice",
                1,
                10,
                PLATFORM1_ID,
                LOCATION1_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE, IoTDeviceType.SENSOR)
        );

        List<IoTDevice> actuatorDevices = generateIoTDevices(
                DEVICE_ID_PREFIX + "ActuatorDevice",
                1,
                10,
                PLATFORM1_ID,
                LOCATION1_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE, IoTDeviceType.ACTUATOR)
        );

        List<IoTDevice> devices = generateIoTDevices(
                DEVICE_ID_PREFIX + "OtherDevice",
                1,
                10,
                PLATFORM1_ID,
                LOCATION1_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE)
        );
        devices.addAll(sensorDevices);
        devices.addAll(actuatorDevices);


        registry.registerDevices(devices);

        List<IoTDevice> registrySensorDevices = registry.getDevicesByType(IoTDeviceType.SENSOR, PLATFORM1_ID);

        assertEquals("Registry should return correct number of sensor devices", 10, registrySensorDevices.size());

        for (IoTDevice device : registrySensorDevices) {
            assertTrue("Each sensor device should be of type SENSOR", device.getDeviceTypes().contains(IoTDeviceType.SENSOR));
        }

    }

    @Test
    public void testRegisterLocationArea() throws MiddlewareException {
        String wktPolygon1 = "POLYGON((" +
                "0.1 0.1, " +
                "0.1 44.1, " +
                "89.1 44.1, " +
                "89.1 0.1, " +
                "0.1 0.1))";

        String wktPolygon2 = "POLYGON((" +
                "-45.911690 -15.584657, " +
                "-45.968990 -15.474841, " +
                "-45.913601 -15.005381, " +
                "-45.911690 -15.584657))";

        LocationArea staticLocationArea = new LocationArea(
                LOCATION_AREA_ID,
                PLATFORM1_ID,
                "description",
                wktPolygon1
        );
        LocationArea staticLocationArea2 = new LocationArea(
                LOCATION_AREA_ID2,
                PLATFORM1_ID,
                "description23",
                wktPolygon2
        );

        // REGISTER LOCATION AREA
        registry.registerLocationArea(staticLocationArea);
        registry.registerLocationArea(staticLocationArea2);

        // GET LOCATION AREA
        LocationArea staticLocationAreaRetrieved = registry.getLocationArea(PLATFORM1_ID, LOCATION_AREA_ID);
        LocationArea staticLocationAreaRetrieved2 = registry.getLocationArea(PLATFORM1_ID, LOCATION_AREA_ID2);

        assertEquals("Saved and retrieved location should equal", staticLocationArea, staticLocationAreaRetrieved);
        assertEquals("Saved and retrieved location should equal", staticLocationArea2, staticLocationAreaRetrieved2);

        // GET ALL LOCATION AREAS
        List<LocationArea> locationAreas = registry.getLocationAreas(PLATFORM1_ID);

        assertEquals("Retrieved location areas should count 2 location areas", 2, locationAreas.size());

        String wktPolygonUpdate1 = "POLYGON((" +
                "45.1 45.1, " +
                "45.1 44.1, " +
                "45.1 44.1, " +
                "45.1 0.1, " +
                "45.1 45.1))";

        LocationArea staticLocationAreaUpdate = new LocationArea(
                LOCATION_AREA_ID,
                PLATFORM1_ID,
                "description",
                wktPolygonUpdate1
        );

        // UPDATE LOCATION AREA
        registry.updateLocationArea(LOCATION_AREA_ID, staticLocationAreaUpdate);
        LocationArea staticLocationAreaRetrievedUpdated = registry.getLocationArea(PLATFORM1_ID, LOCATION_AREA_ID);
        assertEquals("Updated and retrieved location should equal", staticLocationAreaUpdate, staticLocationAreaRetrievedUpdated);

        // DELETE LOCATION AREA
        registry.deleteLocationArea(LOCATION_AREA_ID2);
        LocationArea deletedLocationArea = registry.getLocationArea(PLATFORM1_ID, LOCATION_AREA_ID2);
        assertNull("DeletedLocationArea should be null", deletedLocationArea);

    }

    @Test
    public void testRegisterLocation() throws MiddlewareException {

        String wktPoint1 = "POINT(45.911690 15.584657)";
        String wktPoint2 = "POINT(45.920000 15.333333)";

        LocationPoint locationPoint = new LocationPoint(
                LOCATION_POINT_ID,
                PLATFORM1_ID,
                "description",
                wktPoint1
        );
        LocationPoint locationPoint2 = new LocationPoint(
                LOCATION_POINT_ID2,
                PLATFORM1_ID + "23",
                "description23",
                wktPoint2
        );

        // REGISTER DYNAMIC LOCATIONS
        registry.registerLocationPoint(locationPoint);
        registry.registerLocationPoint(locationPoint2);

        // GET DYNAMIC LOCATIONS
        LocationPoint locationPointRetrieved = registry.getLocationPoint(LOCATION_POINT_ID);
        LocationPoint locationPointRetrieved2 = registry.getLocationPoint(LOCATION_POINT_ID2);

        assertEquals("Saved and retrieved location should equal", locationPoint, locationPointRetrieved);
        assertEquals("Saved and retrieved location should equal", locationPoint2, locationPointRetrieved2);

        String wktPointUpdate = "POINT(0.920000 0.333333)";
        LocationPoint locationPointUpdate = new LocationPoint(
                LOCATION_POINT_ID,
                PLATFORM1_ID,
                "updated description",
                wktPointUpdate
        );

        // UPDATE DYNAMIC LOCATION
        registry.updateLocationPoint(LOCATION_POINT_ID, locationPointUpdate);
        LocationPoint locationPointRetrievedUpdated = registry.getLocationPoint(LOCATION_POINT_ID);

        assertEquals("Updated and retrieved location should equal", locationPointUpdate, locationPointRetrievedUpdated);

        // DELETE DYNAMIC LOCATION
        registry.deleteLocationPoint(LOCATION_POINT_ID2);
        LocationPoint deletedLocation = registry.getLocationPoint(LOCATION_POINT_ID2);
        assertNull("Deleted dynamic location should be null", deletedLocation);

    }

    @Test
    public void testLocationInclusion() throws MiddlewareException {
        String wktPointInside = "POINT(45.0 15.0)";
        String wktPointOutside = "POINT(50.0 25.0)";
        String wktPolygonArea = "POLYGON((42.0 17.0, 47.0 17.0, 47.0 13.0, 42.0 13.0, 42.0 17.0))";

        LocationPoint locationPointInside = new LocationPoint(
                LOCATION_POINT_ID,
                PLATFORM1_ID,
                "point inside area",
                wktPointInside
        );

        LocationPoint locationPointOutside = new LocationPoint(
                LOCATION_POINT_ID2,
                PLATFORM1_ID,
                "point outside area",
                wktPointOutside
        );

        LocationArea staticLocationArea = new LocationArea(
                LOCATION_AREA_ID,
                PLATFORM1_ID,
                "area",
                wktPolygonArea
        );

        registry.registerLocationArea(staticLocationArea);
        registry.registerLocationPoint(locationPointInside);
        registry.registerLocationPoint(locationPointOutside);

        List<String> dynamicLocationsInsideArea = registry.getLocationPointsInsideArea(LOCATION_AREA_ID);

        assertEquals("Retrieved dynamic locations inside area should contain 1 element. Check geospatial index in Parliament triple store.", 1, dynamicLocationsInsideArea.size());
        assertEquals("Retrieved dynamic locations inside area should return correct location id", LOCATION_POINT_ID, dynamicLocationsInsideArea.get(0));
    }

    private void registerClient() throws Exception {
        // register client
        Client client = new Client();
        client.setClientId(CLIENT_ID1);
        client.setCallbackUrl(new URL("http://localhost:9090"));
        client.setReceivingCapacity(10);
        client.setResponseDelivery(Client.ResponseDelivery.CLIENT_PULL);
        client.setResponseFormat(Client.ResponseFormat.JSON_LD);
        registry.registerClient(client);

        Client client1a = registry.getClientById(CLIENT_ID1);
        assertNotNull(client1a);
        assertEquals(client1a.getClientId(), CLIENT_ID1);
        assertEquals(client1a.getCallbackUrl(), client.getCallbackUrl());
        assertEquals(client1a.getReceivingCapacity(), client.getReceivingCapacity());
        assertEquals(client1a.getResponseDelivery(), client.getResponseDelivery());
        assertEquals(client1a.getResponseFormat(), client.getResponseFormat());

        // check client update
        Client clientUpdate = new Client();
        clientUpdate.setClientId(CLIENT_ID1);
        clientUpdate.setCallbackUrl(new URL("http://localhost:9999"));
        clientUpdate.setReceivingCapacity(20);
        clientUpdate.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        clientUpdate.setResponseFormat(Client.ResponseFormat.JSON);
        registry.updateClient(clientUpdate);

        Client client1b = registry.getClientById(CLIENT_ID1);
        assertEquals(client1b.getClientId(), CLIENT_ID1);
        assertEquals(client1b.getCallbackUrl(), clientUpdate.getCallbackUrl());
        assertEquals(client1b.getReceivingCapacity(), clientUpdate.getReceivingCapacity());
        assertEquals(client1b.getResponseDelivery(), clientUpdate.getResponseDelivery());
        assertEquals(client1b.getResponseFormat(), clientUpdate.getResponseFormat());

        registry.updateClient(client);

        // check isClientRegistered
        boolean clientExists = registry.isClientRegistered(CLIENT_ID1);
        assertTrue(clientExists);
    }

    private void registerPlatform() throws Exception {
        // register platform 1
        Platform platform1 = new Platform();
        platform1.setPlatformId(PLATFORM1_ID);
        platform1.setName("My Platform 1");
        platform1.setType("ExamplePlatform");
        platform1.setBaseEndpoint(new URL("http://localhost:4568"));
        platform1.setLocationId(LOCATION1_ID);
        platform1.setClientId(CLIENT_ID1);
        platform1.setUsername("username");
        platform1.setEncryptedPassword("password");
        platform1.setEncryptionAlgorithm("SHA-256");

        platform1.setDownstreamInputAlignmentName("Alignment1");
        platform1.setDownstreamInputAlignmentVersion("1.0");
        platform1.setDownstreamOutputAlignmentName("Alignment2");
        platform1.setDownstreamOutputAlignmentVersion("1.0");
        platform1.setUpstreamInputAlignmentName("Alignment3");
        platform1.setUpstreamInputAlignmentVersion("1.0");
        platform1.setUpstreamOutputAlignmentName("Alignment4");
        platform1.setUpstreamOutputAlignmentVersion("1.0");

        registry.registerPlatform(platform1);

        Platform platform1a = registry.getPlatformById(PLATFORM1_ID);
        assertEquals(platform1a.getPlatformId(), platform1a.getPlatformId());
        assertEquals(platform1a.getName(), platform1a.getName());
        assertEquals(platform1a.getType(), platform1a.getType());

        Platform platform2 = new Platform();
        platform2.setPlatformId(PLATFORM2_ID);
        platform2.setName("My Platform 2");
        platform2.setType("ExamplePlatform");
        platform2.setBaseEndpoint(new URL("http://localhost:4569"));
        platform2.setClientId(CLIENT_ID1);
        platform2.setDownstreamInputAlignmentName("Alignment1");
        platform2.setDownstreamInputAlignmentVersion("1.0");
        platform2.setDownstreamOutputAlignmentName("Alignment2");
        platform2.setDownstreamOutputAlignmentVersion("1.0");
        platform2.setUpstreamInputAlignmentName("Alignment3");
        platform2.setUpstreamInputAlignmentVersion("1.0");
        platform2.setUpstreamOutputAlignmentName("Alignment4");
        platform2.setUpstreamOutputAlignmentVersion("1.0");

        registry.registerPlatform(platform2);

        Platform platform2a = registry.getPlatformById(PLATFORM2_ID);
        assertEquals(platform2a.getPlatformId(), PLATFORM2_ID);

        List<Platform> platforms = registry.listPlatforms();
        assertEquals(platforms.size(), 2);
        Platform platform1b = platforms.stream()
                .filter(platform -> platform.getPlatformId().equals(PLATFORM1_ID))
                .findFirst()
                .orElseThrow(Exception::new);
        assertEquals(platform1b.getName(), platform1.getName());
        assertEquals(platform1b.getType(), platform1.getType());
        assertEquals(platform1b.getBaseEndpoint(), platform1.getBaseEndpoint());
        assertEquals(platform1b.getLocationId(), platform1.getLocationId());
    }

    private void registerDevices() throws Exception {
        // register devices on platform 1
        List<IoTDevice> devices1 = generateIoTDevices(
                DEVICE_ID_PREFIX,
                1,
                10,
                PLATFORM1_ID,
                LOCATION1_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE));
        registry.registerDevices(devices1);

        // register devices on platform 2
        List<IoTDevice> devices2 = generateIoTDevices(
                DEVICE_ID_PREFIX,
                20,
                29,
                PLATFORM2_ID,
                LOCATION1_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE));
        registry.registerDevices(devices2);

        // get all devices
        List<String> deviceIdsAllPlatforms = registry.getDeviceIds(PLATFORM1_ID);
        deviceIdsAllPlatforms.addAll(registry.getDeviceIds(PLATFORM2_ID));

        List<String> deviceIdsNoPlatform = registry.getDeviceIds(null);

        List<IoTDevice> allDevices = registry.getDevices(deviceIdsAllPlatforms);
        assertEquals(devices1.size() + devices2.size(), allDevices.size());

        List<IoTDevice> noPlatformDevices = registry.getDevices(deviceIdsNoPlatform);
        assertEquals(devices1.size() + devices2.size(), noPlatformDevices.size());

        // get devices for platform 1
        IoTDevice deviceFilter = new IoTDevice();
        deviceFilter.setHostedBy(PLATFORM1_ID);
        List<IoTDevice> devices1a = registry.getDevices(registry.getDeviceIds(PLATFORM1_ID));
        assertEquals(devices1a.size(), devices1.size());
        IoTDevice ioTDevice = devices1a.stream()
                .filter(device -> device.getDeviceId().equals(DEVICE_ID_PREFIX + "1"))
                .findFirst()
                .orElseThrow(Exception::new);
        assertEquals(ioTDevice.getHostedBy(), PLATFORM1_ID);
        assertEquals(ioTDevice.getLocation(), LOCATION1_ID);
        assertEquals(ioTDevice.getName(), DEVICE_NAME_PREFIX + 1);

        // get selected devices
        List<String> deviceIds = new ArrayList<>();
        deviceIds.add(DEVICE_ID_PREFIX + 1);
        deviceIds.add(DEVICE_ID_PREFIX + 2);
        deviceIds.add(DEVICE_ID_PREFIX + 20);
        List<IoTDevice> selectedDevices = registry.getDevices(deviceIds);
        assertEquals(selectedDevices.size(), deviceIds.size());
        List<IoTDevice> selectedDevicesSorted = selectedDevices.stream().sorted((o1, o2) -> o1.getDeviceId().compareTo(o2.getDeviceId()))
                .collect(Collectors.toList());
        assertEquals(selectedDevicesSorted.get(0).getDeviceId(), DEVICE_ID_PREFIX + 1);
        assertEquals(selectedDevicesSorted.get(1).getDeviceId(), DEVICE_ID_PREFIX + 2);
        assertEquals(selectedDevicesSorted.get(2).getDeviceId(), DEVICE_ID_PREFIX + 20);
    }

    private void updateDevices() throws Exception {
        List<IoTDevice> devicesNew = generateIoTDevices(
                DEVICE_ID_PREFIX,
                1,
                5,
                PLATFORM1_ID,
                LOCATION2_ID,
                DEVICE_NAME_PREFIX,
                EnumSet.of(IoTDeviceType.DEVICE));
        registry.updateDevices(devicesNew);

        List<String> deviceIds = new ArrayList<>();
        deviceIds.add(DEVICE_ID_PREFIX + 1);
        List<IoTDevice> selectedDevices = registry.getDevices(deviceIds);
        assertEquals(selectedDevices.size(), deviceIds.size());
        assertEquals(selectedDevices.get(0).getDeviceId(), DEVICE_ID_PREFIX + 1);
        assertEquals(selectedDevices.get(0).getName(), devicesNew.get(0).getName());
        assertEquals(selectedDevices.get(0).getLocation(), devicesNew.get(0).getLocation());

    }

    private List<IoTDevice> generateIoTDevices(
            String deviceIdPrefix,
            int deviceIndexFrom,
            int deviceIndexTo,
            String platformId,
            String locationId,
            String deviceNamePrefix,
            EnumSet<IoTDeviceType> deviceTypes) {
        List<IoTDevice> devices2 = new ArrayList<>();
        for (int i = deviceIndexFrom; i <= deviceIndexTo; i++) {
            IoTDevice ioTDevice = new IoTDevice(deviceIdPrefix + i);
            ioTDevice.setName(deviceNamePrefix + i);
            ioTDevice.setHostedBy(platformId);
            ioTDevice.setLocation(locationId);
            ioTDevice.setDeviceTypes(deviceTypes);
            devices2.add(ioTDevice);
        }
        return devices2;
    }

    private void subscribe() throws Exception {
        final String conversationId1 = "conversationId1";
        final String conversationId2 = "conversationId2";

        // subscription 1
        Subscription subscription1 = new Subscription();
        subscription1.setClientId(CLIENT_ID1);
        subscription1.setConversationId(conversationId1);
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "1");
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "2");
        subscription1.addDeviceId(DEVICE_ID_PREFIX + "3");
        registry.subscribe(subscription1);

        // subscription 2
        Subscription subscription2 = new Subscription();
        subscription2.setClientId(CLIENT_ID1);
        subscription2.setConversationId(conversationId2);
        subscription2.addDeviceId(DEVICE_ID_PREFIX + "20");
        subscription2.addDeviceId(DEVICE_ID_PREFIX + "21");
        registry.subscribe(subscription2);

        Subscription subscription1a = registry.getSubscriptionById(conversationId1);
        assertEquals(subscription1a.getConversationId(), conversationId1);
        assertEquals(subscription1a.getClientId(), CLIENT_ID1);
        assertEquals(subscription1a.getDeviceIds().size(), subscription1.getDeviceIds().size());
        assertTrue(subscription1a.getDeviceIds().contains(DEVICE_ID_PREFIX + "1"));
        assertTrue(subscription1a.getDeviceIds().contains(DEVICE_ID_PREFIX + "2"));
        assertTrue(subscription1a.getDeviceIds().contains(DEVICE_ID_PREFIX + "3"));

        Subscription subscription2a = registry.getSubscriptionById(conversationId2);
        assertEquals(subscription2a.getConversationId(), conversationId2);
        assertEquals(subscription2a.getClientId(), CLIENT_ID1);
        assertEquals(subscription2a.getDeviceIds().size(), subscription2.getDeviceIds().size());
        assertTrue(subscription2a.getDeviceIds().contains(DEVICE_ID_PREFIX + "20"));
        assertTrue(subscription2a.getDeviceIds().contains(DEVICE_ID_PREFIX + "21"));

        // delete subscription 2
        registry.deleteSubscription(conversationId2);
        Subscription deleted = registry.getSubscriptionById(conversationId2);
        assertNull(deleted);
    }


    private void removeDevices() throws Exception {
        // Remove devices with specific device ID
        List<String> deviceIds = new ArrayList<>();
        deviceIds.add(DEVICE_ID_PREFIX + "1");
        deviceIds.add(DEVICE_ID_PREFIX + "29");
        registry.removeDevices(deviceIds);

        List<String> deletedDeviceIds = new ArrayList<>();
        deletedDeviceIds.add(DEVICE_ID_PREFIX + 1);
        deletedDeviceIds.add(DEVICE_ID_PREFIX + 29);

        List<IoTDevice> deletedDevices = registry.getDevices(deletedDeviceIds);
        assertTrue(deletedDevices.isEmpty());
    }

    private void removePlatform() throws Exception {
        registry.removePlatform(PLATFORM1_ID);
        Platform deletedPlatform = registry.getPlatformById(PLATFORM1_ID);
        assertNull(deletedPlatform);
    }

    private void removeClient() throws Exception {
        registry.removeClient(CLIENT_ID1);
        boolean clientExists = registry.isClientRegistered(CLIENT_ID1);
        assertFalse(clientExists);
    }

    @Test
    public void testPlat2PlatSubscriptions() throws MiddlewareException {
        Plat2PlatSubscription s1 = new Plat2PlatSubscription();
        s1.setClientId("myclient");
        s1.setConversationId("conversationId1");
        s1.setSourceDeviceId("http://inter-iot.eu/p-device1");
        s1.setSourcePlatformId("http://inter-iot.eu/p-platform");
        s1.setTargetDeviceId("http://inter-iot.eu/v-device1");
        s1.setTargetPlatformId("http://inter-iot.eu/v-platform");
        registry.addPlat2PlatSubscription(s1);

        Plat2PlatSubscription s2 = new Plat2PlatSubscription();
        s2.setClientId("myclient");
        s2.setConversationId("conversationId2");
        s2.setSourceDeviceId("http://inter-iot.eu/p-device2");
        s2.setSourcePlatformId("http://inter-iot.eu/p-platform");
        s2.setTargetDeviceId("http://inter-iot.eu/v-device2");
        s2.setTargetPlatformId("http://inter-iot.eu/v-platform");
        registry.addPlat2PlatSubscription(s2);

        Plat2PlatSubscription s1a = registry.getPlat2PlatSubscription(s1.getConversationId());
        assertEquals(s1a.getClientId(), s1.getClientId());
        assertEquals(s1a.getConversationId(), s1.getConversationId());
        assertEquals(s1a.getSourceDeviceId(), s1.getSourceDeviceId());
        assertEquals(s1a.getSourcePlatformId(), s1.getSourcePlatformId());
        assertEquals(s1a.getTargetDeviceId(), s1.getTargetDeviceId());
        assertEquals(s1a.getTargetPlatformId(), s1.getTargetPlatformId());

        Plat2PlatSubscription s1b = registry.findPlat2PlatSubscription(s1.getSourceDeviceId(), s1.getTargetDeviceId());
        assertEquals(s1b.getConversationId(), s1.getConversationId());

        List<Plat2PlatSubscription> subscriptions = registry.listPlat2PlatSubscriptions();
        assertEquals(2, subscriptions.size());
        Map<String, Plat2PlatSubscription> vsMap = subscriptions.stream().collect(Collectors.toMap(Plat2PlatSubscription::getConversationId, x -> x));
        assertTrue(vsMap.containsKey(s1.getConversationId()));
        assertTrue(vsMap.containsKey(s2.getConversationId()));

        registry.deletePlat2PlatSubscription(s1.getConversationId());
        subscriptions = registry.listPlat2PlatSubscriptions();
        assertEquals(1, subscriptions.size());
        assertEquals(s2.getConversationId(), subscriptions.get(0).getConversationId());
    }
}