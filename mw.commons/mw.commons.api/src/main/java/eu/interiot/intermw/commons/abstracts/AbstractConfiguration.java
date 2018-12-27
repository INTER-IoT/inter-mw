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
package eu.interiot.intermw.commons.abstracts;

import com.google.common.base.Strings;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract class for a {@link Properties} based {@link Configuration}
 *
 * @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public abstract class AbstractConfiguration {
    private final static String CONFIG_LOCATION_PROP = "intermw.config.location";
    private final static String INTERMW_ENV_VAR_PREFIX = "intermw_";
    protected Properties properties;
    final Logger log = LoggerFactory.getLogger(AbstractConfiguration.class);

    public AbstractConfiguration() {
        this.properties = new Properties();
    }

    /**
     * Creates a new instance given a <propertiesFileName>
     *
     * @param propertiesFileName The properties file name
     * @throws MiddlewareException
     */
    public AbstractConfiguration(String propertiesFileName) throws MiddlewareException {
        this.properties = loadPropertiesFile(propertiesFileName);
        loadPropertiesFromEnv();
    }

    protected Properties loadPropertiesFile(String propertiesFileName) throws MiddlewareException {
        String configLocation = System.getProperty("intermw.config.location");
        Properties properties;

        if (Strings.isNullOrEmpty(configLocation)) {
            log.info("System property {} is not set. Properties file '{}' will be loaded from classpath.",
                    CONFIG_LOCATION_PROP, propertiesFileName);
            properties = loadPropertiesFileFromCP(propertiesFileName);

        } else {
            log.info("System property {} is set to '{}'. Properties file '{}' will be loaded from specified location.",
                    CONFIG_LOCATION_PROP, configLocation, propertiesFileName);
            if (propertiesFileName.startsWith("*.")) {
                properties = loadPropertiesFilesFromPath(configLocation, propertiesFileName);
            } else {
                properties = loadPropertiesFileFromPath(configLocation, propertiesFileName);
            }
        }

        log.info("Properties file {} has been loaded successfully.", propertiesFileName);
        return properties;
    }

    protected Properties loadPropertiesFileFromPath(String configLocation, String propertiesFileName) throws MiddlewareException {
        File configFile = new File(configLocation, propertiesFileName);
        log.debug("Loading properties from file {}...", configFile.getAbsolutePath());

        if (!configFile.exists()) {
            throw new MiddlewareException(String.format("Configuration file '%s' does not exist.", configFile.getAbsolutePath()));
        }

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(configFile);
            Properties props = new Properties();
            props.load(fileInputStream);
            return props;

        } catch (Exception e) {
            log.error("Failed to load properties file " + configFile.getAbsolutePath() + ": " + e.getMessage(), e);
            throw new MiddlewareException(e);

        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    protected Properties loadPropertiesFilesFromPath(String configLocation, String propertiesFileFilter) throws MiddlewareException {
        log.debug("Loading properties from all files matching filter '{}' in '{}'.", propertiesFileFilter, configLocation);
        Collection<File> propFiles = new ArrayList<>();

        String baseFileName = propertiesFileFilter.substring(2);
        File baseFile = new File(configLocation, baseFileName);
        if (!baseFile.exists()) {
            throw new MiddlewareException(String.format("Configuration file '%s' does not exist.", baseFile.getAbsolutePath()));
        }
        propFiles.add(baseFile);

        Collection<File> prefixPropFiles = FileUtils.listFiles(new File(configLocation), new WildcardFileFilter(propertiesFileFilter), null);
        propFiles.addAll(prefixPropFiles);

        log.debug("Following properties files have been found: {}...", propFiles.toString());
        Properties allProps = new Properties();
        for (File propFile : propFiles) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(propFile);
                Properties props = new Properties();
                props.load(fileInputStream);
                allProps.putAll(props);

            } catch (Exception e) {
                throw new MiddlewareException(String.format("Failed to load properties file '%s'.", propFile.getAbsolutePath()), e);

            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }

        return allProps;
    }

    protected Properties loadPropertiesFileFromCP(String propertiesFileName) throws MiddlewareException {
        log.debug("Searching for properties file '{}' in classpath...", propertiesFileName);
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFileName);
            Properties prop = new Properties();
            prop.load(is);
            return prop;

        } catch (Exception e) {
            throw new MiddlewareException(String.format("Failed to load properties file '%s' from classpath.", propertiesFileName), e);

        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private void loadPropertiesFromEnv() {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith(INTERMW_ENV_VAR_PREFIX)) {
                String propertyName = entry.getKey().substring(INTERMW_ENV_VAR_PREFIX.length());
                propertyName = propertyName.replaceAll("_", ".");
                properties.put(propertyName, entry.getValue());
            }
        }
    }
}
