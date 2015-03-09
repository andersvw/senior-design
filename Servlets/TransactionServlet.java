import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TransactionServlet extends HttpServlet{

	private static final long serialVersionUID = 2702524403574618122L;
	private static final String PROPERTIESFILENAME = "db.properties";
	
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
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException
	{
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		out.println("Handled GET request");
	}
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException
	{
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();


		BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
		String data = URLDecoder.decode(br.readLine(), "UTF-8");

		JSONObject json = null;
		try {
			json = (JSONObject) new JSONParser().parse(data);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		String type = (String) json.get("type");
		JSONObject info = (JSONObject) json.get("info");
		String accountNumber = (String) info.get("account number");
		
		double amount = 0.0;
		
		JSONObject returnedJSON = new JSONObject();
		switch (type){
			case "balance":
				returnedJSON = getBalance(accountNumber);
				break;
			case "withdraw":
				amount = (Double) info.get("amount");
				returnedJSON = withdraw(accountNumber, amount);
				break;
			case "charge":
				amount = (Double) info.get("amount");
				returnedJSON = withdraw(accountNumber, amount);
				break;
			case "deposit":
				amount = (Double) info.get("amount");
				returnedJSON = deposit(accountNumber, amount);
				break;
			case "payment":
				amount = (Double) info.get("amount");
				returnedJSON = deposit(accountNumber, amount);
				break;
			case "history":
				long numDays = (Long) info.get("day range");
				returnedJSON = getHistory(accountNumber, numDays);
				break;
		}
		
		out.println(returnedJSON.toJSONString());

		
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getBalance(String accountNumber){
		JSONObject account = new JSONObject();
		
		try{
			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();

			String tableName = "account";
			List<String> fields = new ArrayList<String>();
			fields.add("balance");
			if(accountNumber.length() > 10) {
				tableName = "credit_account";
				fields.add("limit");
			}
			
			String query = "SELECT ";
			for(int i = 0; i < fields.size(); i++){
				query += fields.get(i) + " ";
			}
			query += "FROM " + tableName + " WHERE number = '" + accountNumber + "';";
			
			ResultSet rs = st.executeQuery(query);
			
			while(rs.next()){
				for(int i = 0; i < fields.size(); i++){
					account.put(fields.get(0), rs.getString(fields.get(0)));
				}
			}

			
		} catch(Exception e){
			e.printStackTrace();
		} 
		
		return account;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject withdraw(String accountNumber, double amount){
		JSONObject account = new JSONObject();
		
		try{
			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();

			String tableName = "account";
			if(accountNumber.length() > 10) {
				tableName = "credit_account";
			}
			
			String query = "UPDATE " + tableName + " SET balance=balance-" + amount + " WHERE number='" + accountNumber + "';";
			st.executeUpdate(query);
			
			//Withdraw from cash accounts, charge credit cards
			String type = (tableName.compareTo("account") == 0) ? "Withdraw" : "Charge";
			st = conn.createStatement();
			query = "INSERT INTO history(transaction_type_id, account_number, amount, datetime) "
					+ "VALUES((SELECT id FROM transaction_type WHERE `type`='"+type+"') ,'"+accountNumber+"', "+amount+", NOW());";
			st.executeUpdate(query);
			
			st = conn.createStatement();
			query = "SELECT * FROM " + tableName + " WHERE number='" + accountNumber + "';";
			ResultSet rs = st.executeQuery(query);
			while(rs.next()){				
				account.put("account number", accountNumber);
				double balance = rs.getDouble("balance");
				account.put("previous balance", balance + amount);
				account.put("current balance", balance);
			}

		} catch(Exception e){
			e.printStackTrace();
		} 
		
		return account;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject deposit(String accountNumber, double amount){
		JSONObject account = new JSONObject();
		
		try{
			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();

			String tableName = "account";
			if(accountNumber.length() > 10) {
				tableName = "credit_account";
			}
			
			String query = "UPDATE " + tableName + " SET balance=balance+" + amount + " WHERE number='" + accountNumber + "';";
			st.executeUpdate(query);
			
			//Deposit into cash accounts, make payments on credit cards
			String type = (tableName.compareTo("account") == 0) ? "Deposit" : "Payment";
			st = conn.createStatement();
			query = "INSERT INTO history(transaction_type_id, account_number, amount, datetime) "
					+ "VALUES((SELECT id FROM transaction_type WHERE `type`='"+type+"') ,'"+accountNumber+"', "+amount+", NOW());";
			st.executeUpdate(query);
			
			st = conn.createStatement();
			query = "SELECT * FROM " + tableName + " WHERE number='" + accountNumber + "';";
			ResultSet rs = st.executeQuery(query);
			while(rs.next()){				
				account.put("account number", accountNumber);
				double balance = rs.getDouble("balance");
				account.put("previous balance", balance - amount);
				account.put("current balance", balance);
			}

		} catch(Exception e){
			e.printStackTrace();
		} 
		
		return account;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getHistory(String accountNumber, long numDays){
		JSONObject json = new JSONObject();
		
		try{
			if(conn.isClosed()) conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement st = conn.createStatement();
	
			String query = "SELECT * "
					+ "FROM history "
					+ "LEFT JOIN (transaction_type) ON (transaction_type.id = transaction_type_id) "
					+ "WHERE account_number='" + accountNumber + "' "
					+ "AND datetime >= NOW() - INTERVAL " + numDays + " DAY;";
			ResultSet rs = st.executeQuery(query);
			
			json.put("account number", accountNumber);
			json.put("day range", numDays);
			JSONArray history = new JSONArray();
			while(rs.next()){
				JSONObject entry = new JSONObject();
				entry.put("transaction type", rs.getString("type"));
				entry.put("amount", rs.getDouble("amount"));
				entry.put("datetime", rs.getString("datetime"));
				history.add(entry);
			}
			json.put("history", history);
			
			st = conn.createStatement();
			query = "INSERT INTO history(transaction_type_id, account_number, amount, datetime) "
					+ "VALUES((SELECT id FROM transaction_type WHERE `type`='Inquiry') ,'"+accountNumber+"', "+numDays+", NOW());";
			st.executeUpdate(query);

		} catch(Exception e){
			e.printStackTrace();
		} 
		
		return json;
	}
}




