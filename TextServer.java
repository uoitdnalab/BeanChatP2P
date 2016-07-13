import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
//import org.json.simple.JSONObject;
import org.json.simple.*;

public class TextServer
{
	//public static JSONObject routing_table;
	/* The routing_table is a JSONObject which is able to translate
	 * node ID names into local IP addresses. For example, the routing_table
	 * on NODE B has an entry stored for NODE A which may be different from
	 * the entry stored for NODE A on NODE C
	 */
	public static RoutingTable routing_table;
	 
	public static JSONArray neighbour_list;
	 /* A list of nodes which this node is directly connected to.
	  * This is used by nodes who will map a local portion of the
	  * network
	  */
	  
	public static JSONObject full_routes;
	  /* Contains full routes (list of nodes which packet must pass
	   * through in order to reach a destination
	   */

	  
	public static String node_id; // The node ID of this server
	 
	
	public static void main(String args[])
	{

		node_id = args[0];
		System.out.println("Starting server...");
		//routing_table = new JSONObject();
		routing_table = new RoutingTable();
		System.out.println("Created a blank routing table");
		//Add our localhost to the routing table
		routing_table.add_entry(node_id,"127.0.0.1");
		neighbour_list = new JSONArray();
		System.out.println("Created a blank neighbour list");
		full_routes = new JSONObject();
		System.out.println("Created a blank list of complete routes");
		

		System.out.println("This server is running under node id: " + node_id);
		
		//Now connect to the IPs given on the command line and signal our presence on the network
		for (int i=1; i<args.length; i++)
		{
			System.out.println("Connecting to " + args[i]);
			//actually do the connection
			NetworkAdvertise net_ad = new NetworkAdvertise();
			net_ad.do_advertise(node_id,args[i]);
		}
		
		Boolean has_packet_from_self = false;
		/* Indicates if we have data for the node itself. This data
		 * would have originated from a 'multi-hop' packet with our
		 * destination address
		 */
		 
		 Boolean next_reply_multihop = false;
		 /* Indicates that a reply should be wrapped in a multihop
		  * packet and sent back to the destination. For example, if
		  * this node recieves a 'get-neighbours' packet which was
		  * multihopped over "A B C D E F". The code block which creates
		  * replys to 'get-neighbours' querys will know to wrap its reply
		  * in a multihop packet with route "F E D C B A".
		  */
		
		
		// declaration section:
		// declare a server socket and a client socket for the server
		// declare an input and an output stream
		
		ServerSocket textServer = null;
        String line = null;
        DataInputStream is = null;
		PrintStream os = null;
        Socket clientSocket = null;
		// Try to open a server socket on port 9999
		// Note that we can't choose a port less than 1023 if we are not
		// privileged users (root)
		try
		{
			textServer = new ServerSocket(9999);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}   
		// Create a socket object from the ServerSocket to listen and accept 
		// connections.
		// Open input and output streams
		try
		{
			//clientSocket = textServer.accept();
			//is = new DataInputStream(clientSocket.getInputStream()); //Moved this part further down
			//os = new PrintStream(clientSocket.getOutputStream());
			//Work with the data that we have recieved.
			JSONObject packet_object = null;
			while (true)
			{
				if (has_packet_from_self)
				{
					System.out.println("I'm reading a packet from myself");
					has_packet_from_self = false;
				}
				else
				{
					//Don't wait for a client if we have data from ourselves
					clientSocket = textServer.accept();
					is = new DataInputStream(clientSocket.getInputStream());
					os = new PrintStream(clientSocket.getOutputStream());
					line = is.readLine();
					Object obj=JSONValue.parse(line);
					packet_object=(JSONObject)obj;
				}
				//os.println("Ok"); // Acknowledge the data packet
				if (line != null)
				{
					os.println("Ok"); // Acknowledge the data packet
					//DEBUG
					System.out.println("");
					System.out.println("");
					System.out.println("");
					System.out.println("--- BEGIN RECIEVED PACKET ---");
					System.out.println(line);
					System.out.println("--- END RECIEVED PACKET ---");
					//Decoding the JSON packet
					//Object obj=JSONValue.parse(line);
					//JSONObject packet_object=(JSONObject)obj;
					String payload;
					try
					{
						payload = (String)packet_object.get("payload");
					}
					catch (ClassCastException e)
					{
						System.out.println("This is a multi-hop packet");
						payload = "multi-hop packet inside";
					}
					catch (NullPointerException e)
					{
						System.out.println("Bad packet");
						// Don't waste time attempting any further decoding
						continue;
					}
					
					//TODO: Validate this
					String packet_source = (String)packet_object.get("src");
					String destination = (String)packet_object.get("dst");
					String packet_type = (String)packet_object.get("type");
					
					
					//DEBUG
					System.out.println("-------- BEGIN PACKET ANALYSIS --------");
					System.out.println("--- BEGIN PAYLOAD ---");
					System.out.println(payload);
					System.out.println("--- END PAYLOAD ---");
					System.out.println("--- BEGIN DESTINATION ---");
					System.out.println(destination);
					System.out.println("--- END DESTINATION ---");
					System.out.println("--- BEGIN SOURCE ---");
					System.out.println(packet_source);
					System.out.println("--- END SOURCE ---");
					System.out.println("--- BEGIN TYPE ---");
					System.out.println(packet_type);
					System.out.println("--- END TYPE ---");
					System.out.println("-------- END PACKET ANALYSIS --------");
					System.out.println("");
					
					// Deal with the packet that we recieved
					// For many functions we will need to know which node sent us the packet
					InetAddress client_ip = clientSocket.getInetAddress();
					String client_ip_string = client_ip.toString().substring(1);
					
					// 'type' is a mandatory field
					if (packet_type == null)
					{
						System.out.println("Bad packet. Type not specified");
					}
					
					
					/* 'text-message' packet. This is a text message which
					 * may be for us or we might need to
					 * forward it closer to it's destination
					 */
					else if (packet_type.equals("text-message"))
					{
						//Stop immediately if destination is not specified
						if (destination == null)
						{
							System.out.println("Bad packet. Destination must be specified for 'text-message'");
							continue;
						}
						//DEBUG
						System.out.println("This message may or may not be for you");
						System.out.println(" --- BEGIN MESSAGE ---");
						System.out.println(payload);
						System.out.println(" --- END MESSAGE ---");
						//Forward the message appropriately - but not if it has already arrived
						if (destination.equals(node_id))
						{
							System.out.println("------ WE HAVE A MESSAGE FOR US ------");
							System.out.println(payload);
							System.out.println("------ END OF OUR MESSAGE ------");
							//sendToWeb(packet_object);
							//Try to send this message to the socket running on the local machine
							//sendToMail(packet_object);
							//Writer writer = null;
							//try 
							//{
								//writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("our_messages"), "utf-8"));
								//writer.write("New message from: " + packet_source);
								//writer.write("--- BEGIN MESSAGE ---");
								//writer.write(payload);
								//writer.write("--- END MESSAGE ---");

							//} 
							//catch (IOException ex) 
							//{
								//// report
							//}finally 
							//{
								//try 
								//{
									//writer.close();
								//}
								//catch (Exception ex)
								//{
								//}
							//}
						}
						else
						{
							//forwardPacket(packet_object);
							ForwardMessageThread forward_packet_thread = new ForwardMessageThread(routing_table,packet_object);
							forward_packet_thread.start();
						}
					}
					
					/* 'net-advertise' packet. This packet tells a node that
					 * it has a new neighbour and then adds a route to it to it's routing
					 * table
					 */
					else if (packet_type.equals("net-advertise"))
					{

						//DEBUG
						System.out.println("Recieved a 'net-advertise' packet");
						System.out.println("Node: " + packet_source + " @" + client_ip_string + " wants to be added to the routing table");
						// TODO: figure out if this is allowed or not
						//routing_table.put(packet_source,new String(client_ip_string));
						
						if (packet_object.get("port") != null)
						{
							/* A port has been specified. All communication sent to this
							 * node should be sent over this specified port instead of
							 * port 9999.
							 */
							//int tmp_port = Integer.parseInt((String)packet_object.get("port"));
							try
							{
								int tmp_port = Integer.parseInt((String)packet_object.get("port"));
								if (tmp_port > 0 && tmp_port < 65535)
								{
									// Acceptable port, now register it
									routing_table.map_port((String)packet_object.get("src"),tmp_port);
								}
							}
							catch (ClassCastException e)
							{
								System.out.println("Port option was not formatted properly.");
								System.out.println("Disregarding bad 'net-advertise' packet");
								continue;							
							}
						}
						System.out.println("Assuming that everything's okay. Adding now.");
						routing_table.add_entry(packet_source,new String(client_ip_string));
						if (neighbour_list.contains(packet_source))
						{
							System.out.println("I already know about this node");
						}
						else
						{
							System.out.println("Also adding this node to the neighbour list.");
							neighbour_list.add(packet_source);
						}
						System.out.println(" --- BEGIN ROUTING TABLE --- ");
						System.out.println(routing_table.routing_table);
						System.out.println(" --- END ROUTING TABLE --- ");
						System.out.println(" --- BEGIN NEIGHBOURS LIST ---");
						System.out.println(neighbour_list);
						System.out.println(" --- END NEIGHBOURS LIST ---");
						
					}
					
					/* 'get-neighbours' packet. Returns to the sender which nodes are
					 * directly connected to it.
					 */
					else if (packet_type.equals("get-neighbours"))
					{
						System.out.println("Sending list of neighbours to " + client_ip_string);
						//First make a packet with the response in the payload
						JSONObject response_packet = new JSONObject();
						//Set the fields
						response_packet.put("payload",neighbour_list);
						response_packet.put("dst",new String(packet_source));
						response_packet.put("type",new String("neighbour-list"));
						response_packet.put("src",node_id);
						//TODO: set all the fields
						//forwardPacket(response_packet);
						
						//If the packet was sent in a multihop packet, reply in the same way.
						if (routing_table.lookup_entry(packet_source) == null)
						{
							//DEBUG
							System.out.println("Replying in a multi-hop packet");
							
							//next_reply_multihop = false;
							JSONObject m_hop_reply = new JSONObject();
							//Set the fields
							m_hop_reply.put("payload", response_packet);
							m_hop_reply.put("type",new String("multi-hop"));
							m_hop_reply.put("routing_path",full_routes.get(packet_source));
							m_hop_reply.put("src",node_id);
							m_hop_reply.put("dst",new String(packet_source));
							
							/*Now, forward the multi-hop packet in another thread
							 * forcing it back into this program so that it get processed
							 * under the 'multi-hop' section of the loop */
							ForwardMessageThread forward_packet_thread = new ForwardMessageThread(routing_table,m_hop_reply,node_id);
							forward_packet_thread.start();
						}
						
						//If the packet was not wrapped when it was sent, simply forward it
						else
						{
							//Forward the packet in another thread
							ForwardMessageThread forward_packet_thread = new ForwardMessageThread(routing_table,response_packet);
							forward_packet_thread.start();
						}
					}
					
					/* 'multi-hop' packet carries another packet down a
					 * predetermined routing path
					 */
					else if (packet_type.equals("multi-hop"))
					{
						System.out.println("Bouncing a multi-hop packet down a predefined path");
						//Extract the routing path object
						List routing_path = (List) packet_object.get("routing_path");
						System.out.println("The path is recieved is: " + routing_path);
						//Rotate it by one so that it can be used by the following node
						//Collections.rotate(routing_path,1); // --- Moved this code block
						System.out.println("The new routing path is now: " + routing_path);
						Object payload_packet = packet_object.get("payload");
						System.out.println("--- BEGIN INNER PAYLOAD ---");
						System.out.println(payload_packet);
						System.out.println("--- END INNER PAYLOAD ---");
						if (routing_path.get(routing_path.size() - 1).equals(destination))
						{
							System.out.println("Packet has arrived");
							// Send the payload packet to the node itself.
							has_packet_from_self = true;
							packet_object = (JSONObject)payload_packet;
							
							/* The reply to this packet also needs to be
							 * wrapped in a multihop packet. */
							 
							 next_reply_multihop = true;
							 
							
							/* Now we need to send the reverse path to the destination
							 * node so that it knows how to properly reply
							 */
							List return_path = routing_path;
							Collections.rotate(return_path,1); // Rotate 1 place to the right
							Collections.reverse(return_path); // ... and reverse
							System.out.println("Putting " + return_path + " as route to " + packet_source);
							full_routes.put(packet_source,return_path);
							
							
							 
							 //JSONObject path_packet = new JSONObject();
							 //path_packet.put("dst",((JSONObject)payload_packet).get("dst"));
							 //path_packet.put("type",new String("full-path"));
							 //path_packet.put("src",((JSONObject)payload_packet).get("src"));
							 //path_packet.put("payload",routing_path);
							 //Finally forward this packet
							 //forwardPacket(path_packet);
							 
							 /* Log the reply route for this node so that
							  * packets can be sent to it.
							  */
							  
							 
							 // forward the packet in another thread
							 //ForwardMessageThread forward_packet_thread = new ForwardMessageThread(routing_table,path_packet);
							 //forward_packet_thread.start();
							 
						}
						else
						{
							System.out.println("Assembling new packet to forward");
							JSONObject new_packet = new JSONObject();
							Collections.rotate(routing_path,1); //Rotate the routing path so that it is ready for the next node
							new_packet.put("routing_path",routing_path);
							new_packet.put("src",new String(packet_source));
							new_packet.put("dst",new String(destination));
							new_packet.put("type", "multi-hop");
							new_packet.put("payload", payload_packet);
							System.out.println("Sending this packet down the 'wire'");
							System.out.println(new_packet);
							//Actually send the packet
							//forwardPacket(new_packet,(String)routing_path.get(routing_path.size()-1));
							
							//Send the packet in another thread
							ForwardMessageThread forward_packet_thread = new ForwardMessageThread(routing_table,new_packet,(String)routing_path.get(routing_path.size()-1));
							forward_packet_thread.start();
						}
					}
					else if (packet_type.equals("neighbour-list"))
					{
						//Print out list of neighbours
						Object neighbour_payload = packet_object.get("payload");
						System.out.println(neighbour_payload);
						PrintWriter writer = null;
						try 
						{
							writer = new PrintWriter(new OutputStreamWriter(
							new FileOutputStream("our_messages"), "utf-8"));
							writer.println("---BEGIN NEIGHBOURS ---");
							writer.println(neighbour_payload);
							writer.println("---END NEIGHBOURS ---");
						} 
						catch (IOException ex) 
						{
							// report
						}finally 
						{
							try 
							{
								writer.close();
							}
							catch (Exception ex)
							{
							}
						}
					}
					/* 'full-path' packet sends a full path to a node
					 * so that it may establish a route to it
					 */
					else if (packet_type.equals("full-path"))
					{
						List return_path = (List) packet_object.get("payload");
						Collections.rotate(return_path,1); // Rotate 1 place to the right
						Collections.reverse(return_path); // ... and reverse
						System.out.println("Putting " + return_path + " as route to " + packet_source);
						full_routes.put(packet_source,payload);
					}
				} 
			}
		}   
		catch (IOException e)
		{
			System.out.println(e);
		}
	}
	
	public static int forwardPacket(JSONObject packet)
	{
		//DEBUG
		String destination = (String) packet.get("dst");
		String next_ip = (String) routing_table.lookup_entry(destination);
		System.out.println("Forwarding...");
		System.out.println(packet);
		System.out.println("... to " + destination + " via " + next_ip);
		
		//Stop Immediately if there is no route for this packet
		if (next_ip == null)
		{
			return -1;
		}
		
		//Open a socket to next_ip and send it data, port is always 9999
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			textSocket = new Socket(next_ip, 9999); //TODO: add auto lookup of router ip
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// keep on reading from/to the socket till we receive the "Ok" from SMTP,
				// once we received that then we want to break.
				String responseLine;
				while ((responseLine = is.readLine()) != null)
				{
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
					{
						break;
                    }
		
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		
		return 0;
	}

	public static int forwardPacket(JSONObject packet, String destination)
	{
		//DEBUG
		//String destination = (String) packet.get("dst");
		String next_ip = (String) routing_table.lookup_entry(destination);
		System.out.println("Forwarding...");
		System.out.println(packet);
		System.out.println("... to " + destination + " via " + next_ip);
		
		//Stop Immediately if there is no route for this packet
		if (next_ip == null)
		{
			return -1;
		}
		
		//Open a socket to next_ip and send it data, port is always 9999
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			textSocket = new Socket(next_ip, 9999); //TODO: add auto lookup of router ip
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// keep on reading from/to the socket till we receive the "Ok" from SMTP,
				// once we received that then we want to break.
				String responseLine;
				while ((responseLine = is.readLine()) != null)
				{
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
					{
						break;
                    }
		
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		
		return 0;
	}
	
	public static int sendToMail(JSONObject packet)
	{
		//DEBUG
		//String destination = (String) packet.get("dst");
		//String next_ip = (String) routing_table.get(destination);
		//System.out.println("Forwarding...");
		System.out.println(packet);
		//System.out.println("... to " + destination + " via " + next_ip);
		
		//Stop Immediately if there is no route for this packet
		//if (next_ip == null)
		//{
		//	return -1;
		//}
		
		//Open a socket to next_ip and send it data, port is always 9999
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			textSocket = new Socket("192.168.137.1", 9998);
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// keep on reading from/to the socket till we receive the "Ok",
				// once we received that then we want to break.
				String responseLine;
				while ((responseLine = is.readLine()) != null)
				{
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
					{
						break;
                    }
		
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		
		return 0;
	}
	
	// Send this packet to a server which will forward the packet over a websocket
	public static int sendToWeb(JSONObject packet)
	{
		System.out.println(packet);
		
		//Send the data to localhost:9998
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			textSocket = new Socket("127.0.0.1", 9998);
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		
		return 0;
	}
	
	//public static int reply_to_multihop(JSONObject packet)
	//{
		////Construct a multihop packet with the reply and forward it as usual.		
		////Now build the 'super-packet' carrying the routing path along with the 'text-message' packet
		//String destination = (String)packet.get("dst");
		//String source = (String)packet.get("src");
		//JSONObject multihop_network_packet = new JSONObject();
		//multihop_network_packet.put("routing_path",full_routes.get((String)packet.get("dst")));
		//multihop_network_packet.put("payload",packet);
		//multihop_network_packet.put("type",new String("multi-hop"));
		//multihop_network_packet.put("dst", new String(destination));
		//multihop_network_packet.put("src", new String(source));
		
		////DEBUG
		//System.out.println("Now, send this down the 'Wire'");
		////System.out.println(network_packet);
		//System.out.println(multihop_network_packet);
		//return 0;
		
		////TODO: ACTUALLY SEND THE PACKET
	//}			
			
}

// Thread class which forwards a packet.
class ForwardMessageThread extends Thread
{
	RoutingTable routing_table; // Need a routing table if you want to forward packets
	String dst = null; // certain calls to forwardPacket will specify a destination as an IP address.
	JSONObject thread_packet;
	
	//method = 1 for forwardPacket(JSONObject packet)
	//method = 2 for forwardPacket(JSONObject packet, String destination)
	int method;
	 
	
	//Initialization method - pass the routing table as it is needed for forwarding packets
	//Configures to forward to the destination specified in the packet
	ForwardMessageThread(RoutingTable rtable, JSONObject pkt)
	{
		this.routing_table = rtable;
		this.thread_packet = pkt;
		this.method = 1;
		
	}
	
	//Configures to forward to any destination 'd'
	ForwardMessageThread(RoutingTable rtable, JSONObject pkt, String d)
	{
		this.routing_table = rtable;
		this.thread_packet = pkt;
		this.dst = d;
		this.method = 2;
	}
	
	public void run()
	{
		if (this.method == 1)
		{
			forwardPacket(this.thread_packet);
		}
		else if (this.method == 2)
		{
			forwardPacket(this.thread_packet,this.dst);
		}
	}
	
	public int forwardPacket(JSONObject packet)
	{
		//DEBUG
		String destination = (String) packet.get("dst");
		String next_ip = (String) this.routing_table.lookup_entry(destination);
		//See if we need to use a different port
		int port = 9999;
		if (this.routing_table.usesOtherPort(destination))
		{
			port = this.routing_table.get_port(destination);
		}
		System.out.println("Using port: " + port);
		System.out.println("Forwarding...");
		System.out.println(packet);
		System.out.println("... to " + destination + " via " + next_ip);
		
		//Stop Immediately if there is no route for this packet
		if (next_ip == null)
		{
			return -1;
		}
		
		//Open a socket to next_ip and send it data, port is always 9999
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			//textSocket = new Socket(next_ip, 9999); //TODO: add auto lookup of router ip
			textSocket = new Socket(next_ip, port); // Add support for different ports
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// keep on reading from/to the socket till we receive the "Ok" from SMTP,
				// once we received that then we want to break.
				String responseLine;
				while ((responseLine = is.readLine()) != null)
				{
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
					{
						break;
                    }
		
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		
		return 0;
	}

	public int forwardPacket(JSONObject packet, String destination)
	{
		//DEBUG
		//String destination = (String) packet.get("dst");
		String next_ip = (String) this.routing_table.lookup_entry(destination);
		//See if we need to use a different port
		int port = 9999;
		if (this.routing_table.usesOtherPort(destination))
		{
			port = this.routing_table.get_port(destination);
		}
		System.out.println("Using port: " + port);
		System.out.println("Forwarding...");
		System.out.println(packet);
		System.out.println("... to " + destination + " via " + next_ip);
		
		//Stop Immediately if there is no route for this packet
		if (next_ip == null)
		{
			return -1;
		}
		
		//Open a socket to next_ip and send it data, port is always 9999
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			//textSocket = new Socket(next_ip, 9999); //TODO: add auto lookup of router ip
			textSocket = new Socket(next_ip, port); // Add support for different ports
			os = new PrintStream(textSocket.getOutputStream());
			is = new DataInputStream(textSocket.getInputStream());
        }
        catch (UnknownHostException e)
        {
			System.err.println("Don't know about host: hostname");
        }
         
        catch (IOException e)
        {
			System.err.println("Couldn't get I/O for the connection to: hostname");
        }
        
		if (textSocket != null && os != null && is != null)
		{
			try
			{
				os.println(packet);    
				// keep on reading from/to the socket till we receive the "Ok" from SMTP,
				// once we received that then we want to break.
				String responseLine;
				while ((responseLine = is.readLine()) != null)
				{
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
					{
						break;
                    }
		
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				os.close();
				is.close();
				textSocket.close();   
            }
            
			catch (UnknownHostException e)
			{
				System.err.println("Trying to connect to unknown host: " + e);
            }
			catch (IOException e)
			{
				System.err.println("IOException:  " + e);
			}
		}
		return 0;
	}
}

// The routing table which will be shared across multiple threads
class RoutingTable
{
	JSONObject routing_table;
	/* The routing_table is a JSONObject which is able to translate
	* node ID names into local IP addresses. For example, the routing_table
	* on NODE B has an entry stored for NODE A which may be different from
	* the entry stored for NODE A on NODE C
	*/
	 
	JSONObject port_table;
	/* Entries into this table specify the node (key) want to communicate
	 * over a different port (value) instead of the usual port 9999.
	 */
	 
	JSONObject full_routes;
	/* Contains full routes (list of nodes which packet must pass
	 * through in order to reach a destination
	 */

	 
	//initialization method
	RoutingTable()
	{
		this.routing_table = new JSONObject();
		this.port_table = new JSONObject();
	}

	//add an entry to the routing table
	public synchronized void add_entry(String key, String value)
	{
		this.routing_table.put(key,value);
	}
	
	//remove an entry from the routing table
	public synchronized void delete_entry(String key, String value)
	{
		this.routing_table.remove(key);
	}
	
	//lookup an entry from the routing table
	public synchronized String lookup_entry(String key)
	{
		if (this.routing_table.containsKey(key))
		{
			return (String)this.routing_table.get(key);
		}
		else
		{	
			return null;
		}
	}
	
	//register that a node is using a port different from 9999
	public synchronized void map_port(String id, int port)
	{
		if (port != 9999)
		{
			this.port_table.put(id,port);
		}
	}
	
	// Know if 'id' is using port 9999 or some other port
	public synchronized boolean usesOtherPort(String id)
	{
		//return ((List)(this.port_table)).contains(id); Does not work - crashes
		return this.port_table.containsKey(id) && (this.port_table.get(id) != "9999");
	}
	
	// Get the port that 'id' is using
	public synchronized int get_port(String id)
	{
		if (this.usesOtherPort(id))
		{
			return Integer.parseInt(String.valueOf(this.port_table.get(id)));
		}
		else
		{
			return 9999;
		}
	}
	
		
}
	

