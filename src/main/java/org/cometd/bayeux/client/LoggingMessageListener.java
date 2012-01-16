package org.cometd.bayeux.client;

import org.cometd.bayeux.Message;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * 
 * @author gregoryw
 *
 */
public class LoggingMessageListener implements MessageListener {
	
	private final Logger logger = Log.getLogger(getClass().getName());

	@Override
	public void onMessage(ClientSessionChannel channel, Message message) {
		logger.debug(message.getJSON(), message);
	}

}
