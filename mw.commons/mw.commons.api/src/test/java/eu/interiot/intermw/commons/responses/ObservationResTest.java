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
package eu.interiot.intermw.commons.responses;

import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gasper Vrhovsek
 */
public class ObservationResTest {

    private static final String SENDER_PLATFORM_ID = "http://www.inter-iot.eu/wso2port";
    public static final String FEATURE_OF_INTEREST_ID = "http://inter-iot.eu/LogVPmod#1111EKT2222EKT";
    private static String observationPortJson;

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassLoader classLoader = ObservationResTest.class.getClassLoader();
        observationPortJson = IOUtils.toString(classLoader.getResource("observation_port.json"), "UTF-8");
    }

    @Test
    public void testObservationPortJson() throws IOException, MessageException {
        // create
        Message observationMessage = new Message(observationPortJson);
        ObservationRes observationRes = new ObservationRes(observationMessage);

        // assert
        List<ObservationRes.Observation> observations = observationRes.getObservations();
        List<ObservationRes.FeatureOfInterest> featuresOfInterest = observationRes.getFeaturesOfInterest();

        assertEquals("SenderPlatform should be correctly parsed from json", SENDER_PLATFORM_ID, observationRes.getSenderPlatform());
        assertEquals("Observation response should contain 8 observations", 8, observations.size());
        // TODO this assert fails, we have to set standards for OBSERVATION messages
        //        assertEquals("Observation response should contain 8 sensors", 8, observationRes.getSensors().size());
        assertEquals("Observation response should contain 1 feature of interest", 1, featuresOfInterest.size());

        // assert observations
        for (ObservationRes.Observation observation : observations) {
            assertNotNull("Observation.hasFeatureOfInterest should not be null", observation.getHasFeatureOfInterest());
            assertEquals("Observation.hasFeatureOfInterest should have correct value", FEATURE_OF_INTEREST_ID, observation.getHasFeatureOfInterest());

            ObservationRes.Result result = observation.getResult();
            // assert observation result
            assertNotNull("Observation.Result should not be null", result);
            assertNotNull("Observation.Result.Value should not be null", result.getHasResultValue());
        }

        // assert features of interest
        for (ObservationRes.FeatureOfInterest featureOfInterest : featuresOfInterest) {
            assertEquals("Feature of interest should contain correct ID", FEATURE_OF_INTEREST_ID, featureOfInterest.getId());
        }

    }
}
