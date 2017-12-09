package exchange;

import java.io.File;

public class Market {
	String coin, base, coinpair, jsonRecentTrades, oldJsonRecentTrades, exchange;
	String recentTradesSubUrl;
	DataRow[] dataRows;
	File oldJson;
	int lastMs = 100;	//bithumb에서  DB에저장할 떄 추가할 ms
	long lastTid;	//DB에 저장된 가장 큰 tid
	
	//Constructor
	public Market(String coin, String base, String exchange, String recentTradesSubUrl) {
		this.coin = coin;
		this.base = base;
		this.coinpair = coin.concat(base);
		this.exchange = exchange;
		this.recentTradesSubUrl = recentTradesSubUrl;
	}	
	
	public Market() {
	}
}
