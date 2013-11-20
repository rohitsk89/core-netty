/*
 * copyright 2013, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.routing;


import java.util.List;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.CodedOutputStream;

import poke.client.ClientConnection;
import poke.server.client.ServerSideClient;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import eye.Comm.Finger;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;
import eye.Comm.Header.ReplyStatus;

/**
 * The forward resource is used by the ResourceFactory to send requests to a
 * destination that is not this server.
 * 
 * Strategies used by the Forward can include TTL (max hops), durable tracking,
 * endpoint hiding.
 * 
 * @author gash
 * 
 */
public class ForwardResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private ServerConf cfg;

	public ServerConf getCfg() {
		return cfg;
	}

	/**
	 * Set the server configuration information used to initialized the server.
	 * 
	 * @param cfg
	 */
	public void setCfg(ServerConf cfg) {
		this.cfg = cfg;
	}

	@Override
	public Response process(Request request, Channel channel) {
		// Team insane start  -- Forward the resource to next node in configuration file.
		System.out.println("Inside ForwardReso");
		
		String nextNode = determineNextEdge(request, request.getHeader().getToNode());
		//System.out.println(nextNode+"!!!");
		Response reply = null;
		if (nextNode != null) {
			Request fwd = ResourceUtil.buildForwardMessage(request, cfg);
			System.out.println("Next node -> " + nextNode);
			
			if (fwd==null)
			{
				Response.Builder rb = Response.newBuilder();
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "duplicate message"));
				PayloadReply.Builder pb = PayloadReply.newBuilder();
				rb.setBody(pb.build());
				reply = rb.build();
				return reply;
			}
			if(request.getHeader().hasRemainingHopCount())
			{
				long hopcnt = request.getHeader().getRemainingHopCount();
				if(hopcnt <= 0 )
				{
					Response.Builder rb = Response.newBuilder();
					rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.FAILURE, "Out of hops"));
					PayloadReply.Builder pb = PayloadReply.newBuilder();
					rb.setBody(pb.build());
					reply = rb.build();
					return reply;
				}
			}
			// enqueue message
			String hostname = cfg.getNearest().getNode(nextNode).getHost();
			int port = cfg.getNearest().getNode(nextNode).getPort();
			String nodeId = cfg.getNearest().getNode(nextNode).getNodeId();
			
			ServerSideClient cc = ServerSideClient.initConnection(nodeId, hostname, port);
			System.out.println("Forwarding request " + hostname + ":" + port);	
			cc.forwardRequest(fwd, channel);
			
			// TODO forward the request
		} else {
			reply = null;
			// cannot forward the message - no edge or already traveled known
			// edges

			// TODO should we just fail silently?
			System.out.println("no next node");
			Response.Builder rb = Response.newBuilder();
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			Finger.Builder fb = Finger.newBuilder();
			fb.setTag(request.getBody().getFinger().getTag());
			fb.setNumber(request.getBody().getFinger().getNumber());
			pb.setFinger(fb.build());
			rb.setBody(pb.build());

			reply = rb.build();
		}
		return reply;
	}

	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in
	 * its list.
	 * 
	 * @param request
	 * @return
	 */
	private String determineForwardNode(Request request) {
		//System.out.println("Inside determineForwardNode()");
		List<RoutingPath> paths = request.getHeader().getPathList();
		if (paths == null || paths.size() == 0) {
			
			if(cfg==null)
				System.out.println("cfg is null");
			//System.out.println("Is null ^ ?");
			
			// pick first nearest
			NodeDesc nd = cfg.getNearest().getNearestNodes().values().iterator().next();
			
			System.out.println(nd.getNodeId()+" <_<");
			//System.out.println("below.");
			return nd.getNodeId();
		} else {
			System.out.println("Inside else");
			// if this server has already seen this message return null
			for (RoutingPath rp : paths) {
				System.out.println("Inside Routing paths for " + rp.getNode());
				for (NodeDesc nd : cfg.getNearest().getNearestNodes().values()) {
					System.out.println(nd.getNodeId().toString());
					if (!nd.getNodeId().equalsIgnoreCase(rp.getNode()))
						return nd.getNodeId();
				}
			}
		}

		return null;
	}
	
	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in
	 * its list.
	 * 
	 * @param request
	 * @return
	 */
	private String determineNextEdge(Request request, String toNode) {
		for (NodeDesc nd : cfg.getNearest().getNearestNodes().values()) {
			System.out.println(nd.getNodeId().toString());
			if (nd.getNodeId().equalsIgnoreCase(toNode))
				return nd.getNodeId();
		}
		// check in external conf else
		// Give the next node
		return determineForwardNode(request);
	}
}
