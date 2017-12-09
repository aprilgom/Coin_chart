package exchange;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class Coinone extends Exchange {

	public Coinone() throws MalformedURLException {
		super("Coinone", "https://api.coinone.co.kr/");
	}

	@Override
	void getRecentTrades() throws Exception {
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market;
		int responseCode;
		String json;
		
		Pattern p = Pattern.compile("^.*</span>(.*)</pre>.*$");
		Matcher m;
		
		while(e.hasNext()) {
			market = e.next();
			URL url = new URL(this.APIurl, market.recentTradesSubUrl);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			
			uc.setRequestMethod("GET");
			
			responseCode = uc.getResponseCode();
			
			//비정상 응답
			if(responseCode != 200) {
				//error 처리할것
				System.err.println("Http 응답 실패\n responseCode : " + responseCode);
				System.err.println("url : " + url);
				System.exit(-1);
			}
			else {
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String inLine;
				StringBuffer response = new StringBuffer();
				while((inLine = in.readLine()) != null)
					response.append(inLine);
				in.close();
				
				m = p.matcher(response.toString());
				if(!m.find()) {
					SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					System.err.println("Coinone " + market.coinpair + " API call error!");
					PrintWriter out = new PrintWriter(new FileWriter(errLog));
					out.println(sdt.format(Calendar.getInstance().getTime()) + " Coinone " + market.coinpair + " API call error!");
					out.close();
					this.shutdown = true;
					return;
				}
				json = m.group(1);
				json = json.replace("&quot;","\"");
				
				market.jsonRecentTrades = json;
			}
			
			makeDataRows(market);			
		}
		
		renewDB();
		
		saveOldJson();
		e = marketCollection.iterator();
		while(e.hasNext()) {
			market = e.next();
			market.oldJsonRecentTrades = market.jsonRecentTrades;			
		}
		
		//1분에 90회까지 API 호출 가능
		//1회 호출 당 666ms 휴식
		Thread.sleep((long)(666*this.numOfMarket));
	}

	@Override
	void makeDataRows(Market market) {
		long tid;
		//최초로 데이터를 가져왔을때
		if(market.oldJsonRecentTrades.equals("")) {
			market.dataRows = this.json2DataRows(market.jsonRecentTrades);
			tid = market.lastTid + 1;
			for(int i = 0; i < market.dataRows.length; i++) {
				market.dataRows[i].tid = tid;
				tid++;
			}
			market.lastTid = tid - 1;
			
		}
		
		//2번째 이후로 데이터 가져왔을 때
		//중복되는 부분 빼고 새로운 부분만
		//market.dataRows에 넣음
		else {
			
			DataRow[] oldRows = json2DataRows(market.oldJsonRecentTrades);
			DataRow[] newRows = json2DataRows(market.jsonRecentTrades);		
			
			//api 호출 시 간헐적으로 이전 데이터들을 가져오는 경우가 있어
			//새로 읽은 데이터들이 더 과거의 데이터인지 확인
			boolean[] conts = new boolean[10];	//newJson 의 데이터가 oldJson 보다 과거일 경우
			boolean cont = false;				//conts[i], cont = true
			int oldCount, newCount, k;
			for(oldCount = oldRows.length - 1, newCount = newRows.length - 1, k = 0; k < 10; k++, newCount--, oldCount--) {
				if(oldRows[oldCount].timestamp > newRows[newCount].timestamp)
					conts[k] = true;
			}
			
			for(k = 0; k < 10; k++)
				cont = cont | conts[k];
			
			//newJson 이 oldJson 보다 과거의 데이터이면
			if(cont) {
				market.jsonRecentTrades = market.oldJsonRecentTrades;
				market.dataRows = null;
				return;
			}
			
			for(oldCount = oldRows.length - 1, newCount = newRows.length - 1; oldCount >= 0 && newCount >= 0; oldCount--, newCount--) {
				if(oldRows[oldCount].equalsWTid(newRows[newCount])) 
					break;				
			}
			
			//다시 같은 데이터 읽은 경우
			if(newCount == newRows.length - 1) {
				market.dataRows = null;
				return;
			}
			
			tid = market.lastTid + 1;
			market.dataRows = new DataRow[newRows.length - newCount - 1];
			int i;
			for(i = 0, k = newCount + 1; k < newRows.length; k++, i++) {
				newRows[k].tid = tid;
				market.dataRows[i] = newRows[k];
				tid++;
			}
			market.lastTid = tid - 1;						
		}
		
	}

	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();
		Response response = gson.fromJson(json, Response.class);
		
		DataRow[] dataRows = new DataRow[response.completeOrders.length];
		for(int i = 0; i < response.completeOrders.length; i++) {
			dataRows[i] = new DataRow(response.completeOrders[i].timestamp, (double)response.completeOrders[i].price, response.completeOrders[i].qty);
		}
		
		return dataRows;
	}

	@Override
	public void addMarket(String coin, String base) {
		if(base.equals("krw")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "trades/?currency=btc");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "trades/?currency=eth");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "trades/?currency=bch");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		
		//현재 추가하는 market 객체에 DB상에 가장 큰 tid를 저장
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/" + this.name + 
						"?autoReconnect=true&useSSL=false", "coin_chart_manager", "coin_chart");
			st = connection.createStatement();
			
			Market market = markets.get(coin + base);
			
			ResultSet rs = st.executeQuery("select max(tid) from " + market.coinpair);
			
			if(!rs.next()) {
				System.err.println("query max(tid) error!");
				System.exit(-1);
			}
			
			market.lastTid = rs.getInt("max(tid)");
		} catch(SQLException se1) {
			se1.printStackTrace();
		}catch(Exception ex) {
			ex.printStackTrace();
		}finally {
			try {
				if(st!=null)
					st.close();
			}catch(SQLException se2) {
				se2.printStackTrace();
			}
			try {
				if(connection!=null)
					connection.close();
			}catch(SQLException se3) {
				se3.printStackTrace();
			}
		}
	}
	
	public void saveOldJson(){
		FileOutputStream out = null;
		
		Collection<Market> marketCol = markets.values();
		Iterator<Market> iter = marketCol.iterator();
		Market market;
		
		while(iter.hasNext()) {
			market = iter.next();
			try {
				out = new FileOutputStream(market.oldJson, false);
				out.getChannel().truncate(0);
				out.getChannel().force(true);
				out.getChannel().lock();
				out.write(market.jsonRecentTrades.getBytes(),0,market.jsonRecentTrades.length());
				out.close();
			}catch(IOException e) {
				e.printStackTrace();
			}finally {
				if(out != null)
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
	}
	
	private class Response{
		@SuppressWarnings("unused")
		String result, errorCode, currency;
		@SuppressWarnings("unused")
		long timestamp;
		Transaction[] completeOrders;
	}
	
	private class Transaction{
		long price, timestamp;
		double qty;
	}

}
