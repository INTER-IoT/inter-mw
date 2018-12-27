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
package eu.interiot.intermw.commons.model;

import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gasper Vrhovsek
 */
public class IoTDeviceFilterTest {

    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String LOCATION_ID = "http://test.inter-iot.eu/TestLocation1";
    private static final String LOCAL_ID = "http://test.inter-iot.eu/local-id1";

    private static final String ACTUATOR_SPARQL_CONDITION = "rdf:type <http://www.w3.org/ns/sosa/Actuator>;";
    private static final String HOSTED_BY_SPARQL_CONDITION = "sosa:isHostedBy <http://test.inter-iot.eu/test-platform1>;";
    private static final String HAS_LOCATION_SPARQL_CONDITION = "iiot:hasLocation <http://test.inter-iot.eu/TestLocation1>;";

    @Test
    public void test() {
        IoTDeviceFilter query = new IoTDeviceFilter();

        query.addDeviceType(IoTDeviceType.ACTUATOR);
        query.setHostedBy(PLATFORM_ID);
        query.setLocation(LOCATION_ID);
        query.setLocalId(LOCAL_ID);

        String s = query.buildConditions();

        assertEquals("Query should contain 4 lines of conditions", 4, s.split("\\n").length);

        assertTrue("Query should contain device type condition for ACTUATOR", s.contains(ACTUATOR_SPARQL_CONDITION));
        assertTrue("Query should contain hostedBy condition", s.contains(HOSTED_BY_SPARQL_CONDITION));
        assertTrue("Query should contain hasLocation condition", s.contains(HAS_LOCATION_SPARQL_CONDITION));
        assertTrue("Query should contain localId condition", s.contains(LOCAL_ID));
    }
}
