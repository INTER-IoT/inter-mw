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
package eu.interiot.intermw.comm.arm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.commons.exceptions.InvalidMessageException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.responses.*;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Gasper Vrhovsek
 */
public class ResponseMessageParser {
    //    private static final Logger logger = LoggerFactory.getLogger(ResponseMessageParser.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static BaseRes castToMessageType(String rawJsonMessage) throws IOException {
        BaseRes baseRes = objectMapper.readValue(rawJsonMessage, BaseRes.class);

        Set<MessageTypesEnum> messageTypes = baseRes.getMessageTypes();
        messageTypes.remove(MessageTypesEnum.RESPONSE);

        BaseRes response;
        if (messageTypes.iterator().hasNext()) {
            MessageTypesEnum messageType = messageTypes.iterator().next();

            switch (messageType) {
                case OBSERVATION:
                    response = objectMapper.readValue(rawJsonMessage, ObservationRes.class);
                    break;
                case PLATFORM_REGISTER:
                    response = objectMapper.readValue(rawJsonMessage, PlatformRegisterRes.class);
                    break;
                case PLATFORM_UPDATE:
                    response = objectMapper.readValue(rawJsonMessage, PlatformUpdateRes.class);
                    break;
                case PLATFORM_CREATE_DEVICE:
                    response = objectMapper.readValue(rawJsonMessage, PlatformCreateDeviceRes.class);
                    break;
                case PLATFORM_UPDATE_DEVICE:
                    response = objectMapper.readValue(rawJsonMessage, PlatformUpdateDeviceRes.class);
                    break;
                case PLATFORM_DELETE_DEVICE:
                    response = objectMapper.readValue(rawJsonMessage, PlatformDeleteDeviceRes.class);
                    break;
                case SUBSCRIBE:
                    response = objectMapper.readValue(rawJsonMessage, SubscribeRes.class);
                    break;
                case UNSUBSCRIBE:
                    response = objectMapper.readValue(rawJsonMessage, UnsubscribeRes.class);
                    break;
                case ACTUATION:
                    response = objectMapper.readValue(rawJsonMessage, ActuationRes.class);
                    break;
                case PLATFORM_UNREGISTER:
                    response = objectMapper.readValue(rawJsonMessage, PlatformUnregisterRes.class);
                    break;
                case LIST_SUPPORTED_PLATFORM_TYPES:
                    response = objectMapper.readValue(rawJsonMessage, ListPlatformTypesRes.class);
                    break;
                default:
                    response = objectMapper.readValue(rawJsonMessage, BaseRes.class);
                    break;
            }
            return response;
        }
        throw new ClassCastException("No suitable class to cast JSON message to.");
    }

    public static String convertMessages(Client.ResponseFormat responseFormat, List<Message> messages) throws MiddlewareException {
        if (messages.isEmpty()) {
            return "[]";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            Iterator<Message> iterator = messages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                String serializedMessage = convertMessage(message, responseFormat);
                sb.append(serializedMessage);
                if (iterator.hasNext()) {
                    sb.append(",\n");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static String convertMessage(Message message, Client.ResponseFormat responseFormat) throws MiddlewareException {
        String messageID = message.getMetadata().getMessageID().get();
        if (responseFormat.equals(Client.ResponseFormat.JSON)) {
            try {
                Set<MessageTypesEnum> messageTypes = message.getMetadata().getMessageTypes();
                messageTypes.remove(MessageTypesEnum.RESPONSE);
                if (messageTypes.isEmpty()) {
                    throw new InvalidMessageException(String.format(
                            "Failed to serialize message %s to JSON: missing message type.", messageID));
                }
                MessageTypesEnum messageType = messageTypes.iterator().next();
                return convertMessageToSimpleJson(message, messageType);
            } catch (Exception e) {
                throw new MiddlewareException(String.format(
                        "Failed to serialize message %s to JSON.", messageID), e);
            }

        } else if (responseFormat.equals(Client.ResponseFormat.JSON_LD)) {
            try {
                return message.serializeToJSONLD();
            } catch (Exception e) {
                throw new MiddlewareException(String.format(
                        "Failed to serialize message %s to JSON-LD.", messageID), e);
            }

        } else {
            throw new MiddlewareException(String.format("Unsupported response format: %s.", responseFormat));
        }
    }

    private static String convertMessageToSimpleJson(Message message, MessageTypesEnum messageType) throws JsonProcessingException {
        String messageSimpleJSON;
        switch (messageType) {
            case OBSERVATION:
                ObservationRes observationRes = new ObservationRes(message);
                messageSimpleJSON = objectMapper.writeValueAsString(observationRes);
                break;
            case PLATFORM_REGISTER:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformRegisterRes(message));
                break;
            case PLATFORM_UPDATE:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformUpdateRes(message));
                break;
            case PLATFORM_CREATE_DEVICE:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformCreateDeviceRes(message));
                break;
            case PLATFORM_UPDATE_DEVICE:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformUpdateDeviceRes(message));
                break;
            case PLATFORM_DELETE_DEVICE:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformDeleteDeviceRes(message));
                break;
            case SUBSCRIBE:
                messageSimpleJSON = objectMapper.writeValueAsString(new SubscribeRes(message));
                break;
            case UNSUBSCRIBE:
                messageSimpleJSON = objectMapper.writeValueAsString(new UnsubscribeRes(message));
                break;
            case ACTUATION:
                messageSimpleJSON = objectMapper.writeValueAsString(new ActuationRes(message));
                break;
            case PLATFORM_UNREGISTER:
                messageSimpleJSON = objectMapper.writeValueAsString(new PlatformUnregisterRes(message));
                break;
            case LIST_SUPPORTED_PLATFORM_TYPES:
                messageSimpleJSON = objectMapper.writeValueAsString(new ListPlatformTypesRes(message));
                break;
            default:
                messageSimpleJSON = objectMapper.writeValueAsString(new BaseRes(message));
                break;
        }
        return messageSimpleJSON;
    }
}
