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
?>