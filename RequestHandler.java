import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class RequestHandler extends Thread {
	private Socket socket;
	private String documentRoot;
	private final static String RFC1123_DATE_PATTERN="EEE, dd MMM yyyy HH:mm:ss z";
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat(RFC1123_DATE_PATTERN);
	public final static String CRLF ="\r\n";
	InputStream in;
	OutputStream out;
	BufferedReader buffReader;
	// Start the thread in the constructor
	public RequestHandler(Socket s, String docRoot) throws IOException {
		this.socket = s;
		this.documentRoot = docRoot;
		this.out = socket.getOutputStream();
		
		
	}
	public void run()
	{
		boolean keepAliveCheck=false;
		while(true)
		{
			showDetails();
			//System.out.println(keepAliveCheck);
			if(!keepAliveCheck)
			{
				break;
			}
			
		}
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean showDetails() {
		
		boolean isKeepAlive= false;
		String keepAlive = "close";
		int statusCode;
		// Open connections to the socket
		try {
				
					socket.setSoTimeout(50000);
				
					this.buffReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
					isKeepAlive= false;
					String s = buffReader.readLine();
					//System.out.println(s.length());
					if(s==null || s.length()<=0 || s.equalsIgnoreCase("null"))
					{
						s = "";
					}
					if(s.equals(CRLF) || s.equals("")) s="";
					String line;
					int length;
					String filename = "";
					StringTokenizer st = new StringTokenizer(s);
					String headerStr[] = new String[st.countTokens()];
					int index = 0;
					int headerStrLength = headerStr.length;
					while (st.hasMoreElements()) {
						headerStr[index++] = st.nextToken();
					}
					filename = headerStr[1];
					
					if (!headerStr[0].equalsIgnoreCase("GET")) {
						statusCode = 501;
						showErrorMssg(501, out,filename,"close" );
						return false;
					}

					if (!headerStr[headerStrLength - 1]
							.equalsIgnoreCase("HTTP/1.0") && !headerStr[headerStrLength - 1]
									.equalsIgnoreCase("HTTP/1.1")) {
						statusCode = 400;
						showErrorMssg(400, out, filename,"close");
						isKeepAlive= false;
						return false;
					}
					
					while ((line = buffReader.readLine()) != null) {
						if (line.equals("")) {
							break;
						}
						
						if (line.startsWith("Content-Length: ")) { // get the
							// content-length
							index = line.indexOf(':') + 1;
							String len = line.substring(index).trim();
							length = Integer.parseInt(len);
							if(length <0)
							{
								statusCode=400;
								showErrorMssg(400, out, filename,"close");
								isKeepAlive= false;
								
							}
						}
						if (line.startsWith("Connection: ")) { // get the
							// content-length
							index = line.indexOf(':') + 1;
							keepAlive = line.substring(index).trim();
			
							if(keepAlive.equalsIgnoreCase("keep-alive"))
							{
								isKeepAlive=true;
							}
							else
							{
								isKeepAlive=false;
							}
							
						}
					}
					
					if (filename.endsWith("/"))
						filename += "index.html";

					// Remove leading / from filename
					while (filename.indexOf("/") == 0)
						filename = filename.substring(1);

					// Replace "/" with "\" in path for PC-based servers
					filename = filename.replace('/', File.separator.charAt(0));

					// Check for illegal characters to prevent access to
					// superdirectories
					if (filename.indexOf("..") >= 0 || filename.indexOf(':') >= 0
							|| filename.indexOf('|') >= 0)
					{
						showErrorMssg(400, out, filename,keepAlive);
					}
					if (new File(filename).isDirectory()) {
						filename = filename.replace('\\', '/');
						showErrorMssg(301, out, filename,keepAlive);
						return isKeepAlive;
					}

					// Open the file (may throw FileNotFoundException)
					filename = documentRoot+"/"+ filename;
					
					
					
					if(!new File(filename).exists())
					{
						showErrorMssg(404, out, filename,keepAlive);
						return isKeepAlive;
					}
					
					if (!new File(filename).canRead()) {
						showErrorMssg(403, out, filename,keepAlive);
						return isKeepAlive;
					}
					
					InputStream f = new FileInputStream(filename);
					// Determine the MIME type and print HTTP header
					String mimeType = "text/plain";
					if (filename.endsWith(".html") || filename.endsWith(".htm"))
						mimeType = "text/html";
					else if (filename.endsWith(".jpg")
							|| filename.endsWith(".jpeg"))
						mimeType = "image/jpeg";
					else if (filename.endsWith(".gif"))
						mimeType = "image/gif";
					else if (filename.endsWith(".class"))
						mimeType = "application/octet-stream";
					else if (filename.endsWith(".js"))
						mimeType = "application/js";
					
					out.write(("HTTP/1.0 200 OK\r\n" + "Content-type:" + mimeType
							+ "\r\n"+"Connection:"+keepAlive+ "\r\n"
							+ "Date :"+dateFormat.format(new Date())+"\r\n\r\n").getBytes());

					// Send file contents to client, then close the connection
					byte[] a = new byte[4096];
					int n;
					while ((n = f.read(a)) > 0)
						out.write(a, 0, n);
					
					//out.println();

			
//			out.flush();
//			out.close();
			return isKeepAlive;
				

		}catch (SocketTimeoutException e){
	        System.out.println("Timeout");
	        System.exit(1);
	    } 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			
		}
		return isKeepAlive;
	}
	
	public void showErrorMssg(int statusCode, OutputStream out,String filename, String keepAlive) throws IOException
	{
		String errorMssg="";
		
		if(statusCode==301)
		{
			errorMssg="HTTP 301 Moved Permanently";
			out.write(("HTTP/1.0 301 Moved Permanently\r\n"
					+ "Content-type: text/html\r\n"
					+ "Content-Length: "+ errorMssg.length()+"\r\n"
					+ "Connection:"+keepAlive+"\r\n"
					+ "Date:"+ dateFormat.format(new Date())+"\r\n"
					+ "Location: /"
					+ filename
					+ "/\r\n\r\n"
					+ "<html><head><title> HTTP 301 Forbidden"
					+ "</title> </head><body><h1>HTTP 301 Moved Permanently</h1>"
					+ "</body></html>\n").getBytes());
			out.write(errorMssg.getBytes());
			return;
		}
		else if(statusCode==400)
		{
			errorMssg="HTTP 400 Bad Request";
			out.write(("HTTP/1.0 400 Bad Request\r\n"
					+ "Content-type: text/html\r\n"
					+ "Content-Length: "+ errorMssg.length()+"\r\n"
					+ "Connection:"+keepAlive+"\r\n"
					+ "Date:"+ dateFormat.format(new Date())
					+ "/\r\n\r\n"
					+ "<html><head><title> HTTP 400 Bad Request"
					+ "</title> </head><body><h1>HTTP 400 Bad Request</h1></body></html>\n").getBytes());
			out.write(errorMssg.getBytes());
			return;
		}
		else if(statusCode==403)
		{
			errorMssg="HTTP 403 Forbidden";
			out.write(("HTTP/1.0 403 Forbidden \r\n"
					+ "Content-type:text/html;charset=utf-8 \r\n"
					+ "Content-Length: "+ errorMssg.length()+" \r\n"
					+ "Connection:"+keepAlive+" \r\n"
					+ "Date:"+ dateFormat.format(new Date()) + "\r\n"
					+ "<html><head><title>"
					+ "HTTP 403 Forbidden"
					+ "</title> </head><body>HTTP 403 Forbidden</body></html> \r\n\r\n").getBytes());
			//out.close();
			out.write(errorMssg.getBytes());
			return;
		}
		else if(statusCode==404)
		{
			
			errorMssg="HTTP 404 File Not Found";
			out.write(("HTTP/1.0 404 Forbidden \r\n"
					+ "Content-type:text/html;charset=utf-8 \r\n"
					+ "Content-Length: "+ errorMssg.length()+" \r\n"
					+ "Connection:"+keepAlive+" \r\n"
					+ "Date:"+ dateFormat.format(new Date()) + "\r\n"
					+ "<html><head><title>"
					+ "HTTP 404 Forbidden"
					+ "</title> </head><body>HTTP 404 Forbidden</body></html> \r\n\r\n").getBytes());
			out.write(errorMssg.getBytes());
			return;
		}
		else if(statusCode==501)
		{
			errorMssg="HTTP 501 Not Implemented";
			out.write(("HTTP/1.0 501 Not Implemented\r\n"
					+ "Content-type: text/html\r\n"
					+ "Content-Length: "+ errorMssg.length()+"\r\n"
					+ "Connection:"+keepAlive+"\r\n"
					+ "Date:"+ dateFormat.format(new Date()) +"\r\n\r\n"
					+ "<html><head><title>"
					+ "   HTTP 501 Not Implemented"
					+ "  </title> </head><body><h1>HTTP 501 Not Implemented</h1>"
					+ " Header value specifies method that is not implemented</body></html>\n").getBytes());
			out.write(errorMssg.getBytes());
			return;

		}

	}
	
	public void responseHeader (int statusCode, PrintStream out,String filename,String errorMssg,
			String mimeType, String requestType, String keepAlive)
	{
		
		String responseBody ="<html><head><title>"
				+ requestType + " "+ statusCode + " "+ errorMssg
				+ "  </title> </head><body><h1>"+requestType + " "+ statusCode+ " "+ errorMssg +"</h1>"
				+ "</body></html>\n";
		int contentLength = responseBody.length();
		String responseHeader = requestType+" "+statusCode+CRLF
				+ "Content-type:"+ mimeType + CRLF
				+ "Content-Length:"+ contentLength+CRLF
				+ "Connection:"+keepAlive+CRLF
				+ "Date: "+ dateFormat.format(new Date()) +CRLF+CRLF;
		out.println(responseHeader+responseBody);
		out.flush();
		out.close();
	}
}
