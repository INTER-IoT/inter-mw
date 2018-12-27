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
package eu.interiot.intermw.comm.ipsm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public class IPSMApiClient {
    private final static Logger logger = LoggerFactory.getLogger(IPSMApiClient.class);
    private WebTarget webTarget;
    private ObjectMapper objectMapper;

    public IPSMApiClient(String ipsmApiBaseUrl) {
        Client client = ClientBuilder.newClient();
        webTarget = client.target(ipsmApiBaseUrl);
        objectMapper = new ObjectMapper();
    }

    public void setupChannelsForPlatform(Platform platform) throws MiddlewareException {

        logger.debug("setupChannelsForPlatform started.");
        String topicToIPSMDownstream = DefaultIPSMRequestManager.getTopicToIPSMDownstream(platform.getPlatformId());
        String topicFromIPSMDownstream = DefaultIPSMRequestManager.getTopicFromIPSMDownstream(platform.getPlatformId());
        String topicToIPSMUpstream = DefaultIPSMRequestManager.getTopicToIPSMUpstream(platform.getPlatformId());
        String topicFromIPSMUpstream = DefaultIPSMRequestManager.getTopicFromIPSMUpstream(platform.getPlatformId());

        logger.debug("Retrieving list of channels from IPSM...");
        GenericType<List<ChannelInfo>> channelListGenericType = new GenericType<List<ChannelInfo>>() {
        };
        List<ChannelInfo> channelInfos = webTarget.path("/channels")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(channelListGenericType);

        ChannelInfo downstreamChannel = null;
        ChannelInfo upstreamChannel = null;
        for (ChannelInfo channelInfo : channelInfos) {
            if (topicToIPSMDownstream.equals(channelInfo.getSource()) && topicFromIPSMDownstream.equals(channelInfo.getSink())) {
                downstreamChannel = channelInfo;

            } else if (topicToIPSMUpstream.equals(channelInfo.getSource()) && topicFromIPSMUpstream.equals(channelInfo.getSink())) {
                upstreamChannel = channelInfo;
            }
        }

        logger.debug("Downstream channel: {}", downstreamChannel != null ? downstreamChannel.getDescId() : "N/A");
        logger.debug("Upstream channel: {}", upstreamChannel != null ? upstreamChannel.getDescId() : "N/A");

        if (platform.getDownstreamInputAlignmentName().isEmpty() && platform.getDownstreamOutputAlignmentName().isEmpty()) {
            // alignments are not given, downstream channel is not required
            logger.debug("Downstream alignments are not given, downstream IPSM channel is not needed.");
            if (downstreamChannel != null) {
                logger.debug("Downstream IPSM channel exists but is not needed. It will be removed.");
                removeChannel(downstreamChannel);
            }

        } else {
            // alignments are given, downstream channel is required
            if (downstreamChannel == null) {
                logger.debug("Downstream IPSM channel doesn't exists, it has to be created.");
                createDownstreamChannel(platform, topicToIPSMDownstream, topicFromIPSMDownstream);

            } else if (platform.getDownstreamInputAlignmentName().equals(downstreamChannel.getInpAlignmentName()) &&
                    platform.getDownstreamInputAlignmentVersion().equals(downstreamChannel.getInpAlignmentVersion()) &&
                    platform.getDownstreamOutputAlignmentName().equals(downstreamChannel.getOutAlignmentName()) &&
                    platform.getDownstreamOutputAlignmentVersion().equals(downstreamChannel.getOutAlignmentVersion())) {
                logger.debug("Downstream channel already exists, nothing has to be done.");

            } else {
                logger.debug("Downstream channel exists but doesn't use correct alignments. It will be removed and new one created.");
                removeChannel(downstreamChannel);
                createDownstreamChannel(platform, topicToIPSMDownstream, topicFromIPSMDownstream);
            }
        }

        if (platform.getUpstreamInputAlignmentName().isEmpty() && platform.getUpstreamOutputAlignmentName().isEmpty()) {
            // alignments are not given, upstream channel is not required
            logger.debug("Upstream alignments are not given, upstream IPSM channel is not needed.");
            if (upstreamChannel != null) {
                logger.debug("Upstream IPSM channel exists but is not needed. It will be removed.");
                removeChannel(upstreamChannel);
            }

        } else {
            // alignments are given, upstream channel is required
            if (upstreamChannel == null) {
                logger.debug("Upstream IPSM channel doesn't exists, it has to be created.");
                createUpstreamChannel(platform, topicToIPSMUpstream, topicFromIPSMUpstream);

            } else if (platform.getUpstreamInputAlignmentName().equals(upstreamChannel.getInpAlignmentName()) &&
                    platform.getUpstreamInputAlignmentVersion().equals(upstreamChannel.getInpAlignmentVersion()) &&
                    platform.getUpstreamOutputAlignmentName().equals(upstreamChannel.getOutAlignmentName()) &&
                    platform.getUpstreamOutputAlignmentVersion().equals(upstreamChannel.getOutAlignmentVersion())) {
                logger.debug("Upstream channel already exists, nothing has to be done.");

            } else {
                logger.debug("Upstream channel exists but doesn't use correct alignments. It will be removed and new one created.");
                removeChannel(upstreamChannel);
                createUpstreamChannel(platform, topicToIPSMUpstream, topicFromIPSMUpstream);
            }
        }

        logger.debug("IPSM channels have been set up successfully.");
    }

    public void removeChannelsForPlatform(String platformId) throws MiddlewareException {
        logger.debug("Removing channels for platform {}...", platformId);
        String topicToIPSMDownstream = DefaultIPSMRequestManager.getTopicToIPSMDownstream(platformId);
        String topicFromIPSMDownstream = DefaultIPSMRequestManager.getTopicFromIPSMDownstream(platformId);
        String topicToIPSMUpstream = DefaultIPSMRequestManager.getTopicToIPSMUpstream(platformId);
        String topicFromIPSMUpstream = DefaultIPSMRequestManager.getTopicFromIPSMUpstream(platformId);

        logger.debug("Retrieving list of channels from IPSM...");
        GenericType<List<ChannelInfo>> channelListGenericType = new GenericType<List<ChannelInfo>>() {
        };
        List<ChannelInfo> channelInfos = webTarget.path("/channels")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(channelListGenericType);

        ChannelInfo downstreamChannel = null;
        ChannelInfo upstreamChannel = null;
        for (ChannelInfo channelInfo : channelInfos) {
            if (topicToIPSMDownstream.equals(channelInfo.getSource()) && topicFromIPSMDownstream.equals(channelInfo.getSink())) {
                downstreamChannel = channelInfo;

            } else if (topicToIPSMUpstream.equals(channelInfo.getSource()) && topicFromIPSMUpstream.equals(channelInfo.getSink())) {
                upstreamChannel = channelInfo;
            }
        }

        logger.debug("Downstream channel: {}", downstreamChannel != null ? downstreamChannel.getDescId() : "N/A");
        logger.debug("Upstream channel: {}", upstreamChannel != null ? upstreamChannel.getDescId() : "N/A");

        if (downstreamChannel != null) {
            removeChannel(downstreamChannel);
        }
        if (upstreamChannel != null) {
            removeChannel(upstreamChannel);
        }
    }

    private void createDownstreamChannel(Platform platform,
                                         String topicToIPSMDownstream, String topicFromIPSMDownstream) throws MiddlewareException {
        ChannelInput channelInput = new ChannelInput();
        channelInput.setSource(topicToIPSMDownstream);
        channelInput.setSink(topicFromIPSMDownstream);
        channelInput.setInpAlignmentName(platform.getDownstreamInputAlignmentName());
        channelInput.setInpAlignmentVersion(platform.getDownstreamInputAlignmentVersion());
        channelInput.setOutAlignmentName(platform.getDownstreamOutputAlignmentName());
        channelInput.setOutAlignmentVersion(platform.getDownstreamOutputAlignmentVersion());
        channelInput.setParallelism(1);

        if (logger.isDebugEnabled()) {
            logger.debug("Creating downstream IPSM channel for platform {}:\n{}",
                    platform.getPlatformId(), dump(channelInput));
        }
        createChannel(channelInput);
        logger.info("Downstream IPSM channel for platform {} has been created successfully.", platform.getPlatformId());
    }

    private void createUpstreamChannel(Platform platform,
                                       String topicToIPSMUpstream, String topicFromIPSMUpstream) throws MiddlewareException {
        ChannelInput channelInput = new ChannelInput();
        channelInput.setSource(topicToIPSMUpstream);
        channelInput.setSink(topicFromIPSMUpstream);
        channelInput.setInpAlignmentName(platform.getUpstreamInputAlignmentName());
        channelInput.setInpAlignmentVersion(platform.getUpstreamInputAlignmentVersion());
        channelInput.setOutAlignmentName(platform.getUpstreamOutputAlignmentName());
        channelInput.setOutAlignmentVersion(platform.getUpstreamOutputAlignmentVersion());
        channelInput.setParallelism(1);

        if (logger.isDebugEnabled()) {
            logger.debug("Creating upstream IPSM channel for platform {}:\n{}",
                    platform.getPlatformId(), dump(channelInput));
        }
        createChannel(channelInput);
        logger.info("Upstream IPSM channel for platform {} has been created successfully.", platform.getPlatformId());
    }

    private void createChannel(ChannelInput channelInput) throws MiddlewareException {
        Response response = webTarget.path("/channels")
                .request()
                .post(Entity.json(channelInput));
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String content = response.readEntity(String.class);
            throw new MiddlewareException("Failed to create IPSM channel %s. Response from IPSM API: %s: %s.",
                    dump(channelInput), response.getStatusInfo(), content);
        }
    }

    private void removeChannel(ChannelInfo channelInfo) throws MiddlewareException {
        Response response = webTarget.path("/channels/{channelId}")
                .resolveTemplate("channelId", channelInfo.getId())
                .request()
                .delete();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String content = response.readEntity(String.class);
            throw new MiddlewareException("Failed to remove IPSM channel %s. Response from IPSM API: %s: %s.",
                    channelInfo.getId(), response.getStatusInfo(), content);
        }
        logger.debug("IPSM channel with ID {} has been removed.", channelInfo.getId());
    }

    private String dump(ChannelInput channelInput) {
        try {
            return objectMapper.writeValueAsString(channelInput);
        } catch (JsonProcessingException e) {
            return "(Failed to serialize channel to JSON: " + e.getMessage() + ")";
        }
    }

    public void uploadAlignment(String alignmentData) throws MiddlewareException {
        Response response = webTarget.path("/alignments")
                .request()
                .post(Entity.xml(alignmentData));
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String content = response.readEntity(String.class);
            throw new MiddlewareException("Failed to create IPSM alignment. Response from IPSM API: %s: %s.",
                    response.getStatusInfo(), content);
        }
    }

    public static class ChannelInfo {
        private int id;
        private String uuid;
        private String source;
        private String sink;
        private String inpAlignmentName;
        private String inpAlignmentVersion;
        private String outAlignmentName;
        private String outAlignmentVersion;
        private String consumerGroup;
        private String descId;
        private int parallelism;

        public ChannelInfo() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSink() {
            return sink;
        }

        public void setSink(String sink) {
            this.sink = sink;
        }

        public String getInpAlignmentName() {
            return inpAlignmentName;
        }

        public void setInpAlignmentName(String inpAlignmentName) {
            this.inpAlignmentName = inpAlignmentName;
        }

        public String getInpAlignmentVersion() {
            return inpAlignmentVersion;
        }

        public void setInpAlignmentVersion(String inpAlignmentVersion) {
            this.inpAlignmentVersion = inpAlignmentVersion;
        }

        public String getOutAlignmentName() {
            return outAlignmentName;
        }

        public void setOutAlignmentName(String outAlignmentName) {
            this.outAlignmentName = outAlignmentName;
        }

        public String getOutAlignmentVersion() {
            return outAlignmentVersion;
        }

        public void setOutAlignmentVersion(String outAlignmentVersion) {
            this.outAlignmentVersion = outAlignmentVersion;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getDescId() {
            return descId;
        }

        public void setDescId(String descId) {
            this.descId = descId;
        }

        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }
    }

    public static class ChannelInput {
        private String source;
        private String sink;
        private String inpAlignmentName;
        private String inpAlignmentVersion;
        private String outAlignmentName;
        private String outAlignmentVersion;
        private int parallelism;

        public ChannelInput() {
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSink() {
            return sink;
        }

        public void setSink(String sink) {
            this.sink = sink;
        }

        public String getInpAlignmentName() {
            return inpAlignmentName;
        }

        public void setInpAlignmentName(String inpAlignmentName) {
            this.inpAlignmentName = inpAlignmentName;
        }

        public String getInpAlignmentVersion() {
            return inpAlignmentVersion;
        }

        public void setInpAlignmentVersion(String inpAlignmentVersion) {
            this.inpAlignmentVersion = inpAlignmentVersion;
        }

        public String getOutAlignmentName() {
            return outAlignmentName;
        }

        public void setOutAlignmentName(String outAlignmentName) {
            this.outAlignmentName = outAlignmentName;
        }

        public String getOutAlignmentVersion() {
            return outAlignmentVersion;
        }

        public void setOutAlignmentVersion(String outAlignmentVersion) {
            this.outAlignmentVersion = outAlignmentVersion;
        }

        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }
    }
}
