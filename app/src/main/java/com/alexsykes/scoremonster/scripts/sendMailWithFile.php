<?php
  
//     $file_path = "uploads/";
//      
//     $file_path = $file_path . basename( $_FILES['uploaded_file']['name']);
//     if(move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file_path)) {
//         echo "success";
//     } else{
//         echo "fail";
//     }

$trialid = $_GET['trialid'];
$ts = $_GET['id'];
$filename = "./uploads/scores_".$ts.".csv"; 
// echo $filename;

// Get trial details
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
$trialDetails = array(); 
 
//this is our sql query 
$sql = "SELECT t.id as id, club, date, eventname, contact, t.email as email FROM ".$dbprefix."entryman_trial AS t LEFT JOIN ".$dbprefix."entryman_venue AS v ON t.venue_id = v.id WHERE t.id = $trialid";

//creating an statment with the query
$stmt = $conn->prepare($sql);
//executing that statment
$stmt->execute();
 
//binding results for that statment 
$stmt->bind_result($id, $club, $date, $eventname, $contact, $email);

//looping through all the records
while($stmt->fetch()){
 //pushing fetched data in an array 
//$details = str_replace( '"', '/"', $details);

// $details = json_encode($details, JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_HEX_AMP | JSON_UNESCAPED_UNICODE); 

//$details = "Nothing to see here";
 $trial = [
 'club'=>$club,
 'date'=>$date,
 'eventname'=>$eventname,
 'venue'=> $venue,
 'contact' => $contact,
 'email' => $email
 ];
}
$club = $trial['club'];
$venue = $trial['venue'];
$eventname = $trial['eventname'];
$email = $trial['email'];
$contact = $trial['contact'];

$message = "Scores from $club $eventname held at $venue on $date attached";

// echo $message;

//looping through all the records
// while($stmt->fetch()){
 //pushing fetched data in an array 
//$details = str_replace( '"', '/"', $details);

use PHPMailer\PHPMailer\PHPMailer; 
use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\SMTP;
//  phpinfo();


require './mailer/test.php';
require './mailer/Exception.php';
require './mailer/PHPMailer.php';
require './mailer/SMTP.php';
 
// Instantiation and passing `true` enables exceptions
$mail = new PHPMailer(true);

try {
    //Server settings
 //   $mail->SMTPDebug = SMTP::DEBUG_SERVER;                      // Enable verbose debug output
    $mail->isSMTP();                                            // Send using SMTP
    $mail->Host       = $smtpHost;                    // Set the SMTP server to send through
    $mail->SMTPAuth   = true;                                   // Enable SMTP authentication
    $mail->Username   = $smtpAddress;                     // SMTP username
    $mail->Password   = $smtpPassword;                               // SMTP password
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;         // Enable TLS encryption; `PHPMailer::ENCRYPTION_SMTPS` encouraged
    $mail->Port       = 587;                                    // TCP port to connect to, use 465 for `PHPMailer::ENCRYPTION_SMTPS` above

    //Recipients
    $mail->setFrom($smtpAddress, 'TrialMonster Admin');
    $mail->addAddress($email, $contact);               // Name is optional
    $mail->addReplyTo($smtpAddress, 'TrialMonster Admin');

    // Attachments
  //  $mail->addAttachment('/var/tmp/file.tar.gz');         // Add attachments
    $mail->addAttachment($filename);    // Optional name

    // Content
    $mail->isHTML(true);                                  // Set email format to HTML
    $mail->Subject = "Scores - $eventname";
    $mail->Body    = $message;
//    $mail->AltBody = 'See attachment';

	 $mail->send();
    echo 'Message has been sent';
} catch (Exception $e) {
    echo "Message could not be sent. Mailer Error: {$mail->ErrorInfo}";
} 
?>