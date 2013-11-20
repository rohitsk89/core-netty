/*
 * Team Insane:
 * Primary purpose of this class is to be able to forward and receive message from neighbour nodes.
 * 
 * This class is has access to channels of neighbor node, in neighbourMap, and is able to directly communicate with them.
 * This class exposes the important function "broadcastRequest", This function simply takes the request and forwards it to all the 
 * neighbour nodes.
 */

package poke.server.client;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.Request;
import eye.Comm.Document;

public class ForwardRequestManager {

	private volatile static ConcurrentHashMap<String, ServerSideClient> neighbourMap = new ConcurrentHashMap<String, ServerSideClient>();
	private volatile static ConcurrentHashMap<String, ServerSideClient> OutboundMap = new ConcurrentHashMap<String, ServerSideClient>();
	//static private volatile ConcurrentHashMap<String, Request> broadCastQueue = new ConcurrentHashMap<String, Request>();
	protected static Logger logger = LoggerFactory.getLogger("ForwardRequestManager ");
	static private ServerConf svrConf=null;
	private List<Request> broadCastQueue = Collections.synchronizedList(new ArrayList<Request>());
	
	static boolean bDone;
	private boolean forever = true;
	private BroadcastThread FRThread=null;
	static private ForwardRequestManager fm=null;
	private static String SvrNodeID=""; 
	
	protected ForwardRequestManager()
	{
		FRThread = new BroadcastThread(this);
		FRThread.start();
		bDone=false;
	}
	
	public void broadcastRequest(Request req)
	{
		init();
		broadCastQueue.add(req);
		FRThread.interrupt();
	}
	
	public static ForwardRequestManager init()
	{
		try{
			if(false == bDone && svrConf != null )
			{
				fm = new ForwardRequestManager();
				SvrNodeID = svrConf.getServer().getProperty("node.id");
								
				// Team: only initialize first time. Can be improved in future to re check if there is a new server 
				// and initialize connection or reinitiate if it failed previously.
				for (NodeDesc nn : svrConf.getNearest().getNearestNodes().values()) {
					ServerSideClient neighbourNode = ServerSideClient.initConnection(nn.getNodeId(), nn.getHost(), nn.getPort());
					neighbourMap.put(nn.getNodeId(),neighbourNode);
					logger.info(" added neighbour node: " + nn.getNodeId() + ", " + nn.getHost() + ", " + nn.getPort());
				}
				bDone = true;
			}
		}
		catch(Exception e)
		{
			logger.error("Exception in addActiveNode: " + e.getMessage());
		}
		return fm;
	}

	private ServerSideClient getNeighbourNode(String nodeId)
	{
		return neighbourMap.get(nodeId);
		
	}
	
	public static void setConf(ServerConf conf)
	{
		svrConf = conf;
	}
	
	public static void addOutboundNode(ServerConf conf)
	{
		svrConf = conf;
	}
	
	public ServerConf getConf()
	{
		return svrConf;
	}
	
	public void Shutdown()
	{
		for (ServerSideClient obj : neighbourMap.values())	{
			obj.release();
		}
		
		neighbourMap.clear();
	}
	
	protected class BroadcastThread extends Thread {
		
		ForwardRequestManager fwdReqMgr;
		public BroadcastThread(ForwardRequestManager fwdReqMgr)
		{
			this.fwdReqMgr = fwdReqMgr;
		}
		
		@Override
		public void run() {
			logger.info("Forward REquest Manager thread is started.");
			while (forever) 
			{
				try	{
					logger.info("Trying to send request.");
					if (fwdReqMgr.broadCastQueue.size() != 0) {
						logger.info("There are " + fwdReqMgr.broadCastQueue.size() + " requests to broadcast");
							
						for (Request rq : fwdReqMgr.broadCastQueue) {	// for each request
							for (ServerSideClient obj : fwdReqMgr.neighbourMap.values()) // broadcast request
							{		// Just to simulate different machines
								logger.info("Create a new request");
								Document doc = rq.getBody().getDoc();
								Document.Builder d = eye.Comm.Document.newBuilder();
								
								d.setChunkContent(doc.getChunkContent());
								d.setChunkId(doc.getChunkId());
								d.setDocSize(doc.getDocSize());
								d.setTotalChunk(doc.getTotalChunk());
								d.setDocName(doc.getDocName());
								
								// payload containing data
								Request.Builder r = Request.newBuilder();
								eye.Comm.Payload.Builder p = Payload.newBuilder();
								p.setSpace(NameSpace.newBuilder().setName(rq.getBody().getSpace().getName()));
								p.setDoc(d.build());
								r.setBody(p.build());

								// header with routing info
								eye.Comm.Header.Builder h = Header.newBuilder();
								h.setOriginator(SvrNodeID); // Put current server

								h.setTime(System.currentTimeMillis());
								h.setRoutingId(rq.getHeader().getRoutingId());
								h.setToNode(obj.getNodeId());
								r.setHeader(h.build());
						    	obj.forwardRequest(r.build(), obj.getChannel());
							}
							//fwdReqMgr.broadCastQueue.remove(rq); // remove the object once done ToDO: Fix this as this is better
						}
					} else
						logger.info("No Requests to broadcast");	
					fwdReqMgr.broadCastQueue.clear();
					Thread.sleep(6000); // sleep till interrupted, -1 should do the trip 
				} catch (InterruptedException e) {
					logger.info("Time to wake up");
				}
				
			}
			logger.info("ending hbMgr connection monitoring thread");
		}
	}
}