package exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

public class Bithumb extends Exchange{
	
	public Bithumb() throws MalformedURLException{
		super("Bithumb", "https://api.bithumb.com/public/");
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
				
				json2DataRows(market);
			}
		}
		
		//1초에 20회까지 API 호출 가능
		//1회 호출 당 50ms 휴식
		Thread.sleep((long)(50*this.numOfMarket));
		
	}

	private void json2DataRows(Market market) {
		//json->DataRows로 변환
	}

}
