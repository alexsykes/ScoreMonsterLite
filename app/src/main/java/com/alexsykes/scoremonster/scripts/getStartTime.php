<?php
require("conf.php");
 
$trialid = $_GET['trialid'];
//creating a new connection object using mysqli 
$conn = new mysqli($servername, $username, $password, $database);
 
//if there is some error connecting to the database
//with die we will stop the further execution by displaying a message causing the error 
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
//creating an array for storing the data 
$result = array(); 
$query = "SELECT starttime FROM up93k_entryman_trial WHERE id = '$trialid'";

//creating an statment with the query
$stmt = $conn->prepare($query);
 
//executing that statment
$stmt->execute();

$stmt->bind_result($starttime);

//looping through all the records
while($stmt->fetch()){

 $temp = [
 'starttime'=>$starttime
 ];
 
 //pushing the array inside the array 
 array_push($result, $temp);
}

//displaying the data in json format 
echo json_encode($result);