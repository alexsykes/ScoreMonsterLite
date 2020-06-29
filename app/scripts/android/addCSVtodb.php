<?php

// Database conection
$host = 'localhost';
$user = 'us****er';
$password = 'pa****rd';
$db = 'da****se';

$mysqli = new mysqli($host, $user, $password, $db);

if ($mysqli->connect_error) {
	die("$mysqli->connect_errno: $mysqli->connect_error");
}


$handle = fopen("./uploads/scores.csv", "r");
$lockQuery = "LOCK TABLE up93k_entryman_score WRITE";
$unlockQuery = "UNLOCK TABLE up93k_entryman_score WRITE";

$header = fgetcsv($handle,1000,",");

$stmt = $mysqli->stmt_init();
//$stmt->prepare($lockQuery);
// $stmt->execute();

while($data = fgetcsv($handle,1000,",")){
	$section = $data[1]; // section
	$rider = $data[2]; // rider
	$lap = $data[3]; // lap
	$score = $data[4]; // score
	$created = $data[6]; // created
	$updated = $data[7]; // updated
	$edited = $data[8]; // edited
	$trialid = $data[9]; // trialid

	$query = "SELECT id FROM up93k_entryman_score WHERE `trialid` = $trialid AND `rider` = $rider AND `section` = $section AND `lap` = $lap";


	if(!$stmt->prepare($query))
	{
		print "Failed to prepare statement\n";
	}

	$stmt->execute();
	$result = $stmt->get_result();

	if ($row = $result->fetch_array(MYSQLI_NUM))
	{
	if ($row){
		$id = $row[0];
		// $update = "UPDATE up93k_entryman_score SET `score` = $score, modified = STR_TO_DATE('$created',  '%Y-%m-%d %H:%i:%s') WHERE `id`= $id";
		//$update = "UPDATE up93k_entryman_score SET `score` = $score, modified = CONVERT_TZ(NOW(),'+0:00','+4:00') WHERE `id`= $id";
		$update = "UPDATE up93k_entryman_score SET `score` = $score, `modified` = STR_TO_DATE('$updated',  '%Y-%m-%d %H:%i:%s') WHERE `id`= $id";
	}
	}
	else{
	// Check update / created times taking account of BST / GMT

		$update = "INSERT INTO up93k_entryman_score (section, rider, lap, score, trialid, created, modified) VALUES ($section, $rider, $lap, $score, $trialid, STR_TO_DATE('$created',  '%Y-%m-%d %H:%i:%s'),STR_TO_DATE('$updated',  '%Y-%m-%d %H:%i:%s'))";
	}
	//print "Update: " . $update."<br />";
	if(!$stmt->prepare($update))
	{
		print "Failed to prepare statement\n";
	}

	$stmt->execute();
	$result = $stmt->get_result();
}

$stmt->prepare($unlockQuery);
$stmt->execute();
$mysqli->close();
?>