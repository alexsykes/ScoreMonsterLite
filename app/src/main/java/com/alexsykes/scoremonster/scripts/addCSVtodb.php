<?php

// Database conection
require("conf.php");
$filename = $_GET['id'];
// print $filename;
$mysqli = new mysqli($host, $username, $password, $database);

if ($mysqli->connect_error) {
	die("$mysqli->connect_errno: $mysqli->connect_error");
}


$handle = fopen("./uploads/".$filename, "r");
$lockQuery = "LOCK TABLE ".$dbprefix."entryman_score WRITE";
$unlockQuery = "UNLOCK TABLE".$dbprefix."entryman_score WRITE";

$header = fgetcsv($handle,1000,",");
	
$stmt = $mysqli->stmt_init();
//$stmt->prepare($lockQuery);
// $stmt->execute();

while($data = fgetcsv($handle,1000,",")){
$CSVid = $data[0];
	$rider = $data[1]; // rider
	$section = $data[2]; // section
	$lap = $data[3]; // lap
	$score = $data[4]; // score
	$observer = $data[5]; // observer
	$created = $data[6]; // created
	$updated = $data[7]; // updated
	$edited = $data[8]; // edited
	$trialid = $data[9]; // trialid
	$sync = $data[10];
	
	
	if ($sync == -1) {
	
	print "ID: " . $CSVid." Sync: ".$sync."<br />";
	
	// if (sync == -1) 

	$query = "SELECT id, score FROM ".$dbprefix."entryman_score WHERE `trialid` = $trialid AND `rider` = $rider AND `section` = $section AND `lap` = $lap";
	
	if(!$stmt->prepare($query))
	{
		print "Failed to prepare statement\n";
	}
	
	$stmt->execute();
	$result = $stmt->get_result();
	
	if ($row = $result->fetch_array(MYSQLI_ASSOC))
	{
	if ($row){
		$id = $row['id'];
		// $update = "UPDATE up93k_entryman_score SET `score` = $score, modified = STR_TO_DATE('$created',  '%Y-%m-%d %H:%i:%s') WHERE `id`= $id";
		//$update = "UPDATE up93k_entryman_score SET `score` = $score, modified = CONVERT_TZ(NOW(),'+0:00','+4:00') WHERE `id`= $id";
		
		$update = "UPDATE ".$dbprefix."entryman_score SET `score` = $score, `observer` = '$observer', `created` = '$created',  `modified` = STR_TO_DATE('$updated',  '%Y-%m-%d %H:%i:%s') WHERE `id`= $id";
	}
	}
	else{
	// Check update / created times taking account of BST / GMT

		$update = "INSERT INTO ".$dbprefix."entryman_score (section, rider, lap, score, observer, trialid, created, modified) VALUES ($section, $rider, $lap, $score, '$observer', $trialid, STR_TO_DATE('$created',  '%Y-%m-%d %H:%i:%s'),STR_TO_DATE('$updated',  '%Y-%m-%d %H:%i:%s'))";
	}
	 print "Update: " . $update."<br />";
	if(!$stmt->prepare($update))
	{
		print "Failed to prepare statement\n";
	}
	
	$stmt->execute();
	$result = $stmt->get_result();
}
}
$stmt->prepare($unlockQuery);
$stmt->execute();
$mysqli->close();
?>