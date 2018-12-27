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
package eu.interiot.intermw.integrationtest;

import com.google.common.io.Resources;
import eu.interiot.intermw.comm.ipsm.DefaultIPSMRequestManager;
import eu.interiot.intermw.comm.ipsm.IPSMApiClient;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Platform;
import org.apache.commons.io.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IPSMApiClientIntegrationTest {
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private DefaultConfiguration conf;
    private IPSMApiClient ipsmApiClient;
    private WebTarget ipsmApiTarget;

    @Before
    public void setUp() throws MiddlewareException {
        conf = new DefaultConfiguration(CONFIG_FILE);
        ipsmApiClient = new IPSMApiClient(conf.getIPSMApiBaseUrl());

        Client client = ClientBuilder.newClient();
        ipsmApiTarget = client.target(conf.getIPSMApiBaseUrl());
    }

    @After
    public void tearDown() throws MiddlewareException {
        ipsmApiClient.removeChannelsForPlatform(PLATFORM_ID);
    }

    @Test
    public void testWithAlignments() throws Exception {
        String downstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-downstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        // upload alignment with version set to 1.0.1
        downstreamAlignmentData = downstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        String upstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-upstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        // upload alignment with version to 1.0.1
        upstreamAlignmentData = upstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        // prepare platform
        Platform platform = new Platform();
        platform.setPlatformId("http://test.inter-iot.eu/test-platform1");

        platform.setDownstreamInputAlignmentName("Test_Downstream_Alignment");
        platform.setDownstreamInputAlignmentVersion("1.0");
        platform.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
        platform.setDownstreamOutputAlignmentVersion("1.0");

        platform.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
        platform.setUpstreamInputAlignmentVersion("1.0");
        platform.setUpstreamOutputAlignmentName("Test_Upstream_Alignment");
        platform.setUpstreamOutputAlignmentVersion("1.0");

        // create channels
        ipsmApiClient.setupChannelsForPlatform(platform);
        PlatformChannels channels1 = retrieveChannels();
        assertThat(channels1.downstreamChannel.getInpAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels1.downstreamChannel.getInpAlignmentVersion(), is("1.0"));
        assertThat(channels1.downstreamChannel.getOutAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels1.downstreamChannel.getOutAlignmentVersion(), is("1.0"));

        // update channels (no change)
        ipsmApiClient.setupChannelsForPlatform(platform);

        PlatformChannels channels2 = retrieveChannels();
        assertThat(channels2.downstreamChannel.getId(), is(channels1.downstreamChannel.getId()));
        assertThat(channels2.upstreamChannel.getId(), is(channels1.upstreamChannel.getId()));
        assertThat(channels2.downstreamChannel.getInpAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels2.downstreamChannel.getInpAlignmentVersion(), is("1.0"));
        assertThat(channels2.downstreamChannel.getOutAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels2.downstreamChannel.getOutAlignmentVersion(), is("1.0"));

        // update channels (alignment version is changed)
        platform.setDownstreamInputAlignmentVersion("1.0.1");
        platform.setDownstreamOutputAlignmentVersion("1.0.1");
        platform.setUpstreamInputAlignmentVersion("1.0.1");
        platform.setUpstreamOutputAlignmentVersion("1.0.1");
        ipsmApiClient.setupChannelsForPlatform(platform);

        PlatformChannels channels3 = retrieveChannels();
        assertThat(channels3.downstreamChannel.getInpAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels3.downstreamChannel.getInpAlignmentVersion(), is("1.0.1"));
        assertThat(channels3.downstreamChannel.getOutAlignmentName(), is("Test_Downstream_Alignment"));
        assertThat(channels3.downstreamChannel.getOutAlignmentVersion(), is("1.0.1"));
    }

    @Test
    public void testWithoutAlignments() throws Exception {
        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM_ID);

        platform.setDownstreamInputAlignmentName("");
        platform.setDownstreamInputAlignmentVersion("");
        platform.setDownstreamOutputAlignmentName("");
        platform.setDownstreamOutputAlignmentVersion("");

        platform.setUpstreamInputAlignmentName("");
        platform.setUpstreamInputAlignmentVersion("");
        platform.setUpstreamOutputAlignmentName("");
        platform.setUpstreamOutputAlignmentVersion("");

        // create channels
        ipsmApiClient.setupChannelsForPlatform(platform);

        // update channels (no change)
        ipsmApiClient.setupChannelsForPlatform(platform);
    }

    private PlatformChannels retrieveChannels() {
        String topicToIPSMDownstream = DefaultIPSMRequestManager.getTopicToIPSMDownstream(PLATFORM_ID);
        String topicFromIPSMDownstream = DefaultIPSMRequestManager.getTopicFromIPSMDownstream(PLATFORM_ID);
        String topicToIPSMUpstream = DefaultIPSMRequestManager.getTopicToIPSMUpstream(PLATFORM_ID);
        String topicFromIPSMUpstream = DefaultIPSMRequestManager.getTopicFromIPSMUpstream(PLATFORM_ID);

        GenericType<List<IPSMApiClient.ChannelInfo>> channelListGenericType = new GenericType<List<IPSMApiClient.ChannelInfo>>() {
        };
        List<IPSMApiClient.ChannelInfo> channelInfos = ipsmApiTarget.path("/channels")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(channelListGenericType);

        PlatformChannels channels = new PlatformChannels();
        channels.downstreamChannel = null;
        channels.upstreamChannel = null;
        for (IPSMApiClient.ChannelInfo channelInfo : channelInfos) {
            if (topicToIPSMDownstream.equals(channelInfo.getSource()) && topicFromIPSMDownstream.equals(channelInfo.getSink())) {
                channels.downstreamChannel = channelInfo;

            } else if (topicToIPSMUpstream.equals(channelInfo.getSource()) && topicFromIPSMUpstream.equals(channelInfo.getSink())) {
                channels.upstreamChannel = channelInfo;
            }
        }
        return channels;
    }

    private class PlatformChannels {
        public IPSMApiClient.ChannelInfo downstreamChannel;
        public IPSMApiClient.ChannelInfo upstreamChannel;
    }
}
