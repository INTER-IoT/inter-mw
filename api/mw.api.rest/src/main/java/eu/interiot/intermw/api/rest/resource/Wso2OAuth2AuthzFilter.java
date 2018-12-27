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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.commons.Context;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.services.registry.ParliamentRegistry;
import joptsimple.internal.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PreMatching
public class Wso2OAuth2AuthzFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InterMwApiREST.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String INTROSPECT_PATH = "/oauth2/introspect";
    private static final String WSO2IS_BASE_URL_PROPERTY = "wso2is.baseUrl";
    private static final String WSO2IS_USERNAME_PROPERTY = "wso2is.username";
    private static final String WSO2IS_PASSWORD_PROPERTY = "wso2is.password";
    private static final String TRUSTSTORE_LOCATION_PROP = "wso2is.ssl.truststore.location";

    private static Pattern AUTH_HEADER_PATTERN = Pattern.compile("Bearer ([\\w-]+)");
    private ObjectMapper objectMapper = new ObjectMapper();
    private ParliamentRegistry parliamentRegistry;
    private String base64EncodedCredentials;
    private String introspectEndpointUrl;
    private HttpClient httpClient;

    public Wso2OAuth2AuthzFilter() throws Exception {
        logger.debug("Wso2OAuth2AuthzFilter is initializing...");
        try {
            parliamentRegistry = new ParliamentRegistry(Context.getConfiguration());
            Configuration conf = new DefaultConfiguration(CONFIG_FILE);

            if (Strings.isNullOrEmpty(conf.getProperty(WSO2IS_BASE_URL_PROPERTY))) {
                throw new MiddlewareException("'%s' configuration property is not set.", WSO2IS_BASE_URL_PROPERTY);
            }
            if (Strings.isNullOrEmpty(conf.getProperty(WSO2IS_USERNAME_PROPERTY))) {
                throw new MiddlewareException("'%s' configuration property is not set.", WSO2IS_USERNAME_PROPERTY);
            }
            if (Strings.isNullOrEmpty(conf.getProperty(WSO2IS_PASSWORD_PROPERTY))) {
                throw new MiddlewareException("'%s' configuration property is not set.", WSO2IS_PASSWORD_PROPERTY);
            }

            String credentials = conf.getProperty(WSO2IS_USERNAME_PROPERTY) + ":" + conf.getProperty(WSO2IS_PASSWORD_PROPERTY);
            base64EncodedCredentials = new String(Base64.getEncoder().encode(credentials.getBytes()), StandardCharsets.UTF_8);
            introspectEndpointUrl = conf.getProperty(WSO2IS_BASE_URL_PROPERTY) + INTROSPECT_PATH;

            String truststoreLocation = conf.getProperty(TRUSTSTORE_LOCATION_PROP);

            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (!Strings.isNullOrEmpty(truststoreLocation)) {
                sslContextBuilder.loadTrustMaterial(new File(truststoreLocation));
            }

            SSLContext sslcontext = sslContextBuilder.build();
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslcontext);
            httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sf)
                    .build();

            logger.debug("Wso2OAuth2AuthzFilter has been initialized successfully.");

        } catch (Exception e) {
            logger.error("Wso2OAuth2AuthzFilter failed to initialize: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void filter(ContainerRequestContext context) {
        try {
            filterImpl(context);

        } catch (Exception e) {
            logger.error("Failed to validate OAuth access token.", e);
            context.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to validate OAuth access token. Please see server log for details.")
                    .build());
        }
    }

    private void filterImpl(ContainerRequestContext context) throws Exception {
        String requestPath = context.getUriInfo().getPath();
        if (requestPath.equals("swagger.json")) {
            return;
        }

        if (!context.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("The 'Authorization' header is missing.")
                    .build());
            return;
        }

        Matcher matcher = AUTH_HEADER_PATTERN.matcher(context.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (!matcher.matches()) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid 'Authorization' header.")
                    .build());
            return;
        }

        String token = matcher.group(1);

        HttpPost httpPost = new HttpPost(introspectEndpointUrl);
        HttpEntity httpEntity = new StringEntity("token=" + token,
                ContentType.APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.UTF_8));
        httpPost.setEntity(httpEntity);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + base64EncodedCredentials);

        HttpResponse response;
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            throw new Exception(String.format("Failed to execute HTTP request to %s.", introspectEndpointUrl), e);
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception(String.format("Unexpected response code received from Identity Server: %s.", response.getStatusLine()));
        }

        HttpEntity entity = response.getEntity();
        WSO2User wso2User;
        try {
            wso2User = objectMapper.readValue(entity.getContent(), WSO2User.class);
        } catch (IOException e) {
            throw new Exception("Failed to deserialize WSO2User.", e);
        }

        if (!wso2User.active) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid access token or inactive user.")
                    .build());
            return;
        }

        String clientId = getClientId(wso2User.username);
        if (requestPath.equals("mw2mw/clients") && context.getMethod().equals("POST")) {
            // skip the client registration check
        } else {
            try {
                if (!parliamentRegistry.isClientRegistered(clientId)) {
                    context.abortWith(Response.status(Response.Status.FORBIDDEN)
                            .entity(String.format("Client '%s' is not registered within INTER-MW.", clientId))
                            .build());
                    return;
                }
            } catch (Exception e) {
                throw new Exception("Failed to query Parliament registry.", e);
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

    private String getClientId(String username) {
        return username.substring(username.indexOf('/') + 1, username.indexOf('@'));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WSO2User {
        public boolean active;
        public String username;
    }
}