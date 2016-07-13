import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.json.simple.JSONObject;

public class NetworkAdvertise
{
	public static JSONObject network_packet;
	
	public static void main(String args[])
	{
		String src_node_id = args[0];
		String advertise_to_ip = args[1];
		
		//DEBUG
		System.out.println("Source: " + src_node_id);
		
		//Create a network packet
		network_packet = new JSONObject();
		// ... and fill out the fields
		network_packet.put("type",new String("net-advertise")); // What is this type of packet.
		/* The 'net-advertise' packet advertises the node's presence to it's neighbours so that
		 * it may be added to their routing tables
		 */
		 network_packet.put("src",new String(src_node_id));
		 
		
		
		//DEBUG
		System.out.println("Now, send this down the 'Wire'");
		System.out.println(network_packet);
		
		//Sending this text message down the wire
		Socket textSocket = null;  
        //DataOutputStream os = null;
        //DataInputStream is = null;
        PrintStream os = null;
        DataInputStream is = null;
        
		try
		{
			//textSocket = new Socket("192.168.0.2", 9999); //TODO: add auto lookup of router ip
			textSocket = new Socket(advertise_to_ip, 9999);
			//os = new DataOutputStream(textSocket.getOutputStream());
			//is = new DataInputStream(textSocket.getInputStream());
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
				//os.writeBytes("HELO\n");
				os.println(network_packet);    
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
	
	public static void do_advertise(String src_node_id, String node_ip_addr)
	{
		//String src_node_id = args[0];
		
		//DEBUG
		System.out.println("Source: " + src_node_id);
		
		//Create a network packet
		network_packet = new JSONObject();
		// ... and fill out the fields
		network_packet.put("type",new String("net-advertise")); // What is this type of packet.
		/* The 'net-advertise' packet advertises the node's presence to it's neighbours so that
		 * it may be added to their routing tables
		 */
		 network_packet.put("src",new String(src_node_id));
		 
		
		
		//DEBUG
		System.out.println("Now, send this down the 'Wire'");
		System.out.println(network_packet);
		
		//Sending this text message down the wire
		Socket textSocket = null;  
        //DataOutputStream os = null;
        //DataInputStream is = null;
        PrintStream os = null;
        DataInputStream is = null;
        
		try
		{
			textSocket = new Socket(node_ip_addr, 9999);
			//os = new DataOutputStream(textSocket.getOutputStream());
			//is = new DataInputStream(textSocket.getInputStream());
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
				//os.writeBytes("HELO\n");
				os.println(network_packet);    
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
