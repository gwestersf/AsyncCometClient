package org.cometd.client.transport;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import com.ning.http.client.Response.ResponseBuilder;

/**
 * 
 * @author gregoryw
 *
 */
public class AsyncTransportListener implements TransportListener {
	
	private final Logger logger = Log.getLogger(getClass().getName());

	private final ResponseBuilder builder = new ResponseBuilder();

	@Override
	public void onThrowable(Throwable t) {
		logger.info(t);
	}

	@Override
	public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		builder.accumulate(bodyPart);
		return STATE.CONTINUE;
	}

	@Override
	public AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		builder.accumulate(responseStatus);
		return STATE.CONTINUE;
	}

	@Override
	public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
		builder.accumulate(headers);
		return STATE.CONTINUE;
	}

	@Override
	public Response onCompleted() throws Exception {
		Response response = builder.build();
		logger.debug(response.getResponseBody());
		return response;
	}

}
