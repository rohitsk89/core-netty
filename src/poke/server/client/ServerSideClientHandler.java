

/*
 * Team Insane:
 * This is the Server client handler. It received messages from servers and returns them to callers
 */
package poke.server.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;


public class ServerSideClientHandler  extends SimpleChannelUpstreamHandler {
	protected static Logger logger = LoggerFactory.getLogger("ServerSideClientHandler");
	protected ConcurrentMap<String, ServerSideClientListner> listeners = new ConcurrentHashMap<String, ServerSideClientListner>();
	protected ConcurrentMap<String, Channel> returnChannels = new ConcurrentHashMap<String, Channel>();
	private volatile Channel channel;
	private volatile Channel srcchannel;
	public ServerSideClientHandler() {
	}

	public boolean send(GeneratedMessage msg, Channel source) {
		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		eye.Comm.Request request = (eye.Comm.Request)msg;
		String docName = request.getBody().getDoc().getDocName();
		String namespace = request.getBody().getSpace().getName();
			
		String key = namespace + ":" + docName ;
		logger.info("Source Channel added to returnChannels!");
		//returnChannels.put(key, source);
		if (channel != null && channel.isOpen() && channel.isWritable()) {
			logger.info("Channel is open to write");
			ChannelFuture cf = channel.write(msg);
			if (cf.isDone() && !cf.isSuccess()) {
				logger.error("failed to write!");
				return false;
			}
			else
				logger.info("ChannelFuture says success!");
		}
		else
			logger.info("Channel is null or closed!");
		this.srcchannel = source;
		return true;
	}
	
	public void handleMessage(eye.Comm.Response Response){
		logger.info("handleMessage " + Response.getHeader().toString());
		
		logger.info("Trying to return the response");
		if (srcchannel != null && srcchannel.isOpen() && srcchannel.isWritable()) {
			logger.info("Channel is open to write");
			ChannelFuture cf = srcchannel.write(Response);
			if (cf.isDone() && !cf.isSuccess()) {
				logger.error("failed to write!");
			}
			else
				logger.info("Returned server to server reply!");
		}
		else
			logger.info("Channel is closed!");
	}

	public void addListener(ServerSideClientListner listener) {
		if (listener == null)
			return;
		logger.info("In addlistner");
		listeners.putIfAbsent(listener.getListenerID(), listener);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		channel = e.getChannel();
		super.channelOpen(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (channel.isConnected())
			channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (e.getState() == ChannelState.INTEREST_OPS && ((Integer) e.getValue() == Channel.OP_WRITE)
				|| (Integer) e.getValue() == Channel.OP_READ_WRITE)
			logger.warn("channel is not writable! <--------------------------------------------");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		handleMessage((eye.Comm.Response)e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error("Handler exception, closing channel", e);
		
		// TODO do we really want to do this? try to re-connect?
		e.getChannel().close();
	}
}
