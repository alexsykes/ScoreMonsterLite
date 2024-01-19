<?php

function timeStampToDateTime($timestamp, $zone, $format = "YYYY-m-d H:i:s") {
    $date = new \DateTime($zone);
    $date->setTimeZone(new \DateTimeZone($zone));
    $date->setTimeStamp($timestamp);
    $result = $date->format($format);
    return $result;
}
// Database connection
require("conf.php");
$id = $_GET['id'];
$trialid = $_GET['trialid'];

$mysqli = new mysqli($host, $username, $password, $database);

if ($mysqli->connect_error) {
	die("$mysqli->connect_errno: $mysqli->connect_error");
}

$filename = "data_$id.csv";

$handle = fopen("./uploads/$filename", "r");
$lockQuery = "LOCK TABLE ".$dbprefix."entryman_result WRITE";
$unlockQuery = "UNLOCK TABLE".$dbprefix."entryman_result WRITE";

$header = fgetcsv($handle,1000,",");
// $fastestRider = fgetcsv($handle,1000,",");
//$fastestRider = $header[3];
$baseTime = $header[6];
// echo ("BaseTime: ".$baseTime."<br />");
	echo "$baseTime: ".$baseTime."<br />";
$stmt = $mysqli->stmt_init();

if ($mysqli->connect_errno) {
	echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
}

// Check that trial is using electronic scoring
$query = "SELECT startinterval, penaltydelta FROM ".$dbprefix."entryman_trial WHERE `id` = $trialid";

if(!($stmt->prepare($query)))
{
	print "Line 46: Failed to prepare statement <br />".$mysqli->error;
}

$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_array(MYSQLI_ASSOC);
$startInterval = $row["startinterval"];
$penaltyDelta = $row["penaltydelta"];

// Work through file 
while($data = fgetcsv($handle,1000,",")){
	$CSVid = $data[0];  			// 18
	$rider = $data[1];				// 121
	$finishTimeInMillis = $data[2];	// 1634554729095
	$finishTimeStamp = $data[4];
	$elapsedTime = $data[3]; 		// 14489165
	$finishTime = $finishTimeStamp;	// 2021-10-18 10:58:49
	//$finishTime = timeStampToDateTime($finishTimeInMillis, 'GMT');
	$delta = $elapsedTime - $baseTime;
	$penalty = ceil(($delta)/($penaltyDelta * 1000));
	echo $delta."<br />";
	
	$query = "UPDATE ".$dbprefix."entryman_result SET elapsedtime = '".$elapsedTime."', penalty = '".$penalty."', riderfinishtime='".$finishTime."' WHERE trialid = '".$trialid."' AND rider = ".$rider;
 	// echo ($query."<br />");
	if(!$stmt->prepare($query))
	{
		print "Line 94: Failed to prepare statement\n";
	}
	$stmt->execute();	
}
		
$stmt->prepare($unlockQuery);
$stmt->execute();
$mysqli->close();  