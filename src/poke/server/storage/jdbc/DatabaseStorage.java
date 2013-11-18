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
package poke.server.storage.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.storage.Storage;

import com.google.protobuf.ByteString;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import eye.Comm.Document;
import eye.Comm.NameSpace;

public class DatabaseStorage implements Storage {
	protected static Logger logger = LoggerFactory.getLogger("database");

	public static final String sDriver = "jdbc.driver";
	public static final String sUrl = "jdbc.url";
	public static final String sUser = "jdbc.user";
	public static final String sPass = "jdbc.password";

	protected Properties cfg;
	protected BoneCP cpool;

	protected DatabaseStorage() {
	}

	public DatabaseStorage(Properties cfg) {
		System.out.println("In DatabaseStorage()");
		init(cfg);
	}

	@Override
	public void init(Properties cfg) {
		if (cpool != null)
			return;

		this.cfg = cfg;

		try {
			Class.forName(cfg.getProperty(sDriver));
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(cfg.getProperty(sUrl));
			config.setUsername(cfg.getProperty(sUser, "sa"));
			config.setPassword(cfg.getProperty(sPass, ""));
			config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(10);
			config.setPartitionCount(1);

			cpool = new BoneCP(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gash.jdbc.repo.Repository#release()
	 */
	@Override
	public void release() {
		if (cpool == null)
			return;

		cpool.shutdown();
		cpool = null;
	}

	@Override
	public NameSpace getNameSpaceInfo(long spaceId) {
		NameSpace space = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to retrieve through JDBC/SQL
			// select * from space where id = spaceId
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on looking up space " + spaceId, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	@Override
	public List<NameSpace> findNameSpaces(NameSpace criteria) {
		List<NameSpace> list = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to search through JDBC/SQL
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on find", ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	@Override
	public NameSpace createNameSpace(NameSpace space) {
		if (space == null)
			return space;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to use JDBC
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on creating space " + space, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	@Override
	public boolean removeNameSpace(long spaceId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addDocument(String namespace, Document doc) {
		// TODO Auto-generated method stub

		Connection conn = null;
		try{
			conn = cpool.getConnection();
			if(conn==null)
				//System.out.println();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			PreparedStatement pst = null;
			//System.out.println("namespace" +namespace);
		    String stm = "INSERT INTO fileedge(\"filename\", \"namespace\",\"chunkcontent\",\"chunkid\", \"totalchunk\") VALUES ('"+doc.getDocName()+"', '"+namespace+"','"+doc.getChunkContent().toByteArray()+"',"+doc.getChunkId()+", "+doc.getTotalChunk()+");";
		    System.out.println("File recieved: "+doc.getDocName());
		    System.out.println(new String(doc.getChunkContent().toByteArray()));
		    
			pst = conn.prepareStatement(stm);
			int x = pst.executeUpdate();
			if(x==1)
			{
				return true;
			}
			else{
				return false;
			}
		}catch(Exception e){
			//System.out.println("Exception in PreparedStatement block");
			e.printStackTrace();
			try{
				conn.rollback();
				return false;
			}catch(SQLException s){
				//System.out.println("SQLException thrown !");
				e.printStackTrace();
				return false;
			}	
		}finally {
			if(conn!=null){
				try{
					conn.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	
	@Override
	public boolean updateDocument(String namespace, Document doc) {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Document> findDocuments(String namespace, Document criteria) {
		// TODO Auto-generated method stub
		
		Connection conn = null;
			
				
				try{
					conn = cpool.getConnection();
					System.out.println(conn.isClosed());
					conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
					PreparedStatement pst1 = null;
					ResultSet rs1 = null;
					//System.out.println("-----------------------find-------------------------------------------");			
				
				java.sql.Statement stm1=conn.createStatement();
					rs1= stm1.executeQuery("SELECT * FROM fileedge WHERE filename = '"+criteria.getDocName()+"';");
					
				
					//System.out.println("result "+rs1); 
				if(!rs1.next())
					{
					 	System.out.println("no file in database");
					//if(criteria.getDocName()== rs1.next())
							
					}
				else
					{
					String fn= rs1.getString(1);
					System.out.println("File " + fn+ " is there is database");
					}
				}catch(Exception e){
					System.out.println("Exception in PreparedStatement block");
					e.printStackTrace();
					try{
						conn.rollback();
					}catch(SQLException s){
						System.out.println("SQLException thrown !");
					}	
				}finally {
					if(conn!=null){
						try{
							conn.close();
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
				


	
		
		return null;
	}

	@Override
	public boolean removeDocument(String namespace, Document docId) {
		// TODO Auto-generated method stub
			Connection conn = null;
					
						
						try{
							conn = cpool.getConnection();
							System.out.println(conn.isClosed());
							conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
							PreparedStatement pst1 = null;
							ResultSet rs1 = null;
				//System.out.println("-----------------------remove-------------------------------------------");			
							System.out.println("name space: "+ namespace);
							System.out.println("chunk content: " + docId.getChunkContent().toString());
							System.out.println("docname: " +docId.getDocName());
							
							String stm1 = "DELETE FROM fileedge WHERE filename = '"+docId.getDocName()+"';";
							pst1 = conn.prepareStatement(stm1);
						if(pst1.executeUpdate()!=0){
							//deleted
							System.out.println("deleted------!!!!!!!111");
						}
						else{
							//not deleted
							System.out.println("not removed------!!!!!!!111");
						}
					
						}catch(Exception e){
							System.out.println("Exception in PreparedStatement block");
							e.printStackTrace();
							try{
								conn.rollback();
							}catch(SQLException s){
								System.out.println("SQLException thrown !");
							}	
						}finally {
							if(conn!=null){
								try{
									conn.close();
								}catch(Exception e){
									e.printStackTrace();
								}
							}
						}
	 return false;
	}

}