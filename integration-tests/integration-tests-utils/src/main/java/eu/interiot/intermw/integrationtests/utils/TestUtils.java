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
package eu.interiot.intermw.integrationtests.utils;

import com.google.common.io.Resources;
import eu.interiot.intermw.comm.ipsm.IPSMApiClient;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.apache.commons.io.Charsets;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Properties;

public class TestUtils {
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    public static void clearParliament(Configuration conf) {
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
        logger.debug("Parliament has been cleared.");
    }

    public static void clearRabbitMQ(Configuration conf) {
        CachingConnectionFactory cf = new CachingConnectionFactory(conf.getRabbitmqHostname(), conf.getRabbitmqPort());
        cf.setUsername(conf.getRabbitmqUsername());
        cf.setPassword(conf.getRabbitmqPassword());
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
        String[] queuesToDelete = {"arm_prm_queue", "prm_arm_queue",
                "prm_ipsmrm_queue", "ipsmrm_prm_queue",
                "error_queue",
                "ipsmrm_bridge_http_test.inter-iot.eu_test-platform1_queue",
                "bridge_ipsmrm_http_test.inter-iot.eu_test-platform1_queue",
                "client-myclient"};
        for (String queueToDelete : queuesToDelete) {
            rabbitAdmin.deleteQueue(queueToDelete);
        }
        logger.debug("RabbitMQ has been cleared.");
    }

    public static void checkIfRabbitQueuesEmpty(Configuration conf) throws Exception {
        CachingConnectionFactory cf = new CachingConnectionFactory(conf.getRabbitmqHostname(), conf.getRabbitmqPort());
        cf.setUsername(conf.getRabbitmqUsername());
        cf.setPassword(conf.getRabbitmqPassword());
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
        String[] queueNames = {"arm_prm_queue", "prm_arm_queue",
                "prm_ipsmrm_queue", "ipsmrm_prm_queue",
                "error_queue",
                "ipsmrm_bridge_http_test.inter-iot.eu_test-platform1_queue",
                "bridge_ipsmrm_http_test.inter-iot.eu_test-platform1_queue",
                "client-myclient"};
        for (String queueName : queueNames) {
            Properties queueProperties = rabbitAdmin.getQueueProperties(queueName);
            if (queueProperties == null) {
                continue;
            }
            String queueMessageCountString = queueProperties.getProperty("QUEUE_MESSAGE_COUNT");
            int queueMessageCount = Integer.parseInt(queueMessageCountString != null ? queueMessageCountString : "0");
            if (queueMessageCount > 0) {
                throw new Exception(String.format("RabbitMQ queue %s is not empty.", queueName));
            }
        }
        logger.debug("All RabbitMQ queues are empty.");
    }

    public static void uploadAlignments(Configuration conf) throws Exception {
        logger.debug("Uploading test alignments to IPSM...");

        IPSMApiClient ipsmApiClient = new IPSMApiClient(conf.getIPSMApiBaseUrl());

        String downstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-downstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        // upload alignment with version set to 1.0.1
        downstreamAlignmentData = downstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        String upstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-upstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        // upload alignment with version set to 1.0.1
        upstreamAlignmentData = upstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        logger.debug("Alignments have been uploaded successfully.");
    }
}
