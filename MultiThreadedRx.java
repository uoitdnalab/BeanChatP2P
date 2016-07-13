import java.io.*;
import java.net.*;
import java.util.*;
import org.json.simple.*;

/* Keeps track of open connections (and threads) so that
 * reply messages have routes when they arrive
 */
class ConnectionTracker
{
	List connection_list;
	MessagePool message_pool;
	ConnectionTracker()
	{
		this.connection_list = new ArrayList();
		this.message_pool = new MessagePool();
	}
	
	public void add_connection(String username)
	{
		//DEBUG
		System.out.println("Adding " + username + " to list of local users");
		
		this.connection_list.add(username);
		this.message_pool.add_user(username);
		
		//DEBUG
		System.out.println(this.connection_list);
	}
	
	public void del_connection(String username)
	{
		//DEBUG
		System.out.println("Deleting " + username + " from list of local users");
		
		this.connection_list.remove(username);
		this.message_pool.del_user(username);

		
		//DEBUG
		System.out.println(this.connection_list);
	}
	
	public boolean has_connection(String username)
	{
		if (this.connection_list.contains(username))
		{
			//DEBUG
			System.out.println("Has connection to " + username);
			return true;
		}
		else
		{
			//DEBUG
			System.out.println("NO connection to " + username);
			return false;
		}
	}
}

/* Allows one thread to enqueue a message for user X so that another
 * user may dequeue a message for user X
 */
class MessagePool
{
	HashMap users_hash_map;
	MessagePool()
	{
		this.users_hash_map = new HashMap();
	}
	
	public synchronized void add_user(String username)
	{
		System.out.println("Adding " + username + " to message pool.");
		Queue msgs = new LinkedList();
		this.users_hash_map.put(username,msgs);
	}
	public synchronized void del_user(String username)
	{
		System.out.println("Removing " + username + " from message pool.");
		this.users_hash_map.remove(username);
	}
	public synchronized void enqueue_message(String destination, JSONObject message)
	{
		Queue msgs = (Queue)this.users_hash_map.get(destination);
		msgs.add(message);
	}
	public synchronized JSONObject dequeue_message(String username)
	{
		// Stop immediately if user no longer exists
		if (this.users_hash_map.containsKey(username) == false)
		{
			return null;
		}
		Queue msgs = (Queue)this.users_hash_map.get(username);
		//Check the length of the queue. Don't call remove() on an empty queue
		if (msgs.size() > 0)
		{
			return (JSONObject)msgs.remove();
		}
		else
		{
			return null;
		}
	}
}

//Signals to the thread to shutdown when connection is closed
class MessagePumpSwitch
{
	boolean is_on;
	MessagePumpSwitch()
	{
		this.is_on = true;
	}
	synchronized boolean isOn()
	{
		return this.is_on;
	}
	synchronized void turnOff()
	{
		this.is_on = false;
	}
}


public class MultiThreadedRx
{
	public static ServerSocket myServer = null;
	
	public static void main(String args[])
	{
		System.out.println("Starting multithreaded server");
		
		//Start listening on port 9998
		try
		{
			myServer = new ServerSocket(9998);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
		
		//create a ConnectionTracker object for message passing between threads
		ConnectionTracker tracker = new ConnectionTracker();
		
		//Now listen for connections and for each start a new thread
		while (true)
		{
			//Get an incoming connection
			Socket clientSocket = null;
			try
			{
				clientSocket = myServer.accept();
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
			// And start a new thread for it
			ServerThread tmp_thread = new ServerThread(clientSocket, tracker);
			tmp_thread.start();
		}
		// And start a new thread for it
		//ServerThread thread001 = new ServerThread();
		//thread001.start();
	}
}

class ServerThread extends Thread
{
	Socket clientSocket;
	String username = null; //The ID of the user of which this thread is serving.
	ConnectionTracker tracker;
	MessagePumpSwitch pump_switch;
	ServerThread(Socket sock, ConnectionTracker track)
	{
		this.clientSocket = sock;
		this.tracker = track;
		/*Control for the message pump. It must be shut off when the
		 * connection closes
		 */
		this.pump_switch = new MessagePumpSwitch();
	}
	
	public void run()
	{
		System.out.println("Starting a new thread");
		
		DataInputStream is;
		PrintStream os;
		String line;
		
			
		try
		{
			is = new DataInputStream(this.clientSocket.getInputStream());
			os = new PrintStream(this.clientSocket.getOutputStream());
			// Run another thread pumping messages from the message pool to the output stream
			while (true)
			{
				line = is.readLine();
				//End this thread once the user has exited
				if (line == null)
				{
					System.out.println("Connection is finished");
					if (this.username != null)
					{
						this.tracker.del_connection(this.username);
					}
					this.pump_switch.turnOff();
					is.close(); //close input stream
					return;
				}
				System.out.println(line);
				Object obj = JSONValue.parse(line);
				JSONObject packet_object = (JSONObject)obj;
				
				//When we see a 'network_advertise' packet, add the user to the tracker
				if (((String)packet_object.get("type")).equals("net-advertise"))
				{
					System.out.println("Recieved a network advertise packet");
					this.username = (String)packet_object.get("src");
					this.tracker.add_connection(this.username);
					
					//Push the 'net-advertise' packet onto the mesh while modifying the port number
					packet_object.put("port","9998");
					push_to_mesh(packet_object);
					
					
					//Now start pumping messages in another thread
					MessagePump tmp_message_pump = new MessagePump(os,this.tracker.message_pool,this.username, this.pump_switch);
					tmp_message_pump.start();
				}
				
				//When we see a 'get-neighbours' packet ask the Pi for the neighbours which it is connected to
				else if (((String)packet_object.get("type")).equals("get-neighbours"))
				{
					System.out.println("Recieved a 'get-neighbours' packet");
					// Get the list of neighbours
					push_to_mesh(line);
				}
				
				//Handle 'neighbour-list' packets
				else if (((String)packet_object.get("type")).equals("neighbour-list"))
				{
					System.out.println("Recieved a list of neighbours");
				}
				
				
				
				
				//ignore packets with empty source, type or destination fields.
				if ((String)packet_object.get("src") == null || (String)packet_object.get("type") == null || (String)packet_object.get("dst") == null)
				{
					continue;
				}
				String destination = (String)packet_object.get("dst");
				
				//Record the source of this packet --- NO! this is done when the user logs in
				//this.username = (String)packet_object.get("src");
				//this.tracker.add_connection(this.username);
				
				//test line of JSON --- a reply from a bot
				//os.println("{\"src\":\"reply.bot\",\"payload\":\"I got your message\",\"type\":\"text-message\"}");
				
				//TODO: If packet belongs to somebody connected to this websocket send it that way
				if (this.tracker.has_connection(destination))
				{
					//DEBUG
					System.out.println(destination + " is connected directly to this server");
					
					//Put the message into a pool that is shared between all threads
					this.tracker.message_pool.enqueue_message(destination, packet_object);
				}
				
				
				//if (destination.equals(this.username))// FIXME: check other threads
				//{
				//	os.println(packet_object);
				//}
				else
				{
					//Else, forward this packet onto the mesh
					push_to_mesh(line);
				}
				/*Start a new thread waiting for a reply.
				 * Once the reply arrives forward it to
				 * the sender.
				 */
				 
				 //Temporary: try this
				 //os.println(this.tracker.message_pool.dequeue_message(this.username));
				
			}
		}   
		catch (IOException e)
		{
			System.out.println(e);
		}
	}
	
	//Sends a packet to the local router
	public void push_to_mesh(String packet)
	{
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			//Send the message to the router running on localhost and let it deal with the rest
			textSocket = new Socket("127.0.0.1", 9999);
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
	}
	
	//Sends a packet to the local router
	public void push_to_mesh(JSONObject packet)
	{
		Socket textSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		try
		{
			//Send the message to the router running on localhost and let it deal with the rest
			textSocket = new Socket("127.0.0.1", 9999);
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
	}
}

class MessagePump extends Thread
{
	PrintStream os;
	MessagePool mp;
	String username;
	MessagePumpSwitch mps;
	MessagePump(PrintStream o_stream, MessagePool m_pool, String usr_name, MessagePumpSwitch mp_switch)
	{
		this.os = o_stream;
		this.mp = m_pool;
		this.username = usr_name;
		this.mps = mp_switch;
	}
	
	public void run()
	{
		System.out.println("Message pump is running");
		while (this.mps.isOn())
		{
			//pump messages
			if (this.os == null || this.mp == null)
			{
				System.out.println("User has disconnected. Shutting down message pump");
				return;
			}
			//Check that packet is not null
			JSONObject ready_packet = this.mp.dequeue_message(this.username);
			if (ready_packet != null)
			{
				this.os.println(ready_packet);
			}
		}
		//DEBUG
		System.out.println("Message pump closing");
		this.os.close(); // close output stream
	}
}
