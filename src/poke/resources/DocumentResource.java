/*
 * copyright 2012, gash
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
package poke.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import eye.Comm.Document;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;

public class DocumentResource implements Resource {

	//fixed place to save files
	private static final String savePath="./saved";
	
	@Override
	public Response process(Request request) {
		
		int action = request.getHeader().getRoutingId().getNumber();
		Response res = null;
		
		switch(action) {
		
		case 20:
			System.out.println("DOCUMENT UPLOAD");
			res = docAdd(request);
			break;
		case 21:
			System.out.println("DOCUMENT FIND");
			break;
		case 22:
			System.out.println("DOCUMENT UPDATE");
			break;
		case 23:
			System.out.println("DOCUMENT REMOVE");
			break;
		case 24:
			System.out.println("DOCUMENT HANDSHAKE");
			break;
		}
		
		if(res==null)
		{
			Response.Builder rb = Response.newBuilder();
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.MISSINGARG, "Unknown operation."));
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			rb.setBody(pb.build());
			Response reply = rb.build();
			return reply;
		}
		
		return res;
//        DOCADD = 20;
//        DOCFIND = 21;
//        DOCUPDATE = 22;
//        DOCREMOVE = 23;
//        DOCADDHANDSHAKE = 24;		
	}
	
	private Response docAdd(Request request)
	{
		Document doc = request.getBody().getDoc();
		File file = new File(savePath, doc.getDocName());
		//long totalChunks = doc.getTotalChunk();
		
		String chunk = new String(doc.getChunkContent().toByteArray());
		System.out.println(doc.getDocName()+"\n"+chunk);
		
		FileWriter fw;
		try {
			fw = new FileWriter(file);
			fw.write("File recieved from "+request.getHeader().getOriginator()+" on "+new Date());
			fw.write("\n");
			fw.write(chunk);
			fw.flush();
			fw.close();

			Response.Builder rb = Response.newBuilder();
			
			// metadata
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File saved succesfully."));
			
			// payload --> empty
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			rb.setBody(pb.build());

			Response reply = rb.build();
			return reply;
			
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			Response.Builder rb = Response.newBuilder();
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.FAILURE, "Operation failed."));
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			rb.setBody(pb.build());
			Response reply = rb.build();
			return reply;
		}
	}

}
