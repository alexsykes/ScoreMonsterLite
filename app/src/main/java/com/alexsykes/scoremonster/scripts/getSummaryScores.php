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
 

//$sql = "SELECT ss.rider,  GROUP_CONCAT(`section`) as sections, GROUP_CONCAT(ss.scores) AS scorelist, SUM(ss.total) as totalscore from (SELECT rider, section, lap, SUM(score) AS total, GROUP_CONCAT(score ORDER By lap ASC SEPARATOR '') as scores FROM up93k_entryman_score AS t WHERE trialid = $trialid GROUP BY rider, section ORDER BY t.rider) AS ss GROUP BY rider ORDER BY rider ASC";
	 
// $sql = "SELECT * FROM (SELECT ss.rider,  GROUP_CONCAT(`section`) as sections, GROUP_CONCAT(ss.scores) AS scorelist, SUM(ss.total) as totalscore from (SELECT rider, section, lap, SUM(score) AS total, GROUP_CONCAT(score ORDER By lap ASC SEPARATOR '') as scores FROM up93k_entryman_score AS t WHERE trialid = $trialid GROUP BY rider, section ORDER BY t.rider) AS ss GROUP BY rider ORDER BY rider ASC) AS b WHERE b.totalscore IS NOT NULL";

$sql="SELECT * FROM (SELECT ss.rider,  GROUP_CONCAT(section) AS sections, GROUP_CONCAT(ss.scores) AS scorelist, GROUP_CONCAT(ss.scoresraw) AS scorelists, SUM(ss.total) AS totalscore FROM (SELECT rider, lap, section, SUM(IF(score LIKE 'x',5, IF(score LIKE 'o',0, score)) ) AS total, GROUP_CONCAT(IF(score='x',5, IF(score='o',0, score))  ORDER BY lap ASC SEPARATOR '') AS scores, GROUP_CONCAT(score ORDER BY lap ASC SEPARATOR '') AS scoresraw FROM up93k_entryman_score AS t WHERE trialid = $trialid AND score IS NOT NULL GROUP BY rider, section ORDER BY t.rider) AS ss GROUP BY rider ORDER BY rider ASC) AS b WHERE b.totalscore IS NOT NULL";

//creating an statment with the query
$stmt = $conn->prepare($sql);
 
//executing that statment
$stmt->execute();
 
//binding results for that statment 
$stmt->bind_result($rider, $sections, $scorelist, $scorelists, $totalscore);

//looping through all the records
while($stmt->fetch()){

 $temp = [
 'rider'=>$rider,
 'sections'=>$sections,
 'scorelist'=>$scorelist,
 'scorelists'=>$scorelists,
 'totalscore'=>$totalscore
 ];
 
 //pushing the array inside the hero array 
 array_push($result, $temp);
}

//displaying the data in json format 
echo json_encode($result);
