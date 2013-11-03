package poke.server.routing;

import eye.Comm.Response;
import poke.client.ClientListener;

/*
 * A listener to be used in server-server communication
 * 
 * @author virajh
 */

public class ServerListener implements ClientListener{

	private String id;
	
	public ServerListener(String id)
	{
		this.id = id;
	}
	@Override
	public String getListenerID() {
		return id;
	}

	@Override
	public void onMessage(Response msg) {
		System.out.println("Inside ServerListener");
		//System.out.println(msg.getBody()); //.getHeader().getOriginator()+" says to "+msg.getHeader().getToNode()+ " : "+msg.getHeader().getReplyCode().toString());

	}

}
