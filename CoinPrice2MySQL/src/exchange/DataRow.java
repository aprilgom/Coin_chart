package exchange;

public class DataRow{
	/*
	 * date : 거래 체결 시간 GMT +9:00 기준으로 저장
	 * price : 체결 가격
	 * qty : 체결 수량
	 */
	String date;
	double price;
	double qty;
	
	public DataRow(String date, double price, double qty) {
		this.date = date;
		this.price = price;
		this.qty = qty;
	}
	
	boolean equals(DataRow dataRow) {
		return this.date.equals(dataRow.date) && this.price == dataRow.price && this.qty == dataRow.qty;
	}
}