package exchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import com.google.gson.Gson;

public class Korbit extends Exchange {

	public Korbit() throws MalformedURLException {
		super("Korbit", "https://api.korbit.co.kr/");
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
			
			//������ ����
			if(responseCode != 200) {
				//error ó���Ұ�
				System.err.println("Http ���� ����\n responseCode : " + responseCode);
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
		
		//1�ʿ� 1ȸ���� API ȣ�� ����
		//1ȸ ȣ�� �� 1000ms �޽�
		Thread.sleep((long)(1000*this.numOfMarket));
	}

	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();
		Data[] datas = gson.fromJson(json, Data[].class);
		
		DataRow[] dataRows = new DataRow[datas.length];
		for(int i = 0; i < datas.length; i++) {
			dataRows[i] = new DataRow(datas[i].tid, (long)datas[i].timestamp/1000, datas[i].price, datas[i].amount);
		}
		
		return dataRows;
	}

	@Override
	public void addMarket(String coin, String base) {
		if(base.equals("krw")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "v1/transactions?currency_pair=btc_krw");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "v1/transactions?currency_pair=eth_krw");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "v1/transactions?currency_pair=bch_krw");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		
		//���� �߰��ϴ� market ��ü�� DB�� ���� ū tid�� ����
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
		long timestamp, tid, price;
		double amount;
	}

}
