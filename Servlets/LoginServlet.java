import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;

public class LoginServlet extends HttpServlet{

	private static final long serialVersionUID = 2702524403574618123L;
	private static final String PROPERTIESFILENAME = "db.properties";
	private static final String SPECIALSTRING = "KaChing";
	private static final int SALT_LENGTH = 10; 	//Length 10 to get a 16 character long salt
		
	private static String URL;
	private static String HOST;
	private static String PORT;
	private static String DBNAME;
	private static String USERNAME;
	private static String PASSWORD;

	private static Connection conn = null;
	
	public void init() throws ServletException{
		try{
			Properties properties = new Properties();
			
			try {
				//Load via relative path (or absolute path)
				//InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIESFILENAME);
				//properties.load(inputStream);

				//Load via thread context
				//properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIESFILENAME));

				//Load via servlet context
				properties.load(getServletContext().getResourceAsStream("/WEB-INF/" + PROPERTIESFILENAME));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			HOST = properties.getProperty("host");
			PORT = properties.getProperty("port");
		    DBNAME = properties.getProperty("dbname");
		    USERNAME = properties.getProperty("username");
		    PASSWORD = properties.getProperty("password");

		    URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DBNAME;

			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException
	{
		String user = URLDecoder.decode(request.getParameter("username"), "UTF-8");
		String pass = URLDecoder.decode(request.getParameter("password"), "UTF-8");

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		

		try{
			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT id, username, password, salt FROM user WHERE username LIKE '" + user + "';");

			if(rs.next()){
				int id = rs.getInt("id");
				String username = rs.getString("username");
				String storedPass = rs.getString("password");
				String salt = rs.getString("salt");

				MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
				String saltedPass = pass + salt;
				sha256.update(saltedPass.getBytes("UTF-8"));
				byte[] digest = sha256.digest();
				
				String hashedPass = Base64.encodeBase64String(digest);
				
				if(storedPass.compareTo(hashedPass) == 0){
					JSONObject json = new JSONObject();
					json.put("id", id);
					json.put("username", username);
					
					String tokenString = request.getRemoteAddr() + SPECIALSTRING;
					sha256.reset();
					sha256.update(tokenString.getBytes("UTF-8"));
					digest = sha256.digest();
					String token = Base64.encodeBase64String(digest);
					json.put("token", token);
					
					out.println(json.toJSONString());
				}
				else{
					response.sendError(401);
				}

			}
			else{
				response.sendError(401);
			}

			
		} catch(Exception e){
			e.printStackTrace();
			out.println("Exception e - " + e.getMessage());
			StackTraceElement[] ste = e.getStackTrace();
			for(int i = 0; i < ste.length; i++){
				out.println(ste[i]);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException
	{
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		String username = request.getParameter("username");
		String password = request.getParameter("password");

		byte[] saltBytes = new byte[SALT_LENGTH];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(saltBytes);
		String salt = Base64.encodeBase64String(saltBytes);
		
		try{
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			String saltedPass = password + salt;
			sha256.update(saltedPass.getBytes("UTF-8"));
			byte[] digest = sha256.digest();
			String hashedPass = Base64.encodeBase64String(digest);

			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();	

			//Check if username is already taken
			ResultSet rs = st.executeQuery("SELECT id, username, password, salt FROM user WHERE username LIKE '" + username + "';");

			if(rs.next()){
				response.sendError(401);
			} 
			else{
				st = conn.createStatement();
				st.executeUpdate("INSERT INTO user(username, password, salt) VALUES('"+username+"','"+hashedPass+"','"+salt+"');");

				st = conn.createStatement();
				rs = st.executeQuery("SELECT id, username, password, salt FROM user WHERE username LIKE '" + username + "';");
				if(rs.next()){
					int id = rs.getInt("id");
					JSONObject json = new JSONObject();
					json.put("id", id);
					json.put("username", username);
					
					String tokenString = request.getRemoteAddr() + SPECIALSTRING;
					sha256.reset();
					sha256.update(tokenString.getBytes("UTF-8"));
					digest = sha256.digest();
					String token = Base64.encodeBase64String(digest);
					json.put("token", token);
					
					out.println(json.toJSONString());
				}
			}

			rs.close();
			st.close();

		} catch(Exception e){
			e.printStackTrace();
			out.println("Exception e - " + e.getMessage());
			StackTraceElement[] ste = e.getStackTrace();
			for(int i = 0; i < ste.length; i++){
				out.println(ste[i]);
			}
		}

		//These headers don't show up in the response headers for some reason
		//response.setHeader("Access-Control-Allow-Credentials", "true");
		//response.setHeader("Access-Control-Allow-Origin", "http://128.4.26.235");
		//response.setHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin,Access-Control-Allow-Credentials");
	}
	
	public void destroy(){
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
