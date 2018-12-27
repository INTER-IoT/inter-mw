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
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import joptsimple.internal.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@PreMatching
public class BasicAuthzFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InterMwApiREST.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PASSWORD_FILE_PROPERTY = "basicauthz.passwordfile";
    private static Pattern AUTH_HEADER_PATTERN = Pattern.compile("Basic ([A-Za-z0-9+/=]+)");
    private ParliamentRegistry parliamentRegistry;
    private Map<String, String> allowedUsers;

    public BasicAuthzFilter() throws Exception {
        logger.debug("BasicAuthzFilter is initializing...");
        try {
            parliamentRegistry = new ParliamentRegistry(Context.getConfiguration());
            Configuration conf = new DefaultConfiguration(CONFIG_FILE);

            String passwordFile = conf.getProperty(PASSWORD_FILE_PROPERTY);
            if (Strings.isNullOrEmpty(passwordFile)) {
                throw new MiddlewareException("'%s' configuration property is not set.", PASSWORD_FILE_PROPERTY);
            }
            if (!Files.isReadable(Paths.get(passwordFile))) {
                throw new MiddlewareException("Failed to read password file %s.", passwordFile);
            }

            allowedUsers = new HashMap<>();
            try (Stream<String> stream = Files.lines(Paths.get(passwordFile))) {
                stream.forEach(line -> {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) {
                        return;
                    }
                    String[] tokens = line.split(":");
                    allowedUsers.put(tokens[0], tokens[1]);
                });
            } catch (Exception e) {
                throw new MiddlewareException("Invalid format of password file %s.", passwordFile);
            }

            logger.debug("BasicAuthzFilter has been initialized successfully.");

        } catch (Exception e) {
            logger.error("BasicAuthzFilter failed to initialize.", e);
            throw new MiddlewareException("Failed to initialize authorization filter. Please see server log for details.");
        }
    }

    @Override
    public void filter(ContainerRequestContext context) {
        try {
            filterImpl(context);

        } catch (NotAuthorizedException e) {
            context.abortWith(Response.status(e.getStatus())
                    .entity(e.getMessage())
                    .build());

        } catch (Exception e) {
            logger.error("Failed to authorize request.", e);
            context.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to authorize request. Please see server log for details.")
                    .build());
        }
    }

    private void filterImpl(ContainerRequestContext context) throws Exception {
        String requestPath = context.getUriInfo().getPath();
        if (requestPath.equals("swagger.json")) {
            return;
        }

        if (!context.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            throw new NotAuthorizedException(Response.Status.UNAUTHORIZED, "The 'Authorization' header is missing.");
        }

        Matcher matcher = AUTH_HEADER_PATTERN.matcher(context.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (!matcher.matches()) {
            throw new NotAuthorizedException(Response.Status.UNAUTHORIZED, "Invalid Authorization header.");
        }

        String username = null;
        String passwordDigest = null;
        try {
            String token = matcher.group(1);
            String credentials = new String(Base64.getDecoder().decode(token));
            String[] credentialsArray = credentials.split(":");
            username = credentialsArray[0];
            String password = credentialsArray[1];
            passwordDigest = DigestUtils.sha256Hex(password);

        } catch (Exception e) {
            throw new NotAuthorizedException(Response.Status.UNAUTHORIZED, "Invalid Authorization header.");
        }

        if (!allowedUsers.containsKey(username) ||
                !allowedUsers.get(username).equals(passwordDigest)) {
            throw new NotAuthorizedException(Response.Status.FORBIDDEN, "Invalid username or password.");
        }

        final String clientId = username;
        if (requestPath.equals("mw2mw/clients") && context.getMethod().equals("POST")) {
            // skip the client registration check
        } else {
            boolean isClientRegistered;
            try {
                isClientRegistered = parliamentRegistry.isClientRegistered(clientId);
            } catch (Exception e) {
                throw new MiddlewareException("Failed to query Parliament registry.", e);
            }

            if (!isClientRegistered) {
                throw new NotAuthorizedException(Response.Status.FORBIDDEN,
                        String.format("Client '%s' is not registered within INTER-MW.", clientId));
            }
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

    private class NotAuthorizedException extends Exception {
        private Response.Status status;
        private String message;

        public NotAuthorizedException(Response.Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public Response.Status getStatus() {
            return status;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}