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
 
// Get result set
$sql="SELECT CONCAT(firstname,' ',lastname) AS name, number, ridetime, timepenalty FROM up93k_entryman_entry WHERE trialid='$trialid' AND ridetime > 0 ORDER by timepenalty ASC";

//creating an statment with the query
$stmt = $conn->prepare($sql);

//executing that statment
$stmt->execute();

//binding results for that statment
$stmt->bind_result($name, $number, $ridetime, $timepenalty);

//looping through all the records
while($stmt->fetch()){

 $temp = [
 'name'=>$name,
 'number'=>$number,
 'ridetime'=>$ridetime,
 'timepenalty'=>$timepenalty
 ];
 
 //pushing the array inside the hero array 
 array_push($result, $temp);
}

//displaying the data in json format 
echo json_encode($result);