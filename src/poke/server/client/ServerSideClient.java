/*
 * Team Insane:
 * Primary purpose of this class is to be create node to communicate with another server
 * 
 * This class is create the connection to help suppot server to server communication.
 */
package poke.server.client;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.Request;
import eye.Comm.Response;

public class ServerSideClient {

	protected static Logger logger = LoggerFactory.getLogger("ServerSideClient");

	private String host;
	private int port;
	private String nodeId;
	private ChannelFuture channel; // do not use directly call connect()!
	private Channel srcChannel;
	private ClientBootstrap bootstrap;
	SSCDecoderPipeline clientPipeline;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker worker;

	protected ServerSideClient(String nodeId, String host, int port) {
		this.host = host;
		this.port = port;
		this.nodeId = nodeId;
		init();
	}

	/**
	 * release all resources
	 */
	public void release() {
		bootstrap.releaseExternalResources();
	}

	public Channel getChannel()
	{
		return srcChannel;
	}
	
	public String getNodeId()
	{
		return nodeId;
	}
	
	public static ServerSideClient initConnection(String nodeId, String host, int port) {

		ServerSideClient rtn = new ServerSideClient(nodeId, host, port);
		return rtn;
	}

	/**
	 * add an application-level listener to receive messages from the server (as
	 * in replies to requests).
	 * 
	 * @param listener
	 */
	public void addListener(ServerSideClientListner listener) {
		try {
			if (clientPipeline != null)
				clientPipeline.addListener(listener);
		} catch (Exception e) {
			logger.error("failed to add listener", e);
		}
	}
	
	public void  forwardRequest(Request request, Channel source){
		Response reply = null;
		try {
			this.srcChannel = source;
			// enqueue message
			outbound.put(request);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
	}
	
	public void poke(String toNode,String tag, int num) {
		// data to send
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);
		f.setNumber(num);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setFinger(f.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag("test finger");
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.FINGER);
		h.setToNode(toNode);
		r.setHeader(h.build());

		eye.Comm.Request req = r.build();

		try {
			// enqueue message
			//System.out.println("Req -> " + req);
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}

	private void init() {
		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		clientPipeline = new SSCDecoderPipeline();
		bootstrap.setPipelineFactory(clientPipeline);

		// start outbound message processor
		worker = new OutboundWorker(this);
		worker.start();
		System.out.println("Client connection queue up!");
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			// System.out.println("---> connecting");
			channel = bootstrap.connect(new InetSocketAddress(host, port));

			// cleanup on lost connection

		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess())
			return channel.getChannel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}

	/**
	 * queues outgoing messages - this provides surge protection if the client
	 * creates large numbers of messages.
	 * 
	 * @author gash
	 * 
	 */
	protected class OutboundWorker extends Thread {
		ServerSideClient conn;
		boolean forever = true;

		public OutboundWorker(ServerSideClient conn) {
			this.conn = conn;

			if (conn.outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel ch = conn.connect();
			if (ch == null || !ch.isOpen()) {
				ServerSideClient.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && conn.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = conn.outbound.take();
					
					if (ch.isWritable()) {
						ServerSideClientHandler handler = conn.connect().getPipeline().get(ServerSideClientHandler.class);
						
						if (!handler.send(msg,conn.srcChannel))
							conn.outbound.putFirst(msg);

					} else
						conn.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					ServerSideClient.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				ServerSideClient.logger.info("connection queue closing");
			}
		}
	}

}
