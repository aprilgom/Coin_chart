package exchange;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class Coincheck extends Exchange {

	public Coincheck() throws MalformedURLException {
		super("Coincheck", "https://coincheck.com/api/");
	}

	@Override
	void getRecentTrades() throws Exception {
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market;
		int responseCode;
		
		while(e.hasNext()) {
			market = e.next();
			URL url = new URL(this.APIurl, market.recentTradesSubUrl);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			
			uc.setRequestMethod("GET");
			
			responseCode = uc.getResponseCode();
			
			//비정상 응답
			if(responseCode != 200) {
				//error 처리할것
				System.err.println("Http 응답 실패\n responseCode : " + responseCode);
				System.err.println("url : " + url);
				System.exit(-1);
			}
			else {
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String inLine;
				StringBuffer response = new StringBuffer();
				while((inLine = in.readLine()) != null)
					response.append(inLine);
				in.close();
				
				market.jsonRecentTrades = response.toString();
			}
			
			makeDataRows(market);			
		}
		
		renewDB();
		
		//1회 호출 당 500ms 휴식
		Thread.sleep((long)(500*this.numOfMarket));

	}

	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();
		Data[] datas = gson.fromJson(json, Data[].class);
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
		
		Pattern p = Pattern.compile("^([0-9]{4})-([0-9]{1,2})-([0-9]{1,2})T([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})\\.000Z$");
		Matcher m;
		
		DataRow[] dataRows = new DataRow[datas.length];
		for(int i = 0; i < datas.length; i++) {
			m = p.matcher(datas[i].created_at);
			if(!m.find()) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					PrintWriter out = new PrintWriter(new FileWriter(errLog));
					
					System.err.println("Coincheck date doesn't match with pattern(yyyy-MM-ddTHH:mm:ss.000Z)");
					System.err.println("see the json2DataRows in Coincheck Class");
					out.println(sdf.format(Calendar.getInstance().getTime()) + "Coincheck date doesn't match with pattern(yyyy-MM-ddTHH:mm:ss.000Z)");
					out.println("\tsee the json2DataRows in Coincheck Class");
					out.close();
					shutdown = true;
					return null;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			cal.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
					Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
			dataRows[i] = new DataRow(datas[i].id, (long)cal.getTimeInMillis()/1000, datas[i].rate, datas[i].amount);
		}
		
		return dataRows;
	}

	@Override
	public void addMarket(String coin, String base) {
		// TODO Auto-generated method stub
		if(base.equals("jpy")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "trades");			
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		
		//현재 추가하는 market 객체에 DB상에 가장 큰 tid를 저장
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/" + this.name + 
						"?autoReconnect=true&useSSL=false", "coin_chart_manager", "coin_chart");
			st = connection.createStatement();
			
			Market market = markets.get(coin + base);
			
			ResultSet rs = st.executeQuery("select max(tid) from " + market.coinpair);
			
			if(!rs.next()) {
				System.err.println("query max(tid) error!");
				System.exit(-1);
			}
			
			market.lastTid = rs.getInt("max(tid)");
		} catch(SQLException se1) {
			se1.printStackTrace();
		}catch(Exception ex) {
			ex.printStackTrace();
		}finally {
			try {
				if(st!=null)
					st.close();
			}catch(SQLException se2) {
				se2.printStackTrace();
			}
			try {
				if(connection!=null)
					connection.close();
			}catch(SQLException se3) {
				se3.printStackTrace();
			}
		}
	}
	
	private class Data{
		long id, rate;
		double amount;
		@SuppressWarnings("unused")
		String order_type, created_at;
	}

}
