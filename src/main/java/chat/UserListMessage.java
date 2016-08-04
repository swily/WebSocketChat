package chat;

import java.util.ArrayList;
import java.util.List;

public class UserListMessage extends Message
{
  private String[] users;

  public UserListMessage()
  {
    type("list");
  }

  public UserListMessage users(String[] u)
  {
    users = u;

    return this;
  }

  public String[] users()
  {
    return users;
  }

  @Override
  public String toString()
  {
    ArrayList<String> list = new ArrayList<>();

    for (String user : users) {
      list.add(user);
    }

    return getClass().getSimpleName() + "[" + list + "]";
  }
}
