package exchange;

import java.util.Calendar;

public class Market {
	String coin, base, coinpair, jsonRecentTrades, exchange;
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
	
	class DataRow{
		/*
		 * date : 거래 체결 시간 GMT +9:00 기준으로 저장
		 * price : 체결 가격
		 * qty : 체결 수량
		 */
		Calendar date;
		double price;
		double qty;
	}
}
