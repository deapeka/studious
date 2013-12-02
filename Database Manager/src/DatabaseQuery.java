import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class DatabaseQuery extends SearchAnalyzer{
	private String myPrompt;
	private	int myDepth;
	private	int myBreadth;
	private	ArrayList<Phrase> myPhrases;
	private	HashMap<String, String> myNounsMap;
	
	public DatabaseQuery(String prompt){
		myPrompt = prompt;
		myPhrases = new ArrayList<Phrase>();
		myNounsMap = mapNouns(myPhrases);
		myPhrases = loadFromSQL("phrases", myPrompt, false);
		replaceNouns();
	}
	
	public DatabaseQuery(String prompt, int depth, int breadth){
		myPrompt = prompt;
		myPhrases = new ArrayList<Phrase>();
		myDepth = depth;
		myBreadth = breadth;
		myNounsMap = mapNouns(myPhrases);
		myPhrases = loadFromSQL("phrases", myPrompt, false);
		replaceNouns();
	}
	
	
	public void replaceNouns(){
		for(String key: myNounsMap.keySet()){
			for(int i = 0; i < myPhrases.size(); i++)
				myPhrases.get(i).setContent(myPhrases.get(i).getContent().replace("(?i)"+key, myNounsMap.get(key)));
		}
	}

	public ArrayList<Phrase> getPhrases() {
		return myPhrases;
	}

	public void setPhrases(ArrayList<Phrase> myPhrases) {
		this.myPhrases = myPhrases;
	}
	
	
	
	

}
