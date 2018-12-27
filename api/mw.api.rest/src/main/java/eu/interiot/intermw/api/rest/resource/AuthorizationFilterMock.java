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
package eu.interiot.intermw.api.rest.resource;

import eu.interiot.intermw.commons.Context;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

//@Provider
//@PreMatching
public class AuthorizationFilterMock implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilterMock.class);
    private static final String CLIENT_ID_HEADER = "Client-ID";
    private ParliamentRegistry parliamentRegistry;

    public AuthorizationFilterMock() throws MiddlewareException {
        logger.debug("AuthorizationFilterMock is initializing...");
        parliamentRegistry = new ParliamentRegistry(Context.getConfiguration());
        logger.debug("AuthorizationFilterMock has been initialized successfully.");
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        String path = ((ContainerRequest) context).getRequestUri().getPath();
        String method = context.getMethod();
        if (path.equals("/api/swagger.json") ||
                path.equals("/api/mw2mw/clients") && method.equals("POST")) {
            return;
        }

        if (!context.getHeaders().containsKey(CLIENT_ID_HEADER)) {

            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("The 'Client-ID' header is missing.")
                    .build());
            return;
        }

        String clientId = context.getHeaderString(CLIENT_ID_HEADER);
        boolean clientRegistered;
        try {
            clientRegistered = parliamentRegistry.isClientRegistered(clientId);
        } catch (MiddlewareException e) {
            logger.error("Failed to validate client: " + e.getMessage(), e);
            throw new IOException("Failed to validate client.", e);
        }
        if (!clientRegistered) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Client '" + clientId + "' is not registered.")
                    .build());
            return;
        }

        final SecurityContext oldSecurityContext = context.getSecurityContext();
        context.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> clientId;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return oldSecurityContext.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return oldSecurityContext.getAuthenticationScheme();
            }
        });

    }
}
