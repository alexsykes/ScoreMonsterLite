<?php
//these are the server details
//the username is root by default in case of xampp
//password is nothing by default
//and lastly we have the database named android. if your database name is different you have to change it 
require("conf.php");
 
$id = $_GET['id'];

//creating a new connection object using mysqli 
$conn = new mysqli($servername, $username, $password, $database);
 
//if there is some error connecting to the database
//with die we will stop the further execution by displaying a message causing the error 
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
 
//if everything is fine
 
//creating an array for storing the data 
$trialDetails = array(); 
 
//this is our sql query 
$sql = "SELECT id, numsections, numlaps, eventname AS name FROM ".$dbprefix."entryman_trial  WHERE id = $id;";

//creating an statment with the query
$stmt = $conn->prepare($sql);
//executing that statment
$stmt->execute();
 
//binding results for that statment 
$stmt->bind_result($id, $numsections, $numlaps, $name);


//looping through all the records
while($stmt->fetch()){
 //pushing fetched data in an array 
//$details = str_replace( '"', '/"', $details);

$details = json_encode($details, JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_HEX_AMP | JSON_UNESCAPED_UNICODE); 

//$details = "Nothing to see here";
 $temp = [
 'id'=>$id,
 'numsections'=>$numsections,
 'name'=>$name,
 'numlaps'=>$numlaps
 ];
 
 //pushing the array inside the hero array 
 array_push($trialDetails, $temp);
}
 
 
 //var_dump($trialDetails);
//displaying the data in json format 
echo json_encode($trialDetails);