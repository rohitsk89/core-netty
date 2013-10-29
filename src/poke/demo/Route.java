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
package poke.demo;

import poke.client.ClientConnection;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;

/**
 * This is a test to verify routing of messages through multiple servers
 * 
 * @author gash
 * 
 */
public class Route {
	private String tag;
	private int count;

	public Route(String tag) {
		this.tag = tag;
	}

	public void run() {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);

		for (int i = 0; i < 3; i++) {
			count++;
			cc.poke("zero", tag, count);
		}
	}

	public static void main(String[] args) {
		try {
			Route jab = new Route("jab");
			jab.run();

			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			Thread.sleep(10000);
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
