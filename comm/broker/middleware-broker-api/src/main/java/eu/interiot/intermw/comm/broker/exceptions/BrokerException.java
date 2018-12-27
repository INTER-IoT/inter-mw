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
package eu.interiot.intermw.comm.broker.exceptions;

import eu.interiot.intermw.comm.commons.exceptions.CommException;

/**
 * Base Broker Exception class
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public class BrokerException extends CommException {

    private static final long serialVersionUID = -1779923843342749755L;

    public BrokerException(Integer code, String cause, Throwable e) {
        super(code, cause, e);
    }

    public BrokerException(Integer code, String cause) {
        super(code, cause);
    }

    public BrokerException(Integer code, Throwable e) {
        super(code, e);
    }

    public BrokerException(String cause, Throwable e) {
        super(cause, e);
    }

    public BrokerException(String cause) {
        super(cause);
    }

    public BrokerException(Throwable exception) {
        super(exception);
    }
}
