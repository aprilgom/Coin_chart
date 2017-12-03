<?PHP
	$conn = mysqli_connect('localhost','root','password');
	
	$db_status = mysqli_select_db($conn, "coin_chart");
	if(!$db_status)
		die("DB_ERROR");
	
	$query = "SELECT * FROM min_test";
	$result = mysqli_query($conn,$query);
	$arr = $result->fetch_all(MYSQLI_NUM);
	$candle_data = array(array("date", "qty","test", "","",""));
	$i = 0;
	foreach($arr as $row){
		preg_match("/([0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}\\:[0-9]{1,2})\\:[0-9]{1,2}/",$row[0],$date);
		$row[0] = $date[1];
		$row[1] = (double)$row[1];
		$row[2] = (double)$row[2];
		$row[3] = (double)$row[3];
		$row[4] = (double)$row[4];
		$row[5] = (double)$row[5];
		$arr[$i] = $row;
		$i++;
	}
	$i = 1;
	foreach($arr as $row){
		$candle_data[$i] = $row;
		$i++;
	}
	$json = json_encode($candle_data);
	$num = preg_match_all("/\"([0-9]{4})-([0-9]{1,2})-([0-9]{1,2}).([0-9]{1,2})\\:([0-9]{1,2})\"/", $json, $date);
	for($i = 0; $i < $num; $i++){
		$json = preg_replace("/".$date[0][$i]."/","new Date(".$date[1][$i].",".$date[2][$i].",".$date[3][$i].",".$date[4][$i].",".$date[5][$i].")",$json);
	}
?>
<html>
  <head>
    <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
	<script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
    <script type="text/javascript">
      google.charts.load('current', {'packages':['corechart']});
      google.charts.setOnLoadCallback(drawVisualization);

      function drawVisualization() {
        
		
		
        var data = google.visualization.arrayToDataTable(<?=$json?>);

		var options = {
			vAxis: {title: 'price'},
			hAxis: {title: 'minute'},
			seriesType: 'candlesticks',
			series: { 0:{type:'bars'}},
			candlestick:{	fallingColor:{fill: 'blue', stroke: 'blue'},
							risingColor:{fill: 'red', stroke: 'red'}
					}
			};
		
		var chart = new google.visualization.ComboChart(document.getElementById('chart_div'));
		chart.draw(data, options);	    
	}
    </script>
  </head>
  <body>
    <div id="chart_div" style="width: 100%; height: 100%;"></div>
  </body>
</html>