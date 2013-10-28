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

import poke.server.resources.Resource;
import eye.Comm.Document;
import eye.Comm.Request;
import eye.Comm.Response;

public class DocumentResource implements Resource {

	@Override
	public Response process(Request request) {
		
		int action = request.getHeader().getRoutingId().getNumber();
		Response res = null;
		
		switch(action) {
		
		case 20:
			System.out.println("DOCUMENT UPLOAD");
			docAdd(request);
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
//        DOCADD = 20;
//        DOCFIND = 21;
//        DOCUPDATE = 22;
//        DOCREMOVE = 23;
//        DOCADDHANDSHAKE = 24;

		return res;
	}
	
	private void docAdd(Request request)
	{
		Document doc = request.getBody().getDoc();
		System.out.println(new String(doc.getChunkContent().toByteArray()));
		File file;
	}

}
