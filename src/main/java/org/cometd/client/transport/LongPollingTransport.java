package org.cometd.client.transport;

import java.io.IOException;
import java.util.Map;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

/**
 * 
 */
public class LongPollingTransport extends ClientTransport {
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public static final int IDLE_TIMEOUT_IN_MS = 5000;
	public static final int MAX_CONNECTIONS_PER_HOST = 65535;
	
	
	private final AsyncHttpClient _httpClient;
	private volatile BayeuxClient _bayeuxClient;
	private volatile String _uri;

	public static LongPollingTransport create(Map<String, Object> options) {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
			.setIdleConnectionInPoolTimeoutInMs(IDLE_TIMEOUT_IN_MS)
			.setMaximumConnectionsPerHost(MAX_CONNECTIONS_PER_HOST)
			.setRequestTimeoutInMs(30000)
			.setAllowPoolingConnection(true)
			.setCompressionEnabled(false)
			.setAllowSslConnectionPool(true)
			.build();
		
		AsyncHttpClient httpClient = new AsyncHttpClient(config);
		return create(options, httpClient);
	}

	public static LongPollingTransport create(Map<String, Object> options, AsyncHttpClient httpClient) {
		return new LongPollingTransport(options, httpClient);
	}

	public LongPollingTransport(Map<String, Object> options, AsyncHttpClient httpClient) {
		super("long-polling", options);
		_httpClient = httpClient;
	}

	@Override
	public boolean accept(String bayeuxVersion) {
		return true;
	}

	@Override
	public void init(BayeuxClient bayeux, String uri) {
		_bayeuxClient = bayeux;
		_uri = uri;
		super.init(bayeux, uri);
	}

	@Override
	public void abort() {
		synchronized (this) {
			_bayeuxClient.abort();
		}
	}

	@Override
	public void reset() {

	}
	
	
	public void send(Mutable... messages) {
		send(new AsyncTransportListener(), messages);
	}

	@Override
	public void send(final TransportListener listener, Message.Mutable... messages) {
		String content = JSON.toString(messages);
		
		RequestBuilder builder = new RequestBuilder("POST");
		Request request = builder.setUrl(_uri.toString())
			.setBody(content)
			.setBodyEncoding("UTF-8")
			.setHeader("Content-Type", "application/json;charset=UTF-8")
			.setHeader("Authorization", "OAuth " + System.getenv("SID"))
			.build();

		try {
			_httpClient.executeRequest(request, listener);
		} catch (IOException e) {
			logger.error("Error on long polling transport send", e);
		}
	}
}
