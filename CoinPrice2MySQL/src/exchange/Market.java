package exchange;

public class Market {
	String coin, base, coinpair, jsonRecentTrades, oldJsonRecentTrades = "null", exchange;
	String recentTradesSubUrl;
	DataRow[] dataRows;
	
	//Constructor
	public Market(String coin, String base, String exchange, String recentTradesSubUrl) {
		this.coin = coin;
		this.base = base;
		this.coinpair = coin.concat(base);
		this.exchange = exchange;
		this.recentTradesSubUrl = recentTradesSubUrl;
	}	
}
