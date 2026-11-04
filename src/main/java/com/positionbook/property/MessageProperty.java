package com.positionbook.property;

import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

/**
 * Provides access to localized application messages.
 */
@Component
public class MessageProperty {

    private final MessageSource messageSource;
    private MessageSourceAccessor accessor;

    /**
     * Creates the message accessor for the configured Spring message source.
     *
     * @param messageSource configured message source
     */
    public MessageProperty(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Initializes the accessor after dependency injection.
     */
    @PostConstruct
    public void init() {
        this.accessor = new MessageSourceAccessor(messageSource);
    }

    /**
     * Resolves a message by its configured code.
     *
     * @param code message bundle key
     * @return resolved message
     */
    public String getMessage(String code) {
        return accessor.getMessage(code);
    }
}
