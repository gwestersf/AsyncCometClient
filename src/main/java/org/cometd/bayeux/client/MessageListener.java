package org.cometd.bayeux.client;

import org.cometd.bayeux.Message;

/**
 * A listener for messages on a {@link ClientSessionChannel}.
 */
public interface MessageListener extends ClientSessionChannelListener {

    /**
     * Callback invoked when a message is received on the given {@code channel}.
     * @param channel the channel that received the message
     * @param message the message received
     */
    void onMessage(ClientSessionChannel channel, Message message);
}
