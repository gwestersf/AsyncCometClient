package org.cometd.bayeux.client;

import org.cometd.bayeux.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author gregoryw
 *
 */
public class LoggingMessageListener implements MessageListener {
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	@Override
	public void onMessage(ClientSessionChannel channel, Message message) {
		logger.debug(message.getJSON(), message);
	}

}
