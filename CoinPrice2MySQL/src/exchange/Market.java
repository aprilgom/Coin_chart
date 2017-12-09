package exchange;

import java.io.File;

public class Market {
	String coin, base, coinpair, jsonRecentTrades, oldJsonRecentTrades, exchange;
	String recentTradesSubUrl;
	DataRow[] dataRows;
	File oldJson;
	int lastMs = 100;
	long lastTid;
	
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
