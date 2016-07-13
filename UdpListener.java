import java.io.*;
import java.net.*;

class UdpListener
{
	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(8987);
		byte[] receiveData = new byte[1024];
		String node_id = args[0];
		while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String ip_name = new String( receivePacket.getData());
				System.out.println("RECEIVED: " + ip_name);
				
				//NetworkAdvertise to this advertised node - unless the node is ourself
				if (!ip_name.equals(args[1]))
				{
					NetworkAdvertise net_ad = new NetworkAdvertise();
					net_ad.do_advertise(node_id,ip_name);
				}
			}
      }
}
