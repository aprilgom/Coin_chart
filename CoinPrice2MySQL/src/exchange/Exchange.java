package exchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class Exchange implements Runnable{
	/*
	 * name : name of exchange
	 * numOfMarket : number of Market
	 * market : �ŷ��ҿ� �����ϴ� market ������ K : coinpair, V : Market ���� ����
	 * 			MarketŬ������ obj�� �ŷ��ҿ��� ����ϴ� ������ ��üȭ �� ������ BTC/KRW, ETH/KRW, BTC/USD ��
	 * 			���� �ٸ� coinpair�� ���� ������ �ǹ�
	 * APIurl : �ŷ��� �ü��� �������� ���� �ŷ��� API URL
	 * 			ex) Bithumb : https://api.bithumb.com/public/
	 * 				Coinone : https://api.coinone.co.kr/
	 */
	String name;
	int numOfMarket = 0, IOExceptionCount = 0;
	URL APIurl;
	Map<String, Market> markets = new HashMap<String, Market>();
	Connection connection;
	Statement st;
	File log, errLog;
	boolean usingTid = true;
	boolean shutdown = false;
	
	//Constructor
	public Exchange(String name, String APIurl) throws MalformedURLException {
		this.name = name;
		this.APIurl = new URL(APIurl);
		log = new File("./" + this.name + "/log");
		errLog = new File("./" + this.name + "/err_log");
	}
	
	void _addMarket(String coin, String base, String recentTradesSubUrl) {
		//�̹� add �� market���� üũ
		if(markets.containsKey(coin + base)) {
			System.err.println(coin + base + "market already exist in " + this.name);
			return;
		}
		numOfMarket++;
		Market market = new Market(coin, base, this.name, recentTradesSubUrl);
		market.oldJson = new File("./" +this.name + "/oldJson_" + market.coinpair);
		
		markets.put(market.coinpair, market);			
	}
	
	//�ŷ����� �ֱ� �ŷ����� DB�� ������
	void renewDB() throws IOException{
		Collection<Market> marketCollec = markets.values();
		Iterator<Market> iter = marketCollec.iterator();
		Market market = new Market();
		
		StringBuffer strBuf = new StringBuffer();
		
		BufferedWriter logOut = new BufferedWriter(new FileWriter(this.log, true));
		BufferedWriter errOut = new BufferedWriter(new FileWriter(this.errLog, true));
		Calendar cal;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/" + this.name + 
					"?autoReconnect=true&useSSL=false", "coin_chart_manager", "coin_chart");
			st = connection.createStatement();
			
			while(iter.hasNext()) {
				market = iter.next();
				
				if(market.dataRows == null)
					continue;
				
				for(int i = 0; i < market.dataRows.length - 1; i++) {
					strBuf.append("('" + market.dataRows[i].tid + "','" + market.dataRows[i].timestamp + "','"
							+ market.dataRows[i].price + "','" + market.dataRows[i].qty + "'),");
				}
				strBuf.append("('" + market.dataRows[market.dataRows.length - 1].tid + "','" + market.dataRows[market.dataRows.length - 1].timestamp + "','"
						+ market.dataRows[market.dataRows.length - 1].price + "','" + market.dataRows[market.dataRows.length -1 ].qty + "');");
				
				st.executeUpdate("insert into " + market.coinpair + " values " + strBuf.toString());
				System.out.println(this.name + " mysql query success : " + "insert into " + market.coinpair + " values " + strBuf.toString());
				
				cal = Calendar.getInstance();				
				logOut.append(sdf.format(cal.getTime()) + " mysql query success : " + "insert into " + market.coinpair + " values " + strBuf.toString() + "\n");
				strBuf.delete(0, strBuf.length());
			}			
		} catch(SQLException se1) {
			System.err.println(this.name +" : mysql query failed : insert into " + market.coinpair + " values " + strBuf.toString());
			System.err.println("oldJson :" + market.oldJsonRecentTrades);
			System.err.println("newJson :" + market.jsonRecentTrades);
			
			cal = Calendar.getInstance();
			errOut.append(sdf.format(cal.getTime()) + " mysql query failed : insert into " + market.coinpair + " values " + strBuf.toString() +
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

			strBuf.delete(0, strBuf.length());
		}
	}
	
	public void saveOldJson(){}
	
	//�ŷ��Ҹ��� API�� return value�� ���� �����ϹǷ�
	//�����͸� �о���̴� ����� �ŷ��Ҹ��� �ٸ��� �����ǹǷ�
	//abstract method�� ����
	//APIȣ���� �̿��Ͽ� market�� �ֱ� �ŷ��� �о� �ߺ� ������ �о��� ����� �ߺ��Ǵ� �κ� ���� ��
	//market�� dataRows�� ���� ������ renewDB()���� �̷����
	abstract void getRecentTrades() throws Exception;
	
	//convert market.jsonRecentTrades to market.dataRows
	void makeDataRows(Market market) {
		DataRow[] dataRows = json2DataRows(market.jsonRecentTrades);
		if(dataRows == null) {
			return;
		}
		List<DataRow> tmp = new ArrayList<DataRow>();
		long maxTid;
			
		//���� DB�� ����� ����ū tid ���� �о� ���ο� �ŷ��� ����Ʈ�� �߰�
		maxTid = market.lastTid;
		for(int i = 0; i < dataRows.length; i++) {
			if(dataRows[i].tid > maxTid) {
				tmp.add(dataRows[i]);
				
				//market ��ü�� lastTid ����
				if(i == 0)
					market.lastTid = dataRows[i].tid;
			}
			else
				break;
		}
		
		if(tmp.isEmpty()) {
			market.dataRows = null;
			return;
		}
		else {
			market.dataRows = tmp.toArray(new DataRow[0]);
			return;
		}
	}
	
	abstract DataRow[] json2DataRows(String json);
	
	public abstract void addMarket(String coin, String base);
	
	//getRecentTrades ����ȣ��
	public void run(){
		if(!usingTid) {
			Collection<Market> marketCollection = markets.values();
			Iterator<Market> iter = marketCollection.iterator();
			Market market = new Market();
			String line;
			StringBuffer strBuffer = new StringBuffer();
			BufferedReader in = null;		
		
			while(iter.hasNext()) {
				market = iter.next();
				try {
					in = new BufferedReader(new FileReader(market.oldJson));
					while((line = in.readLine()) != null) {
						strBuffer.append(line);
					}
					market.oldJsonRecentTrades = strBuffer.toString();	
					strBuffer.delete(0, strBuffer.length());
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				finally {
					if(in != null)
						try {
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
		}
		
		while(true) {
			try {
				getRecentTrades();
			} catch(MalformedURLException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch(IOException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
			if(shutdown)
				break;
		}
	}
	
}
