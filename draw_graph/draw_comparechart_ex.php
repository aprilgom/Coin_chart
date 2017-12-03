<?PHP
	$conn = mysqli_connect('localhost','root','password');
	
	$db_status = mysqli_select_db($conn, "coin_chart");
	if(!$db_status)
		die("DB_ERROR");
    
    //get from table min_test
	$query = "SELECT date,close FROM min_test";
	$result = mysqli_query($conn,$query);
    $arr = $result->fetch_all(MYSQLI_NUM);
    $i = 0;
	foreach($arr as $row){
		preg_match("/([0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}\\:[0-9]{1,2})\\:[0-9]{1,2}/",$row[0],$date);
		$row[0] = $date[1];
        $row[1] = (double)$row[1];
        $row[2] = null;
		$arr[$i] = $row;
		$i++;
	}
	
    $json1 = json_encode($arr);
    //x축을 continuous data 로 바꾸기 위해 json포멧에서 date부분을 string에서 Date형식으로 바꿔줌
	$num = preg_match_all("/\"([0-9]{4})-([0-9]{1,2})-([0-9]{1,2}).([0-9]{1,2})\\:([0-9]{1,2})\"/", $json1, $date);
	for($i = 0; $i < $num; $i++){
		$json1 = preg_replace("/".$date[0][$i]."/","new Date(".$date[1][$i].",".$date[2][$i].",".$date[3][$i].",".$date[4][$i].",".$date[5][$i].")",$json1);
    }
    
    //get from tabel min_test1
    $query = "SELECT date,qty,close FROM min_test1";
	$result = mysqli_query($conn,$query);
    $arr = $result->fetch_all(MYSQLI_NUM);
    $i = 0;
	foreach($arr as $row){
		preg_match("/([0-9]{4}-[0-9]{1,2}-[0-9]{1,2}.[0-9]{1,2}\\:[0-9]{1,2})\\:[0-9]{1,2}/",$row[0],$date);
		$row[0] = $date[1];
        $row[1] = null;
        $row[2] = (double)$row[2];
		$arr[$i] = $row;
		$i++;
	}
	
    $json2 = json_encode($arr);
    //x축을 continuous data 로 바꾸기 위해 json포멧에서 date부분을 string에서 Date형식으로 바꿔줌
	$num = preg_match_all("/\"([0-9]{4})-([0-9]{1,2})-([0-9]{1,2}).([0-9]{1,2})\\:([0-9]{1,2})\"/", $json2, $date);
	for($i = 0; $i < $num; $i++){
		$json2 = preg_replace("/".$date[0][$i]."/","new Date(".$date[1][$i].",".$date[2][$i].",".$date[3][$i].",".$date[4][$i].",".$date[5][$i].")",$json2);
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
        
		
		
        var data = new google.visualization.DataTable();
        data.addColumn('date','minute');
        data.addColumn('number','min_test');
        data.addColumn('number','min_test1');

        data.addRows(<?=$json1?>);
        data.addRows(<?=$json2?>);

		var options = {
			vAxis: {title: 'price'},
			hAxis: {title: 'minute'},
			seriesType: 'line',
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