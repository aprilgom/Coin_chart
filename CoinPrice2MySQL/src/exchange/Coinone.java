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

public class Coinone extends Exchange {

	public Coinone() throws MalformedURLException {
		super("Coinone", "https://api.coinone.co.kr/");
	}

	@Override
	void getRecentTrades() throws Exception {
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market;
		int responseCode;
		String json;
		
		Pattern p = Pattern.compile("^.*</span>(.*)</pre>.*$");
		Matcher m;
		
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
				
				m = p.matcher(response.toString());
				if(!m.find()) {
					System.err.println("Coinone API call error!");
					continue;
				}
				json = m.group(1);
				json.replaceAll("&quot;", "\"");				
				
				market.jsonRecentTrades = json;
			}
			
			makeDataRows(market);			
		}
		
		renewDB();
		
		//1분에 90회까지 API 호출 가능
		//1회 호출 당 666ms 휴식
		Thread.sleep((long)(666*this.numOfMarket));
	}

	@Override
	void makeDataRows(Market market) {
		// TODO Auto-generated method stub
		
	}

	@Override
	DataRow[] json2DataRows(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMarket(String coin, String base) {
		if(base.equals("krw")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "trades/btc");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "trades/eth");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "trades/bch");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");
	}
	
	private class Response{
		String result, errorCode, currency;
		long timestamp;
		Transaction[] completeOrders;
	}
	
	private class Transaction{
		long price, timestamp;
		double qty;
	}

}
