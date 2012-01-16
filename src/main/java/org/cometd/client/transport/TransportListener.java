package org.cometd.client.transport;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Response;

/**
 * This is a legacy interface from cometd that I need to keep.
 * I've gutted the methods in the interface and extended another interface
 * that has functionally identical but semantically different methods for 
 * handling asynchronous responses.
 * 
 * @author gregoryw
 *
 */
public interface TransportListener extends AsyncHandler<Response> {

}
