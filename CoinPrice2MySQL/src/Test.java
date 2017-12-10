
import java.net.MalformedURLException;

import exchange.*;

public class Test {

	public static void main(String[] args) throws MalformedURLException {
		Exchange bithumb = new Bithumb();
		Exchange bitstamp = new Bitstamp();
		Exchange coinone = new Coinone();
		Exchange coincheck = new Coincheck();
		Exchange korbit = new Korbit();
		
		bithumb.addMarket("btc", "krw");
		bithumb.addMarket("eth", "krw");
		bithumb.addMarket("bch", "krw");
		
		bitstamp.addMarket("btc", "usd");
		bitstamp.addMarket("eth", "usd");
		bitstamp.addMarket("bch", "usd");
		
		coinone.addMarket("btc", "krw");
		coinone.addMarket("eth", "krw");
		coinone.addMarket("bch", "krw");
		
		coincheck.addMarket("btc", "jpy");
		
		korbit.addMarket("btc", "krw");
		korbit.addMarket("eth", "krw");
		korbit.addMarket("bch", "krw");
		
		
		(new Thread(bithumb, "Thread-Bithumb")).start();
		(new Thread(bitstamp, "Thread-Bistamp")).start();	
		(new Thread(coinone, "Thread-Coinone")).start();
		(new Thread(coincheck, "Thread-Coincheck")).start();
		(new Thread(korbit, "Thread-Korbit")).start();
	}

}
