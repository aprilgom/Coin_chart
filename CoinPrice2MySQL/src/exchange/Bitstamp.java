package exchange;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import com.google.gson.Gson;

public class Bitstamp extends Exchange {
	
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
		FileOutputStream out = null;
		
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
			market.oldJsonRecentTrades = market.jsonRecentTrades;
			out = new FileOutputStream(market.oldJson, false);
			out.getChannel().truncate(0);
			out.getChannel().force(true);
			out.getChannel().lock();
			out.write(market.jsonRecentTrades.getBytes(),0,market.jsonRecentTrades.length());
			out.close();
		}
		
		renewDB();
		
		//1초에 20회까지 API 호출 가능
		//1회 호출 당 50ms 휴식
		Thread.sleep((long)(1000*this.numOfMarket));
	}
	
	@Override
	void makeDataRows(Market market) {
		
	}
	
	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();
		Response response = gson.fromJson(json, Response.class);
		Data[] datas = response.datas;
		
		DataRow[] dataRows = new DataRow[datas.length];
		for(in i = 0; i < datas.length; i++) {
			dataRows[i] = new DataRow()
		}
	}

	@Override
	public void addMarket(String coin, String base) {
		// TODO Auto-generated method stub
		if(base.equals("usd")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "v2/trasactions/btcusd");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "v2/trasactions/ethusd");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "v2/trasactions/bchusd");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");
	}
	private class Response{
		Data[] datas;
	}
	private class Data{
		long date, tid;
		double price, amount;
		int type;	// 0: buy, 1: sell
		
	}

}
