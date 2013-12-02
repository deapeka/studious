import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class Result extends SearchAnalyzer {

	private ArrayList<Phrase> myPhrases;

	public Result(){
		myPhrases = new ArrayList<Phrase>();
	}
	
	public boolean add(Phrase p) {
		myPhrases.add(p);
		return true;
	}
	
	public ArrayList<Phrase> getPhrases() {
		return myPhrases;
	}

	public void setPhrases(ArrayList<Phrase> myPhrases) {
		this.myPhrases = myPhrases;
	}
	
}
