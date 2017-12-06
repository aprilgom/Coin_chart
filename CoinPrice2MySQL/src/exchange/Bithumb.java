package exchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class Bithumb extends Exchange{
	
	public Bithumb() throws MalformedURLException{
		super("Bithumb", "https://api.bithumb.com/public/");
	}
	
	public void addMarket(String coin, String base) {
		if(base.equals("krw")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "recent_transactions/btc");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "recent_transactions/eth");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "recent_transactions/bch");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");		
	}
	
	@Override
	void getRecentTrades() throws Exception{
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market;
		int responseCode;
		int num = 100;	//요쳥 거래수 100개
		
		while(e.hasNext()) {
			market = e.next();
			URL url = new URL(this.APIurl, market.recentTradesSubUrl + "?count=" + num);
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
		}
		
		renewDB();
		
		//1초에 20회까지 API 호출 가능
		//1회 호출 당 50ms 휴식
		Thread.sleep((long)(50*this.numOfMarket));
		
	}	
	
	@Override
	void makeDataRows(Market market) {
		//최초로 데이터를 가져왔을때
		if(market.oldJsonRecentTrades.equals("null")) {
			String sec = "61";
			Pattern p = Pattern.compile("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}:[0-9]{1,2}:([0-9]{1,2})$");
			Matcher m;
			
			int ms = 100;
			market.dataRows = this.json2DataRows(market.jsonRecentTrades);
			for(int i = market.dataRows.length - 1; i >= 0; i--) {
				m = p.matcher(market.dataRows[i].date);
				m.find();
				if(sec.equals(m.group(1)));
				else {
					ms = 100;
					sec = m.group(1);
				}
				market.dataRows[i].date = market.dataRows[i].date.concat(":" + ms);
				ms++;
			}
		}
		//2번째 이후로 데이터 가져왔을 때
		//중복되는 부분 빼고 새로운 부분만
		//market.dataRows에 넣음
		else {
			Pattern p = Pattern.compile("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}:[0-9]{1,2}:([0-9]{1,2})$");
			Matcher m;
			
			DataRow[] oldRows = json2DataRows(market.oldJsonRecentTrades);
			DataRow[] newRows = json2DataRows(market.jsonRecentTrades);
			
			int i,j;
			for(i = 0; i < newRows.length - 2; i++) {
				if(newRows[i].equals(oldRows[0])) 
					if(newRows[i + 1].equals(oldRows[1])) 
						if(newRows[i + 2].equals(oldRows[2])) 
							break;				
			}
			
			//전 거래랑 달라지는 부분부터 과거 체결거래부터 초가 바뀌는 시점까지 초 이하 단위를 500부터 붙이면서 올라감
			//초가 바뀌는 시점 부터는 초 이하 단위를 100부터 붙이면서 올라감
			int ms = 500;
			String sec;
			m = p.matcher(newRows[i-1].date);
			m.find();
			sec = m.group(1);
			for(j = i - 1; j >= 0; j--) {
				m = p.matcher(newRows[j].date);
				m.find();
				if(sec.equals(m.group(1))) {
					newRows[j].date = newRows[j].date.concat(""+ms);
					ms++;
				}
				else{
					ms = 100;
					newRows[j].date = newRows[j].date.concat(""+ms);
					ms++;
				}
			}
			
			market.dataRows = newRows;
		}
	}
	
	private DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();			
		Response response = gson.fromJson(json, Response.class);
		
		Data[] datas = new Data[response.data.length];
		datas = response.data;
		
		DataRow[] dataRows = new DataRow[response.data.length];		
		
		for(int i = 0; i < dataRows.length; i++) {
			dataRows[i] = new DataRow();
			dataRows[i].date = datas[i].transaction_date;
			dataRows[i].price = datas[i].price;
			dataRows[i].qty = datas[i].units_traded;
		}
		
		return dataRows;
	}
	
	private class Response{
		@SuppressWarnings("unused")
		String status;
		Data[] data;
	}
	
	private class Data{
		@SuppressWarnings("unused")
		String transaction_date, type;
		@SuppressWarnings("unused")
		double units_traded, price, total;
	}
}
