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
package eu.interiot.intermw.comm.commons.exceptions;

import eu.interiot.intermw.commons.exceptions.MiddlewareException;

/**
 * Base Middleware Comm Exception class
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 */
public class CommException extends MiddlewareException {

    private static final long serialVersionUID = -3116732623648886127L;

    /**
     * {@inheritDoc}
     */
    public CommException(Throwable exception) {
        super(exception);
    }

    /**
     * {@inheritDoc}
     */
    public CommException(String cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    public CommException(Integer code, Throwable e) {
        super(code, e);
    }

    /**
     * {@inheritDoc}
     */
    public CommException(String cause, Throwable e) {
        super(cause, e);
    }

    /**
     * {@inheritDoc}
     */
    public CommException(Integer code, String cause) {
        super(code, cause);
    }

    /**
     * {@inheritDoc}
     */
    public CommException(Integer code, String cause, Throwable e) {
        super(code, cause, e);
    }
}
