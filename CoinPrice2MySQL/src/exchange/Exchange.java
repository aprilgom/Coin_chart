package exchange;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Exchange implements Runnable{
	/*
	 * name : name of exchange
	 * numOfMarket : number of Market
	 * market : 거래소에 존재하는 market 종류를 K : coinpair, V : Market 으로 묶음
	 * 			Market클래스의 obj는 거래소에서 취급하는 시장을 객체화 한 것으로 BTC/KRW, ETH/KRW, BTC/USD 등
	 * 			서로 다른 coinpair에 대한 시장을 의미
	 * APIurl : 거래소 시세를 가져오기 위한 거래소 API URL
	 * 			ex) Bithumb : https://api.bithumb.com/public/
	 * 				Coinone : https://api.coinone.co.kr/
	 */
	String name;
	int numOfMarket = 0, IOExceptionCount = 0;
	URL APIurl;
	Map<String, Market> markets = new HashMap<String, Market>();
	private Connection connection;
	private Statement st;
	
	//Constructor
	public Exchange(String name, String APIurl) throws MalformedURLException {
		this.name = name;
		this.APIurl = new URL(APIurl);
	}
	
	void _addMarket(String coin, String base, String recentTradesSubUrl) {
		//이미 add 된 market인지 체크
		if(markets.containsKey(coin + base)) {
			System.err.println(coin + base + "market already exist in " + this.name);
			return;
		}
		numOfMarket++;
		Market market = new Market(coin, base, this.name, recentTradesSubUrl);
		markets.put(market.coinpair, market);			
	}
	
	//거래소의 최근 거래들을 DB에 갱신함
	void renewDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql:localhost", "coin_chart_manager", "coin_chart");
			st = connection.createStatement();
		
			StringBuffer values = new StringBuffer();
			
			Collection<Market> marketCollection = markets.values();
			Iterator<Market> e = marketCollection.iterator();
			Market market;
			while(e.hasNext()) {
				market = e.next();
				for(int i = 0; i < market.dataRows.length; i++) {
					values.append("('" + market.dataRows[i].date + "','" + market.dataRows[i].price +
							"','" + market.dataRows[i].qty + "')");
				}
				st.executeUpdate("insert into " + this.name + " values " + values.toString());
				values.delete(0, values.length() - 1);
			}
			st.close();
			connection.close();
		}catch(SQLException se1) {
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
	
	//거래소마다 API의 return value의 값이 상이하므로
	//데이터를 읽어들이는 방법이 거래소마다 다르게 구현되므로
	//abstract method로 선언
	//API호출을 이용하여 market의 최근 거래를 읽어 중복 그전에 읽었던 내용과 중복되는 부분 제거 후
	//market의 dataRows를 새로 갱신함 renewDB()까지 이루어짐
	abstract void getRecentTrades() throws Exception;
	
	//convert market.jsonRecentTrades to market.dataRows
	//getRecentTrades()와 동일한 이유로 abstract로 선언
	abstract void makeDataRows(Market market);
	
	public abstract void addMarket(String coin, String base);
	
	//getRecentTrades 무한호출
	public void run(){
		while(true) {
			try {
				getRecentTrades();
			} catch(MalformedURLException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			} catch(IOException e) {					
				System.err.println(e.getMessage());
				System.err.println("IOException count : " + this.IOExceptionCount);
				if(this.IOExceptionCount > 20)
					System.exit(-1);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
}
