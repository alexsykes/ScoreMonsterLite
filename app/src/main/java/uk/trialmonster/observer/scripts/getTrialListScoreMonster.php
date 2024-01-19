<?php
require("conf.php");

//creating a new connection object using mysqli 
$conn = new mysqli($servername, $username, $password, $database);

//if there is some error connecting to the database
//with die we will stop the further execution by displaying a message causing the error 
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

//if everything is fine

//creating an array for storing the data 
$trials = array(); 

$sql = "SELECT id, eventname, date, club, numlaps, numsections, starttime, email, scoringmode, startinterval  FROM ".$dbprefix."entryman_trial WHERE date >= DATE(NOW()) - INTERVAL 28 DAY AND published = 1  ORDER BY `date` ASC";
//creating an statment with the query
$stmt = $conn->prepare($sql);

//executing that statment
$stmt->execute();

//binding results for that statment 
$stmt->bind_result($id, $eventname, $date, $club, $numlaps, $numsections, $starttime, $email, $scoringmode, $startinterval);

//looping through all the records
while($stmt->fetch()){
	
	//pushing fetched data in an array 
	$temp = [
		'id'=>$id,
		'date'=>$date,
		'club'=>$club,
		'name'=>str_replace(","," -",$eventname),
		'numlaps'=>$numlaps,
		'numsections'=>$numsections,
		'starttime'=>$starttime,
		'scoringmode'=>$scoringmode,
		'startinterval'=>$startinterval,
		'email' => $email
	];
	
	//pushing the array inside the hero array 
	array_push($trials, $temp);
}

//displaying the data in json format 
echo json_encode($trials);