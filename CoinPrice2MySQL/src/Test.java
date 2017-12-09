
import java.net.MalformedURLException;

import exchange.Bithumb;
import exchange.Bitstamp;
import exchange.Exchange;

public class Test {

	public static void main(String[] args) throws MalformedURLException {
		// TODO Auto-generated method stub		
		
		Exchange bithumb = new Bithumb();
		Exchange bitstamp = new Bitstamp();
		
		bithumb.addMarket("btc", "krw");
		bithumb.addMarket("eth", "krw");
		bithumb.addMarket("bch", "krw");
		
		bitstamp.addMarket("btc", "usd");
		bitstamp.addMarket("eth", "usd");
		bitstamp.addMarket("bch", "usd");
		
		(new Thread(bithumb)).start();
		(new Thread(bitstamp)).start();		
		
	}

}
