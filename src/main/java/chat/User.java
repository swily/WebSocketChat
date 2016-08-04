package chat;

public class User implements Comparable<User>
{
  private String _name;
  private boolean _isWebSocket;

  private long _joinTimeMs;

  private int _pollCount;
  private long _lastActivityTimeMs;

  public User(String name, boolean isWebSocket)
  {
    _name = name;
    _isWebSocket = isWebSocket;

    _joinTimeMs = System.currentTimeMillis();
    touch();
  }

  public String name()
  {
    return _name;
  }

  public long joinTime()
  {
    return _joinTimeMs;
  }

  public void startPoll()
  {
    _pollCount++;
  }

  public void endPoll()
  {
    _pollCount--;
    touch();
  }

  public void touch()
  {
    _lastActivityTimeMs = System.currentTimeMillis();
  }

  public boolean isTimedout()
  {
    return ! _isWebSocket
           && _pollCount <= 0
           && (System.currentTimeMillis() - _lastActivityTimeMs >= 1000 * 30);
  }

  public int compareTo(User user)
  {
    int result = (int) (_joinTimeMs - user._joinTimeMs);

    if (result != 0) {
      return result;
    }
    else {
      return _name.compareTo(user._name);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[name=" + _name + ",ws=" + _isWebSocket + ",pollCount=" + _pollCount + "]";
  }
}
