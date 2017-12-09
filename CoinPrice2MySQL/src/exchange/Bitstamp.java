package exchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;

public class Bitstamp extends Exchange {
	
	//constructor
	public Bitstamp() throws MalformedURLException {
		super("Bitstamp", "https://www.bitstamp.net/api/");
	}
	
	@Override
	void getRecentTrades() throws Exception{
		// TODO Auto-generated method stub
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
		
		//1초에 1회까지 API 호출 가능
		//1회 호출 당 1000ms 휴식
		Thread.sleep((long)(1000*this.numOfMarket));
	}
	
	@Override
	void makeDataRows(Market market) {
		DataRow[] dataRows = json2DataRows(market.jsonRecentTrades);
		List<DataRow> tmp = new ArrayList<DataRow>();
		long maxTid;
			
		//현재 DB에 저장된 가장큰 tid 값을 읽어 새로운 거래만 리스트에 추가
		maxTid = market.lastTid;
		for(int i = 0; i < dataRows.length; i++) {
			if(dataRows[i].tid > maxTid) {
				tmp.add(dataRows[i]);
				
				//market 객체의 lastTid 갱신
				if(i == 0)
					market.lastTid = dataRows[i].tid;
			}
			else
				break;
		}
		
		if(tmp.isEmpty()) {
			market.dataRows = null;
			return;
		}
		else {
			market.dataRows = tmp.toArray(new DataRow[0]);
			return;
		}
	}
	
	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();
		Data[] datas = gson.fromJson(json, Data[].class);
		
		DataRow[] dataRows = new DataRow[datas.length];
		for(int i = 0; i < datas.length; i++) {
			dataRows[i] = new DataRow(datas[i].tid,datas[i].date, datas[i].price, datas[i].amount);
		}
		
		return dataRows;
	}

	@Override
	public void addMarket(String coin, String base) {
		// TODO Auto-generated method stub
		if(base.equals("usd")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "v2/transactions/btcusd");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "v2/transactions/ethusd");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "v2/transactions/bchusd");
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
		long date, tid;
		double price, amount;
		@SuppressWarnings("unused")
		int type;	// 0: buy, 1: sell
		
	}

}
