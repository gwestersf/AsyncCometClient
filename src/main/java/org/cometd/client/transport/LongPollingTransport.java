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
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.ajax.JSON;
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
	
	//TODO: gw: put these in a jaxb config file so we can change them without sys test recompiling
	public static final int IDLE_TIMEOUT_IN_MS = 5000;
	public static final int MAX_CONNECTIONS_PER_HOST = 65535;
	
	
	private final AsyncHttpClient _httpClient;
	//private final List<HttpExchange> _exchanges = new ArrayList<HttpExchange>();
	private volatile boolean _aborted;
	private volatile BayeuxClient _bayeuxClient;
	private volatile Request _uri;
	private volatile boolean _appendMessageType;

	public static LongPollingTransport create(Map<String, Object> options) {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder(). 
			setIdleConnectionInPoolTimeoutInMs(IDLE_TIMEOUT_IN_MS).
			setMaximumConnectionsPerHost(MAX_CONNECTIONS_PER_HOST).
			build();
		
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
	public void init(BayeuxClient bayeux, Request uri) {
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
	}

	@Override
	public void send(final TransportListener listener, Message.Mutable... messages) {
		String content = JSON.toString(messages);
		
		RequestBuilder builder = new RequestBuilder("PUT");
		Request request = builder.setUrl(getUrl(messages))
			.add
		
		AsyncHandler<Response> asyncHandler = new CometAsyncHandler<Response>();



		
		httpExchange.setRequestContentType("application/json;charset=UTF-8");
		try {
			httpExchange.setRequestContent(new ByteBuffer(content, "UTF-8"));
			
			if (_bayeuxClient != null)
				_bayeuxClient.customize(httpExchange);

			synchronized (this) {
				if (_aborted)
					throw new IllegalStateException("Aborted");
				_exchanges.add(httpExchange);
			}

			_httpClient.send(httpExchange);
		} catch (Exception x) {
			listener.onException(x);
		}
	}
	
	private class CometAsyncHandler<Response> implements AsyncHandler<Response> {
		private final ResponseBuilder builder = new ResponseBuilder();

		@Override
		public void onThrowable(Throwable t) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Request onCompleted() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	private class TransportExchange extends ContentExchange {
		private final TransportListener _listener;
		private final Message[] _messages;

		private TransportExchange(TransportListener listener, Message... messages) {
			super(true);
			_listener = listener;
			_messages = messages;
		}

		@Override
		protected void onRequestCommitted() throws IOException {
			_listener.onSending(_messages);
		}

		@Override
		protected void onResponseHeader(Buffer name, Buffer value) throws IOException {
			super.onResponseHeader(name, value);
			int headerName = HttpHeaders.CACHE.getOrdinal(name);
			if (headerName == HttpHeaders.SET_COOKIE_ORDINAL) {
				QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(
						value.toString(), "=;", false, false);
				tokenizer.setSingle(false);

				String cookieName = null;
				if (tokenizer.hasMoreTokens())
					cookieName = tokenizer.nextToken();

				String cookieValue = null;
				if (tokenizer.hasMoreTokens())
					cookieValue = tokenizer.nextToken();

				int maxAge = -1;

				if (cookieName != null && cookieValue != null) {
					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken();
						if ("Expires".equalsIgnoreCase(token)) {
							try {
								Date date = new SimpleDateFormat(
										"EEE, dd-MMM-yy HH:mm:ss 'GMT'")
										.parse(tokenizer.nextToken());
								Long maxAgeValue = TimeUnit.MILLISECONDS
										.toSeconds(date.getTime()
												- System.currentTimeMillis());
								maxAge = maxAgeValue > 0 ? maxAgeValue
										.intValue() : 0;
							} catch (ParseException ignored) {
							}
						} else if ("Max-Age".equalsIgnoreCase(token)) {
							try {
								maxAge = Integer.parseInt(tokenizer.nextToken());
							} catch (NumberFormatException ignored) {
							}
						}
					}

					_bayeuxClient.setCookie(cookieName, cookieValue, maxAge);
				}
			}
		}

		protected void onResponseComplete() throws IOException {
			complete();
			if (getResponseStatus() == 200) {
				String content = getResponseContent();
				if (content != null && content.length() > 0) {
					List<Message.Mutable> messages = toMessages(getResponseContent());
					_listener.onMessages(messages);
				} else
					_listener.onProtocolError("Empty response: " + this);
			} else {
				_listener.onProtocolError("Unexpected response " + getResponseStatus() + ": " + this);
			}
		}

		protected void onConnectionFailed(Throwable x) {
			complete();
			_listener.onConnectException(x);
		}

		protected void onException(Throwable x) {
			complete();
			_listener.onException(x);
		}

		protected void onExpire() {
			complete();
			_listener.onExpire();
		}

		private void complete() {
			synchronized (LongPollingTransport.this) {
				_exchanges.remove(this);
			}
		}
	}
}
