package chat;

import java.io.IOException;

import io.baratine.service.Result;
import io.baratine.web.Get;
import io.baratine.web.Path;
import io.baratine.web.ServiceWebSocket;
import io.baratine.web.Web;
import io.baratine.web.WebSocket;

public class TestWeb {

	
	public void next(String value, WebSocket<String> webSocket) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args){
		Web.include(TestWeb.class);
		Web.start();
	}
	
	@Get("/name/{room}")
	public void name(@Path("room") String room, Result<String> result){
		System.out.println("Got this path: " + room);
		result.ok("");
	}

}
