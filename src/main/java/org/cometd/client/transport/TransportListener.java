package org.cometd.client.transport;

import java.util.List;

import org.cometd.bayeux.Message;

/**
 * @version $Revision: 902 $ $Date: 2010-05-26 17:01:08 +0200 (Wed, 26 May 2010) $
 */
public interface TransportListener
{
    void onSending(Message[] messages);

    void onMessages(List<Message.Mutable> messages);

    void onConnectException(Throwable x);

    void onException(Throwable x);

    void onExpire();

    void onProtocolError(String info);
}
