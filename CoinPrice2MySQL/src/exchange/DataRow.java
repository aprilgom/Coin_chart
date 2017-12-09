package exchange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataRow{
	/*
	 * date : 거래 체결 시간 GMT +9:00 기준으로 저장
	 * price : 체결 가격
	 * qty : 체결 수량
	 */
	long tid, timestamp;
	String date;
	double price;
	double qty;
	int year, month, day, hour, min, sec;
	
	public DataRow(String date, double price, double qty) {
		this.date = date;
		this.price = price;
		this.qty = qty;
	}
	
	public DataRow(long tid, long timestamp, double price, double qty) {
		this.tid = tid;
		this.timestamp = timestamp;
		this.price = price;
		this.qty = qty;
	}
	
	public DataRow(long timestamp, double price, double qty) {
		this.timestamp = timestamp;
		this.price = price;
		this.qty = qty;
	}
	
	void parseDate() {
		Pattern p = Pattern.compile("^([0-9]{4})-([0-9]{1,2})-([0-9]{1,2}).([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})$");
		Matcher m = p.matcher(this.date);
		m.find();
		year = Integer.parseInt(m.group(1));
		month = Integer.parseInt(m.group(2));
		day = Integer.parseInt(m.group(3));
		hour = Integer.parseInt(m.group(4));
		min = Integer.parseInt(m.group(5));
		sec = Integer.parseInt(m.group(6));
	}
	
	boolean equals(DataRow dataRow) {
		this.parseDate();
		dataRow.parseDate();
		
		return this.year == dataRow.year && this.month == dataRow.month && this.day == dataRow.day && this.hour == dataRow.hour && this.min == dataRow.min
				&& this.sec == dataRow.sec && Double.compare(this.price, dataRow.price) == 0 && Double.compare(this.price, dataRow.price) == 0;
	}
	
	boolean equalsWTid(DataRow dataRow) {
		return this.timestamp == dataRow.timestamp && Double.compare(this.price, dataRow.price) == 0 && Double.compare(this.qty, dataRow.qty) == 0;
	}
}