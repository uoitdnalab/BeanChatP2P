import java.io.*;
import java.net.*;
public class CommandServer {
    public static void main(String args[]) {
		// declaration section:
		// declare a server socket and a client socket for the server
		// declare an input and an output stream
		ServerSocket cmdServer = null;
		String line;
		DataInputStream is;
		//PrintStream os;
		Socket clientSocket = null;
		// Try to open a server socket on port 9999
		// Note that we can't choose a port less than 1023 if we are not
		// privileged users (root)
        try {
           cmdServer = new ServerSocket(9999);
        }
        catch (IOException e) {
           System.out.println(e);
        }   
		// Create a socket object from the ServerSocket to listen and accept 
		// connections.
		// Open input and output streams
    try {
			clientSocket = cmdServer.accept();
			is = new DataInputStream(clientSocket.getInputStream());
           
           //os = new PrintStream(clientSocket.getOutputStream());
			// As long as we receive data, echo that data back to the client.
           while (true) {
             line = is.readLine();
             //os.println(line); 
             System.out.println(line);
           }
        }   
    catch (IOException e) {
           System.out.println(e);
        }
    }
    
}
