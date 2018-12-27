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
package eu.interiot.intermw.comm.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A default {@link Serializer} instance based on {@link Gson}
 *
 * @param <M> The message class this {@link Serializer} understands
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
/* TODO / for some reason it takes the non default serialser..... so I, Flavio, comment this out for the time being
@eu.interiot.intermw.comm.broker.annotations.Serializer

 */
public class DefaultSerializer<M> implements Serializer<M> {
    private final static Logger log = LoggerFactory.getLogger(JsonLDSerializer.class);

    private final Gson gson;

    /**
     * The constructor
     *
     * Creates a basic {@link Gson} instance
     *
     * FIXME Improve the {@link Gson} instance configuration
     */
    public DefaultSerializer() {
        log.debug("Created DefaultSerializer");
        gson = new GsonBuilder().create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(M message) {
        return gson.toJson(message);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M deserialize(String message, Class<M> type) {
        return gson.fromJson(message, type);
    }

}
