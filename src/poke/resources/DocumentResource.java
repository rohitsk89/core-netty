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
import java.io.FileWriter;
import java.io.IOException;

import java.sql.SQLException;
import java.util.*;

import com.google.protobuf.ByteString;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;



import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.Storage;
import poke.server.storage.jdbc.DatabaseStorage;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;

public class DocumentResource implements Resource {

//"---------------------------------------------------------------------------------------------------"
public static final String sDriver = "org.postgresql.Driver";
public static final String sUrl = "jdbc:postgresql://localhost:5432/jerry";
public static String sUser="jdbc.user";
public static String sPass="jdbc.password";
private Storage store;
protected BoneCP cpool;
	BoneCPConfig config = new BoneCPConfig();
	Properties properties = new Properties();
//"---------------------------------------------------------------------------------------------------"

	// Team insane start

	//fixed place to save files
	private static final String savePath="/home/virajh/workspace/275/saved";
	
	@Override
	public Response process(Request request) {

		int action = request.getHeader().getRoutingId().getNumber();
		System.out.println("ACTION ---> " + action);
		Response res = null;
		
		switch(action) {
		
		case 20:
			System.out.println("DOCUMENT UPLOAD");
			res = docAdd(request);
			break;
		case 21:
			System.out.println("DOCUMENT FIND");
			res = docFind(request);
			break;
		case 22:
			System.out.println("DOCUMENT UPDATE");
			break;
		case 23:
			System.out.println("DOCUMENT REMOVE");
			res = docRemove(request);
			break;
		case 24:
			System.out.println("DOCUMENT HANDSHAKE");
			break;
		case 25:
			System.out.println("DOCUMENT QUERY");
			break;
		}
		
		if(res==null)
		{
			Response.Builder rb = Response.newBuilder();
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.FAILURE, "Unsupported operation."));
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
	// Team insane start - To save document into database.
	public Response docAdd(Request request) 
	{
		Document doc = request.getBody().getDoc();
		String namespace = request.getBody().getSpace().getName();
		DatabaseStorage dbs = null;
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

			if(flag){
				Response.Builder rb = Response.newBuilder();

				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File saved succesfully."));

				// payload --> empty
				PayloadReply.Builder pb = PayloadReply.newBuilder();
				rb.setBody(pb.build());

				return rb.build();
			}
			else{
				Response.Builder rb = Response.newBuilder();
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.FAILURE, "Operation failed."));
				PayloadReply.Builder pb = PayloadReply.newBuilder();
				rb.setBody(pb.build());
				return rb.build();
			}
	}
	
	// Team insane start -- function to set postgreSQL database properties
	public Properties setProperties() throws ClassNotFoundException, SQLException{
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
		
//		cpool = new BoneCP(config);
		System.out.println("Database Properties are set");
		return properties;
	}

	// Team insane start -- function to find document in file system.
	public Response docFind(Request request) {
		Response response = null;
				
		String fileName = request.getBody().getDoc().getDocName();
		
		File file = new File(savePath);
		File[] allFiles = file.listFiles();
		try{
		if(allFiles!=null){
	        for (File fil : allFiles)
	        {
	            if (fileName.equals(fil.getName()))
	            {
	            	//System.out.println("Found the file");
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
	    			d.setTotalChunk(6);
	            	d.setDocName(fil.getName());
	            	d.setId(1);
	        		// metadata
	        		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));
	        		
	        		// payload
	        		PayloadReply.Builder pb = PayloadReply.newBuilder();
	        		Finger.Builder fb = Finger.newBuilder();
	        		fb.setTag(request.getBody().getFinger().getTag());
	        		fb.setNumber(request.getBody().getFinger().getNumber());
	        		pb.setFinger(fb.build());
	        		pb.addDocs(d); //Document
	        		rb.setBody(pb.build());
	        		

	        		response = rb.build();
	        		try{
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
					}
	            }
	        }
		}
		else{
			System.out.println("NOT FOUND IN FOLDER");
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
		// code to search file locally
		
			
		return response;
	}

	// Team insane start  -- function to remove document.
	private Response docRemove(Request request)
	{
		System.out.println(request.getBody().getDoc().getDocName()+" deleted.");
		
		Response.Builder rb = Response.newBuilder();

		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, "File deleted succesfully."));
		
		// payload --> empty
		PayloadReply.Builder pb = PayloadReply.newBuilder();
		rb.setBody(pb.build());
		
		Response reply = rb.build();
		
		try{

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
		
		return reply;
	}
}