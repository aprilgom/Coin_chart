package exchange;

import java.net.MalformedURLException;

public class Bitstamp extends Exchange {
	
	public Bitstamp() throws MalformedURLException {
		super("Bitstamp", "https://www.bitstamp.net/api/");
	}
	
	@Override
	void getRecentTrades() throws Exception{
		// TODO Auto-generated method stub

	}
	
	@Override
	void makeDataRows(Market market) {
		
	}

	@Override
	public void addMarket(String coin, String base) {
		// TODO Auto-generated method stub
		
	}

}
