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
		this.usingTid = false;
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
			
			//������ ����
			if(responseCode != 200) {
				//error ó���Ұ�
				System.err.println("Http ���� ����\n responseCode : " + responseCode);
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
					shutdown = true;
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
		
		//1�п� 90ȸ���� API ȣ�� ����
		//1ȸ ȣ�� �� 666ms �޽�
		Thread.sleep((long)(666*this.numOfMarket));
	}

	@Override
	void makeDataRows(Market market) {
		long tid;
		//���ʷ� �����͸� ����������
		if(market.oldJsonRecentTrades.equals("")) {
			market.dataRows = this.json2DataRows(market.jsonRecentTrades);
			tid = market.lastTid + 1;
			for(int i = 0; i < market.dataRows.length; i++) {
				market.dataRows[i].tid = tid;
				tid++;
			}
			market.lastTid = tid - 1;
			
		}
		
		//2��° ���ķ� ������ �������� ��
		//�ߺ��Ǵ� �κ� ���� ���ο� �κи�
		//market.dataRows�� ����
		else {
			
			DataRow[] oldRows = json2DataRows(market.oldJsonRecentTrades);
			DataRow[] newRows = json2DataRows(market.jsonRecentTrades);		
			
			//api ȣ�� �� ���������� ���� �����͵��� �������� ��찡 �־�
			//���� ���� �����͵��� �� ������ ���������� Ȯ��
			boolean[] conts = new boolean[10];	//newJson �� �����Ͱ� oldJson ���� ������ ���
			boolean cont = false;				//conts[i], cont = true
			int oldCount, newCount, k;
			for(oldCount = oldRows.length - 1, newCount = newRows.length - 1, k = 0; k < 10; k++, newCount--, oldCount--) {
				if(oldRows[oldCount].timestamp > newRows[newCount].timestamp)
					conts[k] = true;
			}
			
			for(k = 0; k < 10; k++)
				cont = cont | conts[k];
			
			//newJson �� oldJson ���� ������ �������̸�
			if(cont) {
				market.jsonRecentTrades = market.oldJsonRecentTrades;
				market.dataRows = null;
				return;
			}
			
			for(newCount = newRows.length - 1; newCount >= 0; newCount--) {
				if(oldRows[oldRows.length - 1].equalsWTid(newRows[newCount])) 
					break;				
			}
						
			//�ٽ� ���� ������ ���� ���
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
		
		//���� �߰��ϴ� market ��ü�� DB�� ���� ū tid�� ����
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
