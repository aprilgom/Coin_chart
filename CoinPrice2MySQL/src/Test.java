
import java.net.MalformedURLException;

import exchange.Bithumb;
import exchange.Exchange;

public class Test {

	public static void main(String[] args) throws MalformedURLException {
		// TODO Auto-generated method stub
		Exchange bithumb = new Bithumb();
		bithumb.addMarket("btc", "krw");
		bithumb.addMarket("eth", "krw");
		bithumb.addMarket("bch", "krw");
		(new Thread(bithumb)).start();
	}

}
