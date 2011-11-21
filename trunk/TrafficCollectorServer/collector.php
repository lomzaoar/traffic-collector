<?php
$elements = json_decode($_POST['elements'], true);
$fh = fopen("storage/".$_POST['filename'], "w");
for($i = 0; $i < count($elements); $i++)
{
	$element = $elements[$i];
	$str = $element['lat']." ".$element['long']." ".$element['speed']." ".$element['timestamp'];
	fprintf($fh, $str."\n");
}
fclose($fh);
$dim = @filesize("storage/".$_POST['filename']);
if($dim!=0)echo "200";
else echo "500";