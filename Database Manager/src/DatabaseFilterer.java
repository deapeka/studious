import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


public class DatabaseFilterer extends SearchAnalyzer{
	
	public void filter(){
		deleteAllSQL("filteredPhrases");
		ArrayList<Phrase> toBePushed = loadFromSQL("phrases", true);
		pushToSQL(toBePushed, "filteredPhrases");		
	}
	
	public void weigh(){
		deleteAllSQL("weighedPhrases");
		ArrayList<Phrase> loaded = loadFromSQL("filteredPhrases", false);
		Map<Phrase, Integer> phraseWeights = new HashMap<Phrase, Integer>();
		/*for(int i = 1; i < loaded.size(); i++){
			Phrase p1 = loaded.get(i);
			int weight = 1;
			double cs;
			for(Phrase p2: phraseWeights.keySet()){
				if((cs = cosineSimilarity(p1, p2)) > SIMILARITY_THRESHOLD){
					weight++;
					loaded.remove(p2);
				}
			}
			phraseWeights.put(p1, weight);
			System.out.println(i);
		}*/
		
		
		for(int i = 0; i < loaded.size(); i++){
			Phrase p1 = loaded.get(i);
			int weight = 1;
			for(int j = i+1; j < loaded.size(); j++){
				Phrase p2 = loaded.get(j);
				//if((cs = cosineSimilarity(p1, p2)) > SIMILARITY_THRESHOLD){
				if(p2.getContent().equals(p1.getContent())){
					weight++;
					loaded.remove(p2);
					j--;
					//System.out.println(" " + cs + " REMOVED");
				}
				//System.out.println(" " + cs);
			}
			phraseWeights.put(p1, weight);
			//System.out.println(p1 + " " + weight);
		}
		pushToSQL(phraseWeights, "weighedPhrases");
	}
	
	public double cosineSimilarity(Phrase p1, Phrase p2){
		ArrayList<String> allKeywords = p1.getKeywords();
		allKeywords.addAll(p2.getKeywords());
		Set<String> uniqueKeywords = new HashSet<String>(allKeywords);
		Vector<Integer> v1 = new Vector<Integer>();
		Vector<Integer> v2 = new Vector<Integer>();
		for(String keyword: uniqueKeywords){
			if(p1.getKeywords().contains(keyword))
				v1.add(1);
			else
				v1.add(0);
			if(p2.getKeywords().contains(keyword))
				v2.add(1);
			else
				v2.add(0);
		}
		return dotProduct(v1, v2)/(magnitude(v1)*magnitude(v2));
	}
	
	public int dotProduct(Vector<Integer> v1, Vector<Integer> v2){
		int dp = 0;
		for(int i = 0; i < v1.size(); i++)
			dp += v1.get(i) * v2.get(i);
		return dp;
	}
	
	public double magnitude(Vector<Integer> v){
		double sumOfSquares = 0;
		for(int i = 0; i < v.size(); i++)
			sumOfSquares += Math.pow(v.get(i), 2);
		return Math.pow(sumOfSquares, 0.5);
	}
}
