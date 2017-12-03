<?PHP
	$conn = mysqli_connect('localhost','root','password');
	
	$db_status = mysqli_select_db($conn, "coin_chart");
	if(!$db_status)
		die("DB_ERROR");
	$now = time();
		
	$open = $low = $close = $high = 1000;
	for($i = 100; $i; $i--){	
		$date = getdate($now);	
		$minute = $date["year"]."-".$date["mon"]."-".$date["mday"]." ".$date["hours"].":".$date["minutes"];
		
		if(rand(0,100) > 49)
			$isPositive = true;
		else
			$isPositive = false;
		if($close < 200)
			$isPostive = true;
		else if($close > 3000)
			$isPositive = false;
		if($isPositive){
			$open = $close + rand(0,20);
			$close = $open + rand(0,100);
			$low = $open - rand(0,100);
			$high = $close + rand(0,100);
		}else{
			$open = $close - rand(0,20);
			$close = $open - rand(0,100);
			$low = $close - rand(0,100);
			$high = $open + rand(0,100);
		}
		$qty = rand(10,300);
		$query = "INSERT INTO min_test1 VALUES ('".$minute."','".$qty."','".$low."','".$open."','".$close."','".$high."')";
		mysqli_query($conn, $query);
		$now = $now - 60;
	}
	print "Complete!<br>";
?>
	