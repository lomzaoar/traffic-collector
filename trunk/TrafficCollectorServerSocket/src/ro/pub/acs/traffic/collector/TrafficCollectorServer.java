package ro.pub.acs.traffic.collector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

import com.mysql.jdbc.Statement;

class ConnectionThread extends Thread {
	
	Socket socket;
	boolean debug;
	DBManager db;
	
	public static final String DATE_FORMAT_NOW = "yyyy_MM_dd_HH_mm_ss";
	
	/**
	 * Constructor for the ConnectionThread class.
	 * @param socket the socket connecting to the client
	 * @param debug boolean value for printing debug data
	 */
	public ConnectionThread(Socket socket, boolean debug) {
		this.socket = socket;
		this.debug = debug;
	}

	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			String nextLine;
			String delimiter = "##";
			String tokens[];
			
			// read the first line.
			nextLine = in.readLine();
			if (debug)
				System.out.println(nextLine);
			
			// exit if the message format is incorrect.
			if (nextLine.startsWith("#s#"))
				out.println("ACK");
			else
				return;
			
			tokens = nextLine.split(delimiter);
			nextLine = nextLine.replace("#s#", "");
			Calendar cal = Calendar.getInstance();
		    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
			String filename = "journey" + URLEncoder.encode(sdf.format(cal.getTime()), "UTF-8") + ".log";
			
			// check for the correctness of the message.
			if (tokens.length < 3)
				return;
			
			File file = new File("logs", filename);
			
			FileWriter fstream = new FileWriter(file, true);
			BufferedWriter outFile = new BufferedWriter(fstream);
			
			StringTokenizer st = new StringTokenizer(nextLine, "##");
			
			String name = st.nextToken();
			String facebook = st.nextToken();
			String twitter = st.nextToken();
			String id_user = st.nextToken();
			
			try {
				db = new DBManager();
				Statement statement = (Statement) db.getConn().createStatement();
			
				ResultSet rs = null; 
				rs = statement.executeQuery("SELECT * FROM location WHERE id_user='" + id_user + "'");
				if(!rs.next())
					db.doQuery("INSERT INTO location " +
								"(id_user, name, facebook, twitter, lat, lng, speed, timestamp, stop) " +
								"VALUES " +
								"('" + id_user + "', '" + name + "', '" + facebook + "', '" + twitter + "', '', '', '', '', 0)");
				else { 
					db.doQuery("UPDATE location SET " +
								"name = '" + name + "', " +
								"facebook = '" + facebook + "', " +
								"twitter = '" + twitter + "' " +
							"WHERE id_user='" + id_user + "'");
				db.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			outFile.write(nextLine);
			outFile.newLine();
			
			outFile.close();
			fstream.close();
			
			// receive data and write it to file.
			nextLine = in.readLine();
			if(nextLine != null)
			{
				while (!nextLine.equals("#f#")) {
					fstream = new FileWriter(file, true);
					outFile = new BufferedWriter(fstream);
					
					StringTokenizer pos = new StringTokenizer(nextLine, " ");
					String lat, lng, speed, timestamp;
					lat = pos.nextToken();
					lng = pos.nextToken();
					speed = pos.nextToken();
					timestamp = pos.nextToken();
					
					db = new DBManager();
					
					db.doQuery("UPDATE `location` SET " +
										"`lat`='" + lat + "', " +
										"`lng`='" + lng + "', " +
										"`speed`='" + speed + "', " +
										"`timestamp`='" + timestamp + "' " +
									"WHERE id_user='" + id_user + "'");
					
					db.close();
					
					outFile.write(nextLine);
					outFile.newLine();
					nextLine = in.readLine();
					
					outFile.close();
					fstream.close();
				}
			}
			// close all open streams.
			
			db.close();
			out.close();
			in.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Server error. Please restart.");
			return;
		}		
	}
}

public class TrafficCollectorServer {
	
	/**
	 * Main method.
	 * @param args array of command line arguments
	 */
	public static void main(String args[]) {
		ServerSocket serverSocket;
		boolean debug = false;
		
		try {
			// create server socket on the 8082 port.
			serverSocket = new ServerSocket(8082);
			
			// set debug value.
			if (args.length == 1 && args[0].equals("-v"))
				debug = true;
			
			while (true) {
				Socket clientSocket = serverSocket.accept();
				Thread connectionThread = new ConnectionThread(clientSocket, debug);
				new Thread(connectionThread).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Server error. Please restart.");
		}
	}
}