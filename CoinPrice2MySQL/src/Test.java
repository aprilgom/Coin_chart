
import java.net.MalformedURLException;

import exchange.Bithumb;
import exchange.Bitstamp;
import exchange.Coinone;
import exchange.Exchange;

public class Test {

	public static void main(String[] args) throws MalformedURLException {
		// TODO Auto-generated method stub		
		
		Exchange bithumb = new Bithumb();
		Exchange bitstamp = new Bitstamp();
		Exchange coinone = new Coinone();
		
		bithumb.addMarket("btc", "krw");
		bithumb.addMarket("eth", "krw");
		bithumb.addMarket("bch", "krw");
		
		bitstamp.addMarket("btc", "usd");
		bitstamp.addMarket("eth", "usd");
		bitstamp.addMarket("bch", "usd");
		
		coinone.addMarket("btc", "krw");
		coinone.addMarket("eth", "krw");
		coinone.addMarket("bch", "krw");
		
		(new Thread(bithumb, "Thread-Bithumb")).start();
		(new Thread(bitstamp, "Thread-Bistamp")).start();	
		(new Thread(coinone, "Thread-Coinone")).start();		
	}

}
