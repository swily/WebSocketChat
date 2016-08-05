package chat;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.service.ServiceRef;
import io.baratine.web.ServiceWebSocket;
import io.baratine.web.Web;
import io.baratine.web.WebSocket;
import io.baratine.web.WebSocketClose;
import io.baratine.pipe.Pipe;
import io.baratine.pipe.PipeBroker;
import io.baratine.pipe.PipeSub;

public class ChatWebSocket implements ServiceWebSocket<Message, Message>
{
  private static Logger LOG = Logger.getLogger(ChatWebSocket.class.getName());

  @Inject
  @Service
  private ChatService _chat;

  @Inject
  @Service("pipe:///messages")
  private PipeBroker<Message> _pipeBroker;
  private Pipe<Message> _messagePipeHandler;

  private String _user;

  private WebSocket<Message> _ws;

  @Override
  public void open(WebSocket<Message> ws)
  {
    LOG.fine("opened websocket connection: " + _user + "," + this);

    _ws = ws;
  }

  @Override
  public void next(Message msg, WebSocket<Message> ws) throws IOException
  {
    LOG.fine("next message: " + msg);

    String type = msg.type();
    String user = msg.user();
    String value = msg.value();

    if ("join".equals(type)) {
      join(user, ws);
    }
    else if ("leave".equals(type)) {
      leave(user, ws, false);
    }
    else if ("message".equals(type)) {
      _chat.send(user, value, Result.ignore());
    }
    else {
      throw new IOException("unknown message: " + msg);
    }
  }

  @Override
  public void close(WebSocketClose code, String msg, WebSocket<Message> webSocket) throws IOException
  {
    LOG.fine("close websocket connection: " + code + "," + this);

    leave(_user, webSocket, true);
  }

  private void join(String user, WebSocket<Message> ws)
  {
    _user = user;
    boolean isWebSocket = true;

    _chat.join(user, isWebSocket, (users, e) -> {
      LOG.fine("user joined: " + user);

      UserListMessage userListMsg = new UserListMessage().users(users);

      ws.next(userListMsg);

      PipeSub<Message> messageResult = PipeSub.of(msg -> {
        onPipeReceive(msg);
      });

      _pipeBroker.subscribe(messageResult);
      _messagePipeHandler = messageResult.pipe();
    });
  }

  private void onPipeReceive(Message msg)
  {
    if (_ws != null) {
      _ws.next(msg);
    }
  }

  private void leave(String user, WebSocket<Message> ws, boolean isClosed)
  {
    if (_user == null) {
      return;
    }

    _ws = null;
    _user = null;
    _chat.leave(user, Result.ignore());

    if (_messagePipeHandler != null) {
      _messagePipeHandler.close();

      _messagePipeHandler = null;
    }

    if (! isClosed) {
      ws.close();
    }
  }
}
