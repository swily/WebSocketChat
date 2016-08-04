package chat;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import io.baratine.inject.Injector;
import io.baratine.pipe.PipeBroker;
import io.baratine.service.OnInit;
import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.service.Startup;
import io.baratine.timer.Timers;
import io.baratine.web.Get;
import io.baratine.web.Query;
import io.baratine.web.RequestWeb;
import io.baratine.web.Web;

@Startup
@Service
public class ChatService
{
  private static Logger LOG = Logger.getLogger(ChatService.class.getName());

  @Inject
  @Service("pipe:///messages")
  private PipeBroker<Message> _pipes;

  @Inject
  @Service
  private ScrollbackService _scrollback;

  @Inject
  @Service
  private Timers _timer;

  private LinkedHashMap<String,User> _userMap = new LinkedHashMap<>();

  private long _messageIdCount;

  @OnInit
  public void onInit()
  {
    _timer.runEvery((cancel) -> {
      for (User user : _userMap.values()) {
        if (user.isTimedout()) {
          leave(user.name(), Result.ignore());
        }
      }
    }, 10, TimeUnit.SECONDS, Result.ignore());
  }

  @Get
  public void send(@Query("user") String userName,
                   @Query("msg") String msg,
                   Result<Void> r)
  {
    LOG.fine("send: " + msg);

    User user = _userMap.get(userName);
    user.touch();

    ChatMessage chatMsg = (ChatMessage) new ChatMessage().id(_messageIdCount++).user(userName).value(msg);

    _pipes.send(chatMsg, r);
  }

  @Get
  public void join(@Query("user") String userName,
                   @Query("websocket") boolean isWebSocket,
                   Result<String[]> r)
  {
    LOG.fine("join: " + userName);

    Message msg = new UserJoinMessage().id(_messageIdCount++).user(userName);

    User user = new User(userName, isWebSocket);
    _userMap.put(userName, user);

    _pipes.send(msg, Result.ignore());

    Set<String> userSet = _userMap.keySet();
    String[] users = new String[userSet.size()];

    r.ok(userSet.toArray(users));
  }

  @Get
  public void leave(@Query("user") String userName, Result<Void> r)
  {
    LOG.fine("leave: " + userName);

    Message msg = new UserLeaveMessage().id(_messageIdCount++).user(userName);

    _userMap.remove(userName);

    _pipes.send(msg, r);
  }

  @Get("/chat")
  public void connectWebSocket(RequestWeb request)
  {
    if (request.header("Connection").contains("Upgrade")) {
      Injector injector = request.injector();

      ChatWebSocket chat = injector.instance(ChatWebSocket.class);

      request.upgrade(chat);
    }
    else {
      request.fail(new Exception("not a websocket request"));
    }
  }

  @Get
  public void getMessages(@Query("user") String userName,
                          @Query("id") long messageIdStart,
                          RequestWeb request)
  {
    if (messageIdStart <= 0) {
      messageIdStart = Long.MAX_VALUE;
    }

    User user = _userMap.get(userName);

    user.startPoll();

    _scrollback.get(messageIdStart, (msgs, e) -> {
      user.endPoll();

      if (e != null) {
        request.fail(e);
      }
      else {
        request.ok(msgs);
      }
    });
  }

  public static void main(String[] args)
  {
    Logger.getLogger(ChatWebSocket.class.getPackage().getName()).setLevel(Level.FINE);

    Web.include(ChatWebSocket.class);
    Web.include(ChatService.class);
    Web.include(ScrollbackService.class);

    Web.start();
  }
}
