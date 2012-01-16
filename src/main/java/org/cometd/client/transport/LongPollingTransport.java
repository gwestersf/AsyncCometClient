package org.cometd.client.transport;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.Response.ResponseBuilder;

/**
 * 
 */
public class LongPollingTransport extends ClientTransport {
	
	private final Logger logger = Log.getLogger(getClass().getName());
	
	//TODO: gw: put these in a jaxb config file so we can change them without sys test recompiling
	public static final int IDLE_TIMEOUT_IN_MS = 5000;
	public static final int MAX_CONNECTIONS_PER_HOST = 65535;
	
	
	private final AsyncHttpClient _httpClient;
	//private final List<HttpExchange> _exchanges = new ArrayList<HttpExchange>();
	private volatile boolean _aborted;
	private volatile BayeuxClient _bayeuxClient;
	private volatile String _uri;
	private volatile boolean _appendMessageType;

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
		_aborted = false;
		_bayeuxClient = bayeux;
		_uri = uri;
		Pattern uriRegexp = Pattern.compile("(^https?://(([^:/\\?#]+)(:(\\d+))?))?([^\\?#]*)(.*)?");
		Matcher uriMatcher = uriRegexp.matcher(uri.toString());
		if (uriMatcher.matches()) {
			String afterPath = uriMatcher.group(7);
			_appendMessageType = afterPath == null
					|| afterPath.trim().length() == 0;
		}
		super.init(bayeux, uri);
	}

	@Override
	public void abort() {
		synchronized (this) {
			_aborted = true;
			_bayeuxClient.abort();
		}
	}

	@Override
	public void reset() {
	}
	
	/*
	private String getUrl(Message.Mutable... messages) {
		String url = _uri.toString();
		if (_appendMessageType && messages.length == 1 && messages[0].isMeta()) {
			String type = messages[0].getChannel().substring(Channel.META.length());
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			url += type;
		} 
		return url;
	}*/
	
	
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
			.build();

		try {
			_httpClient.executeRequest(request, listener);
		} catch (IOException e) {
			logger.info(e);
		}
	}
}
