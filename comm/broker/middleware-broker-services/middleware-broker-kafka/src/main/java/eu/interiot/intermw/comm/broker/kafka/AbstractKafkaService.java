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
package eu.interiot.intermw.comm.broker.kafka;

import eu.interiot.intermw.comm.broker.Service;
import eu.interiot.intermw.comm.broker.abstracts.AbstractService;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

import java.util.Properties;

/**
 * {@link AbstractService} implementation for Kafka Services
 *
 * @param <M> The message class this {@link Service} works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public abstract class AbstractKafkaService<M> extends AbstractService<M> {

    public final static String KAFKA_PROPERTIY_PREFIX = "kafka.";
    public final static String TOPIC_PROPERTIY_PREFIX = "topic-" + KAFKA_PROPERTIY_PREFIX;
    public final static String CUSTOM_PROPERTIY_PREFIX = "custom-" + KAFKA_PROPERTIY_PREFIX;

    private final static String ZOOKEEPER_HOSTS_LIST_PROPERTY = "zookeeper.connect";
    private final static String REPLICAS_PROPERTY = "replicas";
    private final static String PARTITIONS_PROPERTY = "partitions";
    private final static String CONNECTION_TIMEOUT_PROPERTY = "connection.timeout";

    private final static int DEFAULT_NUMBER_OF_PARTITIONS = 1;
    private final static int DEFAULT_NUMBER_OF_REPLICATION = 1;
    private final static int DEFAULT_TIMEOUT = 10000;

    private boolean isTopicCreated = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTopic(String name) throws BrokerException {
        ZkClient zkClient = null;
        ZkUtils zkUtils = null;

        try {
            if (!isTopicCreated) {
                String zookeeperHosts = getZookeeperHostList();

                zkClient = new ZkClient(zookeeperHosts, getTimeout(), getTimeout(), ZKStringSerializer$.MODULE$);
                zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);

                boolean topicExists = AdminUtils.topicExists(zkUtils, name);
                if (!topicExists) {
                    AdminUtils.createTopic(zkUtils, name, getPartitions(), getReplicas(), getTopicProperties());
                }

                isTopicCreated = true;
            }
        } catch (Exception ex) {
            throw new BrokerException(ex);
        } finally {
            if (zkClient != null) {
                try {
                    zkClient.close();
                } catch (Exception e) {
                    throw new BrokerException(e);
                }
            }
        }
    }

    @Override
    public void deleteTopic(String name) throws BrokerException {
        ZkClient zkClient = null;

        try {
            String zookeeperHosts = getZookeeperHostList();

            zkClient = new ZkClient(zookeeperHosts, getTimeout(), getTimeout(), ZKStringSerializer$.MODULE$);
            zkClient.deleteRecursive(ZkUtils.getTopicPath(name));
        } catch (Exception e) {
            throw new BrokerException(e);
        } finally {
            if (zkClient != null) {
                try {
                    zkClient.close();
                } catch (Exception e) {
                    throw new BrokerException(e);
                }
            }
        }
    }

    private String getZookeeperHostList() {
        return this.getKafkaProperties().getProperty(ZOOKEEPER_HOSTS_LIST_PROPERTY);
    }

    protected Properties getKafkaProperties() {
        return this.broker.getConfiguration().getPropertiesWithPrefix(KAFKA_PROPERTIY_PREFIX, true);
    }

    protected Properties getTopicProperties() {
        return this.broker.getConfiguration().getPropertiesWithPrefix(TOPIC_PROPERTIY_PREFIX);
    }

    protected Properties getCustomProperties() {
        return this.broker.getConfiguration().getPropertiesWithPrefix(CUSTOM_PROPERTIY_PREFIX);
    }

    protected int getReplicas() {
        try {
            String replicas = this.getCustomProperties().getProperty(REPLICAS_PROPERTY);
            return Integer.valueOf(replicas);
        } catch (Exception e) {
            return DEFAULT_NUMBER_OF_REPLICATION;
        }
    }

    protected int getPartitions() {
        try {
            String partitions = this.getCustomProperties().getProperty(PARTITIONS_PROPERTY);
            return Integer.valueOf(partitions);
        } catch (Exception e) {
            return DEFAULT_NUMBER_OF_PARTITIONS;
        }
    }

    protected int getTimeout() {
        try {
            String timeout = this.getCustomProperties().getProperty(CONNECTION_TIMEOUT_PROPERTY);
            return Integer.valueOf(timeout);
        } catch (Exception e) {
            return DEFAULT_TIMEOUT;
        }
    }
}
