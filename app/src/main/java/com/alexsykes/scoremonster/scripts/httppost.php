<?php
require('./conf.php');

//Make sure that it is a POST request.
if(strcasecmp($_SERVER['REQUEST_METHOD'], 'POST') != 0){
    throw new Exception('Request method must be POST!');
}

//Make sure that the content type of the POST request has been set to application/json
$contentType = isset($_SERVER["CONTENT_TYPE"]) ? trim($_SERVER["CONTENT_TYPE"]) : '';
if(strcasecmp($contentType, 'application/json') != 0){
    echo 'Content type must be: application/json';
    throw new Exception('Content type must be: application/json');
}

//Receive the RAW post data.
$content = trim(file_get_contents("php://input"));

//Attempt to decode the incoming RAW post data from JSON.
$decoded = json_decode($content, true);
//echo $decoded;
//If json_decode failed, the JSON is invalid.
if(!is_array($decoded)){
echo 'Received content contained invalid JSON!';
    throw new Exception('Received content contained invalid JSON!');
}

// Get the variables
$accept = $decoded["accept"];
$acu = $decoded["acu"];
$address = $decoded["address"];
$class = $decoded["class"];
$course = $decoded["course"];
$dob = $decoded["dob"];
$email = $decoded["email"];
$firstname = $decoded["firstname"];
$isyouth = $decoded["isyouth"];
$lastname = $decoded["lastname"];
$mobile_phone = $decoded["mobile_phone"];
$make = $decoded["make"];
$postcode = $decoded["postcode"];
$size =  $decoded["size"];
$trialid = $decoded["trialid"];


// Process the JSON.

 $query = "INSERT INTO up93k_entryman_entry (accept, acu, address, class, course, dob, email, firstname, isyouth, lastname, mobile_phone, make, postcode, size, trialid, created, created_by) VALUES ('$accept', '$acu', '$address', '$class', '$course', '$dob', '$email', '$firstname', '$isyouth', '$lastname', '$mobile_phone', '$make', '$postcode', '$size', '$trialid', NOW(),'999')";
//$query = "INSERT INTO up93k_entryman_entry (`trialid`) VALUES (999) ";

// Create connection
$conn = new mysqli($servername, $username, $password, $database);
// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

if ($conn->query($query) != TRUE) {
    echo "Error: " . $query . "<br>" . $conn->error;
}

$conn->close();
?>