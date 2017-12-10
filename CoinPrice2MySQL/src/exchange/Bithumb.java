package exchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class Bithumb extends Exchange{
	
	public Bithumb() throws MalformedURLException{
		super("Bithumb", "https://api.bithumb.com/public/");
		this.usingTid = false;
	}
	
	public void addMarket(String coin, String base) {
		if(base.equals("krw")) {
			if(coin.equals("btc")) 
				_addMarket(coin, base, "recent_transactions/btc");			
			else if(coin.equals("eth"))
				_addMarket(coin, base, "recent_transactions/eth");
			else if(coin.equals("bch"))
				_addMarket(coin, base, "recent_transactions/bch");
			else 
				System.err.println(this.name + "has no " + coin + "/" + base + " market!");
		}
		else 
			System.err.println(this.name + "has no " + coin + "/" + base + " market!");		
	}
	
	@Override
	void getRecentTrades() throws Exception{
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market;
		int responseCode;
		int num = 100;	//요쳥 거래수 100개
		
		while(e.hasNext()) {
			market = e.next();
			URL url = new URL(this.APIurl, market.recentTradesSubUrl + "?count=" + num);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			
			uc.setRequestMethod("GET");
			
			responseCode = uc.getResponseCode();
			
			//비정상 응답
			if(responseCode != 200) {
				//error 처리할것
				System.err.println("Http 응답 실패\n responseCode : " + responseCode);
				System.exit(-1);
			}
			else {
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String inLine;
				StringBuffer response = new StringBuffer();
				while((inLine = in.readLine()) != null)
					response.append(inLine);
				in.close();
				
				market.jsonRecentTrades = response.toString();
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
		
		//1초에 20회까지 API 호출 가능
		//1회 호출 당 50ms 휴식
		Thread.sleep((long)(50*this.numOfMarket));
		
	}	
	
	//빗썸의 경우 타임스탬프 형식으로  date를 저장하지 않으므로 재정의
	@Override
	void renewDB() throws IOException{
		StringBuffer values = new StringBuffer();
		
		Collection<Market> marketCollection = markets.values();
		Iterator<Market> e = marketCollection.iterator();
		Market market = new Market();
		BufferedWriter logOut = new BufferedWriter(new FileWriter(log, true));
		BufferedWriter errOut = new BufferedWriter(new FileWriter(errLog, true));
		int i;
		
		Calendar cal;
		SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/bithumb?autoReconnect=true&useSSL=false", "coin_chart_manager", "coin_chart");
			st = connection.createStatement();						
			
			while(e.hasNext()) {
				market = e.next();
				if(market.dataRows == null) {
					continue;
				}
				for(i = 0; i < market.dataRows.length - 1; i++) {
					values.append("('" + market.dataRows[i].date + "','" + market.dataRows[i].price +
							"','" + market.dataRows[i].qty + "'),");
				}
				values.append("('" + market.dataRows[i].date + "','" + market.dataRows[i].price +
						"','" + market.dataRows[i].qty + "');");
				st.executeUpdate("insert into " + market.coinpair + " values " + values.toString());
				System.out.println("Bithumb mysql query success : insert into " + market.coinpair + " values " + values.toString());
				
				cal = Calendar.getInstance();
				logOut.append(sdt.format(cal.getTime()) +" mysql query success : insert into " + market.coinpair + " values " + values.toString() + "\n");
				values.delete(0, values.length());
			}
			st.close();
			connection.close();
		}catch(SQLException se1) {
			System.err.println(this.name +" : mysql query failed : insert into " + market.coinpair + " values " + values.toString());
			System.err.println("oldJson :" + market.oldJsonRecentTrades);
			System.err.println("newJson :" + market.jsonRecentTrades);
			
			cal = Calendar.getInstance();
			errOut.append(sdt.format(cal.getTime()) + " mysql query failed : insert into " + market.coinpair + " values " + values.toString() +
					"\noldJson :" + market.oldJsonRecentTrades + "\nnewJson :" + market.jsonRecentTrades + "\n");
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
			if(logOut != null) {
				logOut.close();
			}
			if(errOut != null) {
				errOut.close();
			}
			
			values.delete(0, values.length());
		}
	}
	
	@Override
	void makeDataRows(Market market) {
		//최초로 데이터를 가져왔을때
		if(market.oldJsonRecentTrades.equals("")) {
			String sec = "61";
			Pattern p = Pattern.compile("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}:[0-9]{1,2}:([0-9]{1,2})$");
			Matcher m;
			
			int ms = 100;
			market.dataRows = this.json2DataRows(market.jsonRecentTrades);
			for(int i = market.dataRows.length - 1; i >= 0; i--) {
				m = p.matcher(market.dataRows[i].date);
				m.find();
				if(sec.equals(m.group(1)));
				else {
					ms = 100;
					sec = m.group(1);
				}
				market.dataRows[i].date = market.dataRows[i].date.concat(":" + ms);
				ms++;
			}
			market.lastMs = ms;
		}
		//2번째 이후로 데이터 가져왔을 때
		//중복되는 부분 빼고 새로운 부분만
		//market.dataRows에 넣음
		else {
			
			DataRow[] oldRows = json2DataRows(market.oldJsonRecentTrades);
			DataRow[] newRows = json2DataRows(market.jsonRecentTrades);		
			
			//bithumb api 호출 시 간헐적으로 이전 데이터들을 가져오는 경우가 있어
			//새로 읽은 데이터들이 더 과거의 데이터인지 확인
			boolean[] conts = new boolean[10];	//newJson 의 데이터가 oldJson 보다 과거일 경우
			boolean cont = false;				//conts[i], cont = true
			int k;	
			Calendar oldDate = Calendar.getInstance();
			Calendar newDate = Calendar.getInstance();
			for(k = 0; k < 10; k++) {
				oldRows[k].parseDate();
				newRows[k].parseDate();
				
				oldDate.set(oldRows[k].year, oldRows[k].month, oldRows[k].day, oldRows[k].hour, oldRows[k].min, oldRows[k].sec);
				newDate.set(newRows[k].year, newRows[k].month, newRows[k].day, newRows[k].hour, newRows[k].min, newRows[k].sec);
				
				if(oldDate.compareTo(newDate) > 0)
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
			
			int i,j;
			for(i = 0; i < newRows.length; i++) {
				if(newRows[i].equals(oldRows[0])) 
					break;				
			}
			
			if(i == 0) {
				market.dataRows = null;
				return;
			}
			
			//전 거래랑 달라지는 부분부터 과거 체결거래부터 초가 바뀌는 시점까지 초 이하 단위를 500부터 붙이면서 올라감
			//초가 바뀌는 시점 부터는 초 이하 단위를 100부터 붙이면서 올라감
			int ms = market.lastMs;
			int sec;
			
			sec = newRows[i-1].sec;
			for(j = i - 1; j >= 0; j--) {
				if(sec == newRows[j].sec) {
					newRows[j].date = newRows[j].date.concat(":"+ms);
					ms++;
				}
				else{
					ms = 100;
					sec = newRows[j].sec;
					newRows[j].date = newRows[j].date.concat(":"+ms);
					ms++;
				}
			}
			market.lastMs = ms;
			market.dataRows = new DataRow[i];
			for(j = 0; j < i; j ++) {
				market.dataRows[j] = newRows[j];
			}
		}
	}
	
	@Override
	DataRow[] json2DataRows(String json) {
		Gson gson = new Gson();			
		Response response = gson.fromJson(json, Response.class);
		
		Data[] datas = response.data;
		
		DataRow[] dataRows = new DataRow[response.data.length];		
		
		for(int i = 0; i < dataRows.length; i++) {
			dataRows[i] = new DataRow(datas[i].transaction_date, datas[i].price, datas[i].units_traded);
		}
		
		return dataRows;
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
		String status;
		Data[] data;
	}
	
	private class Data{
		@SuppressWarnings("unused")
		String transaction_date, type;
		@SuppressWarnings("unused")
		double units_traded, price, total;
	}
}
