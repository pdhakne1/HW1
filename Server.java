import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Server {

	private static ServerSocket serverSocket;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String documentRoot="";
		int defaultPort = 6456;
		int portNumber=0;
		for (int i=0;i<args.length;i++)
		{
			if(args[i].equalsIgnoreCase("-document_root"))
			{
				documentRoot=args[i+1];
			}
			if(args[i].equalsIgnoreCase("-port"))
			{
				portNumber = Integer.valueOf(args[i+1]);
			}
		}
		
		serverSocket = new ServerSocket(portNumber); // Start, listen on port specified
														

		while (true) {
			try {

				
				Socket s = serverSocket.accept(); // waiting for client
													// connection
				new RequestHandler(s,documentRoot).start();
				

			} catch (Exception x) {
				System.out.println("Exception occured: "+x);
			}
		}

	}

}
