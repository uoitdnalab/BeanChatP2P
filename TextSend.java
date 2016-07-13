import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.json.simple.JSONObject;

public class TextSend
{
	public static JSONObject network_packet;
	
	public static void main(String args[])
	{
		String dst_node_id = args[0];
		String source_node_id = args[1];
		String message_body = args[2];

		
		//DEBUG
		System.out.println("Destination: " + dst_node_id);
		System.out.println("Message: " + message_body);
		
		//Create a network packet
		network_packet = new JSONObject();
		// ... and fill out the fields
		network_packet.put("dst",new String(dst_node_id)); // The destination of the packet
		network_packet.put("payload", new String(message_body)); // The payload, in this case a 'text-message'
		network_packet.put("type",new String("text-message")); // What is this type of packet
		network_packet.put("src",new String(source_node_id));
		
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
			//Send the message to the router running on localhost and let it deal with the rest
			textSocket = new Socket("127.0.0.1", 9999);
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
