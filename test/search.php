<?php
	$depth = $_POST['depth'];
	$breadth = $_POST['breadth'];
	$prompt = $_POST['searchBox'];
	$wordmap = array();
    if(isset($_POST['searchBox'])){
	  if(strlen($_POST['searchBox']) > 0)
		fetchResults();
	}
	
	function fetchResults(){
		global $depth, $breadth, $prompt, $wordmap;
		$con = mysqli_connect("localhost","neelvirdy","kryptiatk1","studious");
		$db = 'weighedPhrases';
		$phrases = array();
		$promptWords = explode(" ", $prompt);
		// Check connection
		if (mysqli_connect_errno())
			echo "Failed to connect to MySQL: " . mysqli_connect_error();
			
		foreach($promptWords as $promptWord){
			$promptWord = trim($promptWord);
			$escapedPromptWord = mysqli_real_escape_string($con, $promptWord);
			$result = mysqli_query($con,"SELECT * FROM " . $db . " WHERE content LIKE '%" . $escapedPromptWord . "%';");

			while($row = mysqli_fetch_array($result)) {
				if($db === 'weighedPhrases')
					$phrase = new phrase($row[0] . nl2br("\n"), $row[1], $row[2], $row[3]);
				else
					$phrase = new phrase($row[0] . nl2br("\n"), $row[1], $row[2], 0);
				if(!in_array($phrase, $phrases))
					array_push($phrases, $phrase);
			}
		}
		
		foreach($phrases as $phrase){
			foreach($phrase->keywords as $keyword)
				if(!in_array($keyword, array_keys($wordmap))){
					if(sizeof($wordmap) < $breadth/5)
						$wordmap[$keyword] = 1 * $phrase->immediateRelevance;
				}
				else
					$wordmap[$keyword] = $wordmap[$keyword]+1*$phrase->immediateRelevance;
		}
		
		foreach($phrases as $phrase)
			$phrase->calculateRelevance();
		
		usort($phrases, "cmp");
		
		echo 'test';
	  
		foreach($phrases as $phrase)
			if($phrase->complexity < 60 + $depth)
				echo $phrase->relevance . " " . $phrase->complexity . " " . $phrase->content;
	  
		echo mysqli_error($con);
		mysqli_close($con);
	}

	class phrase {
		public $content;
		public $source;
		public $keywords;
		public $keywordsString;
		public $complexity;
		public $weight;
		public $relevance;
		public $immediateRelevance;
		
		public function __construct($content, $source, $keywords, $weight)
		{
			$this->content = $content;
			$this->source = $source;
			$this->keywordsString = $keywords;
			$keywords = substr($keywords, strripos($keywords, '['), stripos($keywords, ']') - strripos($keywords, '['));
			$this->keywords = explode(',', $keywords);
			foreach($this->keywords as $keyword)
				trim($keyword);
			$this->complexity = complexity($this->content);
			$this->weight = $weight;
			$this->relevance = 0;
			$this->immediateRelevance = 0;
			global $prompt, $wordmap;
			$promptWords = explode(" ", $prompt);
			foreach($promptWords as $promptWord){
				if(strlen($promptWord) > 0 && stripos($this->keywordsString, $promptWord) !== false)
					$this->immediateRelevance += 1.5;		
				if(strlen($promptWord) > 0 && stripos($this->source, $promptWord) !== false)
					$this->immediateRelevance += 1;
			}
			if(strpos($this->content, $prompt) !== false)
				$this->immediateRelevance += 3;
			foreach($this->keywords as $keyword){
				foreach($promptWords as $promptWord)
					if(stripos($keyword, $promptWord) === false)
						$this->immediateRelevance -= 0.05;
			}
		}
		
		public function calculateRelevance(){
			global $prompt, $wordmap;
			$promptWords = explode(" ", $prompt);
			$this->relevance = $this->immediateRelevance;
			foreach(array_keys($wordmap) as $word){
				if(strlen($word) > 0 && stripos($this->keywordsString, $word) !== false)
					$this->relevance += $wordmap[$word] * 0.01;
			}
		}
	}
	
	function cmp($p1, $p2){
		if($p1->relevance > $p2->relevance)
			return -1;
		else if($p1->relevance == $p2->relevance)
			if($p1->weight > $p2->weight)
				return -1;
			else
				return 1;
		else
			return 1;
	}
	
	function complexity($str){
	    $PUNCTUATION_BASIC_WEIGHT = 2;
		$PUNCTUATION_MORE_WEIGHT = 5;
		$SPECIAL_CHARACTER_WEIGHT = 10;
		$LENGTH_WEIGHT = 0.1;
	    $PUNCTUATION_BASIC = ",.!?-'\"():;";
		$PUNCTUATION_MORE = "/{}[]<>";
		$special_character_count = 0;
		$punctuation_basic_count = 0;
		$punctuation_more_count = 0;
		$complexity = 0.0;
		for ($i = 0; $i < strlen($str); $i++) {
			if (!(ctype_alnum($str[$i]) || $str[$i] === ' ')) {
				if (strpos($PUNCTUATION_BASIC, $str[$i]))
					$punctuation_basic_count++;
				else if (strpos($PUNCTUATION_MORE, $str[$i]))
					$punctuation_more_count++;
				else
					$special_character_count++;
			}
		}
		$complexity += $punctuation_basic_count * $PUNCTUATION_BASIC_WEIGHT;
		$complexity += $punctuation_more_count * $PUNCTUATION_MORE_WEIGHT;
		$complexity += $special_character_count * $SPECIAL_CHARACTER_WEIGHT;
		$complexity += strlen($str) * $LENGTH_WEIGHT;
		return $complexity;
	}
?>