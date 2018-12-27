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
package eu.interiot.intermw.commons.exceptions;

/**
 * {@link MiddlewareException} subclass for Bridge API exceptions
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 * @author <a href="mailto:mllorente@prodevelop.es">Miguel A. Llorente</a>
 * @see ErrorCode
 */
public class MiddlewareException extends Exception {

    private static final long serialVersionUID = 2134144519277702319L;

    private String cause;
    private Integer code;
    private Throwable exception;

    /**
     * The constructor
     *
     * @param exception The origin {@link Exception}
     */
    public MiddlewareException(Throwable exception) {
        super(exception);
        this.exception = exception;
    }

    /**
     * The constructor
     *
     * @param cause A string describing the cause of the {@link Exception}
     */
    public MiddlewareException(String cause) {
        super(cause);
        this.cause = cause;
    }

    /**
     * The constructor
     *
     * @param format Format of the cause
     * @param args   Arguments for format
     */
    public MiddlewareException(String format, Object... args) {
        this(String.format(format, args));
    }

    /**
     * The constructor
     *
     * @param code A unique ID to identify the {@link Exception}
     * @param e    The origin {@link Exception}
     */
    public MiddlewareException(Integer code, Throwable e) {
        super(e);
        this.code = code;
    }

    /**
     * The constructor
     *
     * @param cause A string describing the cause of the {@link Exception}
     * @param e     The origin {@link Exception}
     */
    public MiddlewareException(String cause, Throwable e) {
        super(cause, e);
        this.cause = cause;
        exception = e;
    }

    /**
     * The constructor
     *
     * @param e      The origin {@link Exception}
     * @param format Format of the cause
     * @param args   Arguments for format
     */
    public MiddlewareException(Throwable e, String format, Object... args) {
        super(String.format(format, args), e);
        this.cause = String.format(format, args);
        exception = e;
    }

    /**
     * The constructor
     *
     * @param code  A unique ID to identify the {@link Exception}
     * @param cause The origin {@link Exception}
     */
    public MiddlewareException(Integer code, String cause) {
        super(cause);
        this.cause = cause;
        this.code = code;
    }

    /**
     * The constructor
     *
     * @param code  A unique ID to identify the {@link Exception}
     * @param cause A string describing the cause of the {@link Exception}
     * @param e     The origin {@link Exception}
     */
    public MiddlewareException(Integer code, String cause, Throwable e) {
        super(cause, e);
        this.cause = cause;
        this.code = code;
        exception = e;
    }
}