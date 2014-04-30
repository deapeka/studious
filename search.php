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
		$db = 'phrases';
		$contentPhrases = array();
		$keywordPhrases = array();
		$phrases = array();
		$promptWords = explode(" ", $prompt);
		// Check connection
		if (mysqli_connect_errno())
			echo "Failed to connect to MySQL: " . mysqli_connect_error();
			
		$escapedPrompt = mysqli_real_escape_string($con, $prompt);
		$contentResult = mysqli_query($con,"SELECT * FROM " . $db . " WHERE content Like '% " . $escapedPrompt . " %' LIMIT 100;");
		while($row = mysqli_fetch_array($contentResult)) {
			$contentPhrase = new phrase($row[0] . nl2br("\n"), $row[1], $row[2]);
			array_push($contentPhrases, $contentPhrase);
		}
		
		
		foreach($contentPhrases as $contentPhrase)
			foreach($contentPhrase->keywords as $keyword)
				if(sizeof($wordmap) < $breadth)
					if(in_array($keyword, array_keys($wordmap)))
						$wordmap[$keyword] += 3;
					else
						$wordmap[$keyword] = 3;
		
		foreach($promptWords as $promptWord){
			$promptWord = trim($promptWord);
			$escapedPromptWord = mysqli_real_escape_string($con, $promptWord);
			$keywordResult = mysqli_query($con,"SELECT * FROM " . $db . " WHERE keywords LIKE '% " . $escapedPromptWord . ",%' LIMIT 100;");

			while($row = mysqli_fetch_array($keywordResult)) {
				$keywordPhrase = new phrase($row[0] . nl2br("\n"), $row[1], $row[2]);
				array_push($keywordPhrases, $keywordPhrase);
			}
		}
		
		
		foreach($keywordPhrases as $keywordPhrase){
			$containsAllPromptWords = true;
			foreach($promptWords as $promptWord){
				if(stripos($keywordPhrase->keywordsString, $promptWord) == false)
					$containsAllPromptWords = false;
			}
			if($containsAllPromptWords)
				foreach($keywordPhrase->keywords as $keyword){
					if(!is_numeric($keyword)){
						if(in_array($keyword, array_keys($wordmap)))
							$wordmap[$keyword] += 2;
						else
							$wordmap[$keyword] = 2;
					}
				}
		}
		
		foreach($keywordPhrases as $keywordPhrase)
			foreach($keywordPhrase->keywords as $keyword)
				if(sizeof($wordmap) < $breadth && !is_numeric($keyword))
					if(in_array($keyword, array_keys($wordmap)))
						$wordmap[$keyword] += 1;
					else
						$wordmap[$keyword] = 1;
		
		$phrases = array_unique(array_merge($contentPhrases, $keywordPhrases));
		
		normalize($phrases);
		
		usort($phrases, "cmp");
	  
	    $relevances = array();
		
		foreach($phrases as $phrase){
			if($phrase->relevance >= 100 - $depth & !in_array($phrase->relevance, $relevances)){
				array_push($relevances, $phrase->relevance);
				echo '<sup> <a href=' . $phrase->source . '>src</a> </sup>' . $phrase->content;
			}
		}
		
		echo mysqli_error($con);
		mysqli_close($con);
	}
	
	function normalize($phrases){
		$maxRelevance = -9999;
		$minRelevance = 9999;
		$maxComplexity = -9999;
		$minComplexity = 9999;
		foreach($phrases as $phrase){
			$phrase->calculateRelevance();
			if($phrase->relevance > $maxRelevance)
				$maxRelevance = $phrase->relevance;
		    if($phrase->relevance < $minRelevance)
				$minRelevance = $phrase->relevance;
			if($phrase->complexity > $maxComplexity)
				$maxComplexity = $phrase->complexity;
		    if($phrase->complexity < $minComplexity)
				$minComplexity = $phrase->complexity;
		}		
		foreach($phrases as $phrase){
			if($maxRelevance != $minRelevance)
				$phrase->relevance = 100 * ($phrase->relevance-$minRelevance)/($maxRelevance-$minRelevance);
			if($maxComplexity != $minComplexity)
				$phrase->complexity = 100 * ($phrase->complexity-$minComplexity)/($maxComplexity-$minComplexity);
		}
	}

	class phrase {
		public $content;
		public $source;
		public $keywords;
		public $keywordsString;
		public $complexity;
		public $relevance;
		
		public function __construct($content, $source, $keywords)
		{
			//$content = preg_replace("/(?![.=$'€%-])\p{P}/u", "", $content);
			$this->content = $content;
			$this->source = $source;
			$this->keywordsString = $keywords;
			$tempKeywords = explode(',', $keywords);
			$this->keywords = array();
			foreach($tempKeywords as $keyword){
				//$keyword = preg_replace("/(?![.=$'€%-])\p{P}/u", "", $keyword);
				trim($keyword);
				array_push($this->keywords, $keyword);
			}
			$this->complexity = complexity($this->content);
			$this->relevance = 0;
		}
		
		public function calculateRelevance(){
			global $prompt, $wordmap;
			$phraseWords = explode(" ", $this->content);
			foreach($phraseWords as $phraseWord)
				if(!is_numeric($phraseWord) && in_array($phraseWord, array_keys($wordmap)))
					$this->relevance += $wordmap[$phraseWord];
			foreach($this->keywords as $keyword)
				if(!is_numeric($keyword) && in_array($keyword, array_keys($wordmap)))
					$this->relevance += $wordmap[$keyword];
		}
		
		public function __toString(){
			return $this->content;
		}
	}
	
	function cmp($p1, $p2){
		if($p1->relevance - $p1->complexity > $p2->relevance - $p2->complexity)
			return -1;
		else
			return 1;
	}
	
	function complexity($str){
	    $PUNCTUATION_BASIC_WEIGHT = 2;
		$PUNCTUATION_MORE_WEIGHT = 5;
		$SPECIAL_CHARACTER_WEIGHT = 10;
		$LENGTH_WEIGHT = 1;
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