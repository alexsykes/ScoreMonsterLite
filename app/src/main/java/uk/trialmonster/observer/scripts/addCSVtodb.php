<?php

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
$lockQuery = "LOCK TABLE ".$dbprefix."entryman_score WRITE";
$unlockQuery = "UNLOCK TABLE".$dbprefix."entryman_score WRITE";

$header = fgetcsv($handle,1000,",");
	
$stmt = $mysqli->stmt_init();

if ($mysqli->connect_errno) {
    echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
}
// Check that trial is using electronic scoring
$query = "SELECT scoringmode FROM ".$dbprefix."entryman_trial WHERE `id` = $trialid";

	if(!($stmt->prepare($query)))
	{
		print "Line 46: Failed to prepare statement <br />".$mysqli->error;
	}
	
	$stmt->execute();
	$result = $stmt->get_result();
	$row = $result->fetch_array(MYSQLI_ASSOC);
	$mode = $row["scoringmode"];
	//scoringmode = 2 for electronic scoring
	// if($mode==2)
	

	// Work through file 
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
	
		if($score == 10) {
			$score = 'x';
		}
	
		if ($sync == -1) {
		$query = "SELECT id, score FROM ".$dbprefix."entryman_score WHERE `trialid` = $trialid AND `rider` = $rider AND `section` = $section AND `lap` = $lap";

	  print $query."<br />";
		if(!($stmt->prepare($query)))
		{
			print "Line 46: Failed to prepare statement <br />".$mysqli->error;
		}
	
		$stmt->execute();
		$result = $stmt->get_result();
		// print $result;
		if ($row = $result->fetch_array(MYSQLI_ASSOC))
		{
			if ($row){
				$id = $row['id'];
				$update = "UPDATE ".$dbprefix."entryman_score SET `score` = '$score', `modified` = STR_TO_DATE('$updated',  '%Y-%m-%d %H:%i:%s') WHERE `id`= $id";
			}
		}
		else{
		// Check update / created times taking account of BST / GMT
			$update = "INSERT INTO ".$dbprefix."entryman_score (section, rider, lap, score, trialid, created) VALUES ('$section', '$rider', '$lap', '$score', '$trialid', STR_TO_DATE('$created',  '%Y-%m-%d %H:%i:%s'))";
		}
	 
		 print "<br />".$update."<br />";
		if(!$stmt->prepare($update))
		{
			print "Line 94: Failed to prepare statement\n";
		}
		// Disabled
		$stmt->execute();
		$result = $stmt->get_result();
		}
	}

$stmt->prepare($unlockQuery);
$stmt->execute();
$mysqli->close();  
// unlink("./uploads/$filename");
?>