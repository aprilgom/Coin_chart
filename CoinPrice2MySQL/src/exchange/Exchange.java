package exchange;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
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
	int numOfMarket = 0;
	URL APIurl;
	Map<String, Market> markets = new HashMap<String, Market>();
	
	//Constructor
	public Exchange(String name, String APIurl) throws MalformedURLException {
		this.name = name;
		this.APIurl = new URL(APIurl);
	}
	
	public void addMarket(String coin, String base, String recentTradesSubUrl) {
		numOfMarket++;
		Market market = new Market(coin, base, this.name, recentTradesSubUrl);
		markets.put(market.coinpair, market);		
	}
	
	void renewDB() {
		
	}
	
	//거래소마다 API의 return value의 값이 상이하므로
	//데이터를 읽어들이는 방법이 거래소마다 다르게 구현되므로
	//abstract method로 선언
	abstract void getRecentTrades() throws Exception;
	
	public void run() {
		
	}
	
}
