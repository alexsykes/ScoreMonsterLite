<?php
require("conf.php");
 
$trialid = $_GET['id'];
//creating a new connection object using mysqli 
$conn = new mysqli($servername, $username, $password, $database);
 
//if there is some error connecting to the database
//with die we will stop the further execution by displaying a message causing the error 
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

//creating an array for storing the data 
$result = array(); 
 
//this is our sql query 
//$sql = "SELECT section, rider, GROUP_CONCAT(score  SEPARATOR '.') AS score FROM up93k_entryman_score WHERE trialid = $trialid GROUP BY  section, rider ORDER BY section, rider ";

// $sql = "SELECT rider, SUM(score) AS total FROM up93k_entryman_score WHERE trialid = $trialid GROUP BY rider ORDER BY rider";

$sql = "SELECT rider, CONCAT(e.firstname,' ', e.lastname) as name, course, SUM(score) as total FROM up93k_entryman_score s left JOIN up93k_entryman_entry e ON s.rider = e.number AND e.trialid  = s.trialid WHERE s.trialid = $trialid GROUP BY rider ORDER BY course, total";



//creating an statment with the query
$stmt = $conn->prepare($sql);
 
//executing that statment
$stmt->execute();
 
//binding results for that statment 
$stmt->bind_result($rider, $name, $course, $total);


// var_dump($stmt);
//looping through all the records
while($stmt->fetch()){
 //pushing fetched data in an array 
//$details = str_replace( '"', '/"', $details);

//$details = json_encode($details, JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_HEX_AMP | JSON_UNESCAPED_UNICODE); 

//$details = "Nothing to see here";
 $temp = [
 'rider'=>$rider,
 'name'=>$name,
 'course'=>$course,
 'total'=>$total
 ];
 
 //pushing the array inside the hero array 
 array_push($result, $temp);
}

//displaying the data in json format 
echo json_encode($result);
