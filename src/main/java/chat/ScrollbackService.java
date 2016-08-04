package chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

import io.baratine.pipe.Pipe;
import io.baratine.pipe.PipeBroker;
import io.baratine.pipe.PipeSub;
import io.baratine.service.OnInit;
import io.baratine.service.Result;
import io.baratine.service.Service;

@Service
public class ScrollbackService
{
  @Inject
  @Service("pipe:///messages")
  private PipeBroker<Message> _pipes;

  private Pipe<Message> _pipe;

  private TreeMap<Long,Message> _msgList = new TreeMap<>();

  private int _scrollbackSize = 1024;

  private ArrayList<Result<Message[]>> _longPollingSubscriberList = new ArrayList<>();

  @OnInit
  public void onInit()
  {
    PipeSub<Message> messageResult = PipeSub.of(msg -> {
      onPipeReceive(msg);
    });

    _pipes.subscribe(messageResult);
    _pipe = messageResult.pipe();
  }

  private void onPipeReceive(Message msg)
  {
    if (_msgList.size() > _scrollbackSize) {
      _msgList.pollFirstEntry();
    }

    _msgList.put(msg.id(), msg);

    Message[] msgs = new Message[] { msg };

    for (Result<Message[]> result : _longPollingSubscriberList) {
      result.ok(msgs);
    }

    _longPollingSubscriberList.clear();
  }

  public void get(long messageIdStart, Result<Message[]> result)
  {
    NavigableMap<Long,Message> map = _msgList.tailMap(messageIdStart, false);

    if (map.size() > 0) {
      Message[] msgs = new Message[map.size()];

      int i = 0;
      for (Map.Entry<Long,Message> entry : map.entrySet()) {
        Message msg = entry.getValue();

        msgs[i++] = msg;
      }

      result.ok(msgs);
    }
    else {
      _longPollingSubscriberList.add(result);
    }
  }
}
