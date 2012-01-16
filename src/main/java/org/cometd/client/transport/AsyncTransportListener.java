package org.cometd.client.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	private final ResponseBuilder builder = new ResponseBuilder();

	@Override
	public void onThrowable(Throwable t) {
		logger.error("Non blocking IO problem", t);
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
