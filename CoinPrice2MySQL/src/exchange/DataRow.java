package exchange;

public class DataRow{
	/*
	 * date : �ŷ� ü�� �ð� GMT +9:00 �������� ����
	 * price : ü�� ����
	 * qty : ü�� ����
	 */
	String date;
	double price;
	double qty;
	
	boolean equals(DataRow dataRow) {
		return this.date.equals(dataRow.date) && this.price == dataRow.price && this.qty == dataRow.qty;
	}
}