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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.stream.events.Namespace;

import org.jboss.netty.channel.Channel;
import java.sql.SQLException;
import java.util.*;
import com.google.protobuf.ByteString;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import poke.server.client.ForwardRequestManager;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.Storage;
import poke.server.storage.jdbc.DatabaseStorage;
import eye.Comm;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;

public class DocumentResource implements Resource {
	//"---------------------------------------------------------------------------------------------------"
	// Team Insane
	//File server location to save file
	private static final String savePath="./saved";
	private static boolean bReplicate=true; // set to true to replicate by default

	/*Enable for Database access
	 * public static final String sDriver = "org.postgresql.Driver";
	public static final String sUrl = "jdbc:postgresql://localhost:5432/jerry";
	public static String sUser="jdbc.user";
	public static String sPass="jdbc.password";
	private Storage store;
	protected BoneCP cpool;
	BoneCPConfig config = new BoneCPConfig();
	Properties properties = new Properties();*/
	//"---------------------------------------------------------------------------------------------------"


	public void turnReplicationOn()
	{bReplicate=true;}
	
	public void turnReplicationOff()
	{bReplicate=false;}
	
	@Override
	public Response process(Request request, Channel channel) {
		//Team Insane
		int action = request.getHeader().getRoutingId().getNumber();
		System.out.println("ACTION ---> " + action);
		Response reply = null;
		
		switch(action) {
		
		case 20:
			System.out.println("DOCUMENT UPLOAD");
			reply = docAdd(request);
			break;
		case 21:
			System.out.println("DOCUMENT FIND");
			reply = docFind(request);
			break;
		case 22:
			System.out.println("DOCUMENT UPDATE");
			break;
		case 23:
			System.out.println("DOCUMENT REMOVE");
			reply = docRemove(request);
			break;
		case 24:
			System.out.println("DOCUMENT HANDSHAKE");
			break;
		}
		
		if(reply==null)
		{
			Response.Builder rb = Response.newBuilder();
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.MISSINGARG, "Unknown operation."));
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			rb.setBody(pb.build());
			reply = rb.build();
			
		}
		
		return reply;
//        DOCADD = 20;
//        DOCFIND = 21;
//        DOCUPDATE = 22;
//        DOCREMOVE = 23;
//        DOCADDHANDSHAKE = 24;		
	}
	
	private Response docAdd(Request request) 
	{ 
		Document doc = request.getBody().getDoc();
		String namespace = request.getBody().getSpace().getName();
		
		/* Switch this on to use Database
		 * DatabaseStorage dbs = null;
		try {
				dbs = new DatabaseStorage(setProperties());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("in doc resource");
			boolean flag = dbs.addDocument(namespace, doc);
		*/
		String path=savePath + File.separator; 
		if(namespace.length() > 0)
			path =  path + namespace + File.separator + doc.getDocName();
		else
			path = path + doc.getDocName();
		
		FileOutputStream fos;
		File file = new File(path);
		// ensure that we have the folders created
		file.getParentFile().mkdirs();
		
		
		try {
			Response.Builder rb = Response.newBuilder();
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			Response reply =null;
			long totalChunks = doc.getTotalChunk();
			long fileSize = doc.getDocSize();
			// add safety check to ensure that this file can be locally stored.
			long size = new File("/").getFreeSpace();
			if(fileSize >=  size) {
				System.out.println("File size is: " + fileSize + " Disk size is: " + size);
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.FAILURE, "File size too large."));
			}
			else{
				fos = new FileOutputStream(file, true);
				fos.write(doc.getChunkContent().toByteArray());
				fos.flush();
				fos.close();
				
				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File saved succesfully."));
			}
			
			if(bReplicate == true)
			{
				ForwardRequestManager.init().broadcastRequest(request);
			}
			// payload --> empty
			rb.setBody(pb.build());
			reply = rb.build();
			System.out.println("Reply returned from server.");
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
	
	private Response docFind(Request request){
		Response response = null;
				
		String fileName = request.getBody().getDoc().getDocName();
		String namespace = request.getBody().getSpace().getName();
		System.out.println("----- Namespace ------" + namespace);
		
		String path=savePath ; 
		if(namespace.length() > 0)
			path =  path + File.separator + namespace;
		
		File file = new File(path);
		File[] allFiles = file.listFiles();
		try{
		if(allFiles!=null){
	        for (File fil : allFiles)
	        {
	            if (fileName.equals(fil.getName()))
	            {
	            	System.out.println("Found the file");
	            	Response.Builder rb = Response.newBuilder();
	            	byte[] data = new byte[65000];
	            	FileInputStream fileInputStream = new FileInputStream(fil);
	                fileInputStream.read(data);
	            	//document
	            	
	            	Document.Builder d = eye.Comm.Document.newBuilder();
	    			d.setChunkContent(ByteString.copyFrom(data));
	    			d.setChunkId(001);
	    			//d.setDocName(filepath);
	    			d.setDocSize(1);
	    			d.setTotalChunk(1);
	            	d.setDocName(fil.getName());
	        		// metadata
	        		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "Found the file"));
	        		
	        		// payload
	        		PayloadReply.Builder pb = PayloadReply.newBuilder();
	        		Finger.Builder fb = Finger.newBuilder();
	        		fb.setTag(request.getBody().getFinger().getTag());
	        		fb.setNumber(request.getBody().getFinger().getNumber());
	        		pb.setFinger(fb.build());
	        		pb.addDocs(d); //Document
	        		rb.setBody(pb.build());
	        		

	        		response = rb.build();
	        		/*Team Insane: Turn on to use Database.
	        		 * try{
		        		//"---------------------------------------------------------------------------------------------------"
		    			//Properties property=setProperties();
		    			DatabaseStorage dbs1 = new DatabaseStorage(setProperties());
		    			//System.out.println("in doc find");
		    		//	DatabaseStorage dbs = new DatabaseStorage(setProperties());
		    			List<Document> docs = dbs1.findDocuments(savePath, request.getBody().getDoc());
		    			//return reply;
		    //"---------------------------------------------------------------------------------------------------"			
		        		}
		        		catch(SQLException e)
		        		{
		        			System.out.println("error in properties");
		        		} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
	            }
	        }
		}
		else{
			System.out.println("NOT FOUND IN FOLDER");
//			ClientConnection cc = ClientConnection.initConnection("localhost", 5571);
//			cc.forwardRequest(request);
		}
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
		
		
		return response;
	}

	// Team insane start -- function to set postgreSQL database properties
		/*Uncomment for DB access
		 * public Properties setProperties() throws ClassNotFoundException, SQLException{
			Class.forName(sDriver);
			
			config.setPassword("mogli465");
			System.out.println("in properties password");		
			config.setUsername("tom");
			System.out.println("in properties username");
			config.setJdbcUrl(sUrl);
			System.out.println("in properties url");
			
			properties.setProperty("jdbc.driver",sDriver);
			
			properties.setProperty("jdbc.url",sUrl);
			properties.setProperty(sUser,"tom");
			properties.setProperty(sPass,"mogli465");
			
//			cpool = new BoneCP(config);
			System.out.println("Database Properties are set");
			return properties;
		}*/
		
	private Response docRemove(Request request)
	{	
		System.out.println(request.getBody().getDoc().getDocName()+" deleted.");
		
		Document doc = request.getBody().getDoc();
		String namespace = request.getBody().getSpace().getName();
		System.out.println("----- Namespace ------" + namespace);
		
		String path=savePath + File.separator; 
		if(namespace.length() > 0)
			path =  path + namespace + File.separator + doc.getDocName();
		else
			path = path + doc.getDocName();
		
		File file = new File(path);
		boolean res = file.delete();
		
		// ToDo: Should we remove the directory too?
		// if empty - dlete
		
		Response.Builder rb = Response.newBuilder();
		if(true == res)
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File deleted succesfully."));
		else
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File does not exist."));
		// payload --> empty
		PayloadReply.Builder pb = PayloadReply.newBuilder();
		rb.setBody(pb.build());
		
		Response reply = rb.build();
		/* Team Insane: Use this for database
		 * try{

			//Properties property=setProperties();
			DatabaseStorage dbs1 = new DatabaseStorage(setProperties());
			//System.out.println("in doc remove");
			//	DatabaseStorage dbs = new DatabaseStorage(setProperties());
			dbs1.removeDocument(savePath, request.getBody().getDoc());
			//return reply;
			
    		}
    		catch(SQLException e)
    		{
    			System.out.println("error in properties");
    		} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 */
		if(bReplicate == true)
		{
			ForwardRequestManager.init().broadcastRequest(request);
		}
		// ToDo:initiate deletion of replicated files 
		return reply;
	}
	
	
}
