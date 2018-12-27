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
package eu.interiot.intermw.commons;

import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.commons.exceptions.ErrorCode;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.ErrorMessageMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private final static Logger logger = LoggerFactory.getLogger(ErrorReporter.class);
    private Publisher<Message> errorPublisher;

    public ErrorReporter(Publisher<Message> errorPublisher) {
        this.errorPublisher = errorPublisher;
    }

    public void sendErrorResponseMessage(Message originalMessage, Exception exception, ErrorCode errorCode) {
        sendErrorResponseMessage(originalMessage, exception, null, errorCode);
    }

    public void sendErrorResponseMessage(Message originalMessage, Exception exception, String description, ErrorCode errorCode) {
        Message errorMessage = createErrorResponseMessage(originalMessage, exception, description, errorCode);
        sendMessageToErrorTopic(errorMessage);
    }

    public void sendErrorResponseMessage(Message originalMessage, Exception exception, ErrorCode errorCode,
                                         Publisher<Message> publisher) {
        sendErrorResponseMessage(originalMessage, exception, null, errorCode, publisher);
    }

    public void sendErrorResponseMessage(Message originalMessage, Exception exception, String description,
                                         ErrorCode errorCode, Publisher<Message> publisher) {
        Message errorMessage = createErrorResponseMessage(originalMessage, exception, description, errorCode);

        try {
            publisher.publish(errorMessage);

        } catch (Exception e) {
            logger.error(String.format("Failed to publish error message %s to the exchange %s: %s",
                    errorMessage.getMetadata().getMessageID().get(), publisher.getTopic().getExchangeName(), e.getMessage()), e);

            ErrorCode errorCode1 = ErrorCode.CANNOT_PUBLISH_MESSAGE_UPSTREAM;
            Message errorMessage1 = new Message();
            ErrorMessageMetadata metadata1 = errorMessage.getMetadata().asErrorMessageMetadata();
            errorMessage1.setMetadata(metadata1);
            metadata1.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.ERROR);
            metadata1.setErrorCategory(errorCode1.name());
            metadata1.setErrorDescription(errorCode1.getErrorDescription());
            sendMessageToErrorTopic(errorMessage1);
        }

        // send a copy of error message to ERROR stream
        sendMessageToErrorTopic(errorMessage);
    }

    private Message createErrorResponseMessage(Message originalMessage, Exception exception, String description, ErrorCode errorCode) {
        String conversationID = originalMessage.getMetadata().getConversationId().get();
        logger.debug("Creating error response message based on the message {} for conversation {}...",
                originalMessage.getMetadata().getMessageID().get(), conversationID);

        Message errorMessage = new Message(originalMessage.getMessageConfig());
        ErrorMessageMetadata metadata = errorMessage.getMetadata().asErrorMessageMetadata();
        errorMessage.setMetadata(metadata);
        metadata.setMessageTypes(originalMessage.getMetadata().getMessageTypes());
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.ERROR);
        metadata.setConversationId(conversationID);
        metadata.setExceptionStackTrace(exception);
        metadata.setErrorCategory(errorCode != null ? errorCode.name() : "N/A");

        List<String> descriptions = new ArrayList<>();
        if (errorCode != null) {
            descriptions.add(errorCode.getErrorDescription());
        }
        if (description != null) {
            descriptions.add(description);
        }
        if (exception.getMessage() != null) {
            descriptions.add(exception.toString());
        }
        Throwable cause = exception.getCause();
        int depth = 1;
        while (cause != null) {
            descriptions.add(cause.toString());
            cause = cause.getCause();
            depth++;
            if (depth > 3) {
                break;
            }
        }

        if (descriptions.isEmpty()) {
            descriptions.add("No error description available.");
        }
        metadata.setErrorDescription(StringUtils.join(descriptions, "\n"));

        try {
            metadata.setOriginalMessage(originalMessage);

        } catch (IOException e) {
            logger.warn(String.format(
                    "Failed to serialize message %s: %s", originalMessage.getMetadata().getMessageID().get(), e.getMessage()), e);
            metadata.setOriginalMessage("Malformed message.");
        }

        return errorMessage;
    }

    private void sendMessageToErrorTopic(Message errorMessage) {
        try {
            logger.debug("Publishing error message {} to ERROR stream...", errorMessage.getMetadata().getMessageID().get());
            errorPublisher.publish(errorMessage);

        } catch (Exception e) {
            logger.error(String.format("Failed to publish error message %s to ERROR stream: %s",
                    errorMessage.getMetadata().getMessageID().get(), e.getMessage()), e);
        }
    }
}
