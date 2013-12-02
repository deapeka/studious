import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Phrase extends SearchAnalyzer {
	private String myContent;
	private String mySource;
	private ArrayList<String> myKeywords;

	public Phrase(String content, String source, ArrayList<String> keywords) {
		myContent = content;
		mySource = source;
		myKeywords = keywords;
	}

	public String getSQLInsertQuery(String db) {
		String keywords = myKeywords.toString();
		keywords.substring(keywords.indexOf("[") + 1, keywords.indexOf("]"));
		keywords = keywords.replace("\\", "\\\\");
		keywords = keywords.replace("\'", "\\" + "\'");
		String content = myContent.replace("\\", "\\\\");
		content = content.replace("\'", "\\" + "\'");
		String source = mySource.replace("\\", "\\\\");
		source = source.replace("\'", "\\" + "\'");
		return "INSERT IGNORE INTO " + db + " VALUES ( '" + content + "', '"
				+ source + "', '" + keywords + "');";
	}

	public void filter() {
		removeTagged();
		myContent.trim();
		capitalizeFirstLetter();
		filterKeywords();
	}

	public void ner() {
		if (!setup) {
			setupParser();
			setup = true;
		}
		AbstractSequenceClassifier classifier = CRFClassifier.getDefaultClassifier();
		myContent = classifier.apply(myContent);
		System.out.println(myContent);
	}

	public void filterKeywords() {
		Iterator<String> iterator = myKeywords.iterator();
		ArrayList<String> newKeywords = new ArrayList<String>();
		for(String keyword: myKeywords){
			keyword = keyword.trim();
			keyword = keyword.replaceFirst("^[^a-zA-Z]+", "");
			keyword = keyword.replaceAll("[^a-zA-Z]+$", "");
			keyword = keyword.toLowerCase();
			newKeywords.add(keyword);
		}
		myKeywords = newKeywords;
		iterator = myKeywords.iterator();
		while (iterator.hasNext()) {
			String keyword = iterator.next();
			if (!StringUtils.isAlphanumericSpace(keyword)) 
				iterator.remove();
			if (StringUtils.containsIgnoreCase(KEYWORD_FILTERS, keyword))
				iterator.remove();
		}
	}

	public void removeTagged() {
		for (String tag : REMOVE_TAGS)
			setContent(getContent().replace(tag, ""));
	}

	public void capitalizeFirstLetter() {
		myContent = (myContent.charAt(0) + "").toUpperCase()
				+ myContent.substring(1);
	}

	public String toString() {
		return myContent;
	}

	public String getContent() {
		return myContent;
	}

	public void setContent(String myContent) {
		this.myContent = myContent;
	}

	public String getSource() {
		return mySource;
	}

	public void setSource(String mySource) {
		this.mySource = mySource;
	}

	public ArrayList<String> getKeywords() {
		return myKeywords;
	}

	public void setKeywords(ArrayList<String> myKeywords) {
		this.myKeywords = myKeywords;
	}

}
