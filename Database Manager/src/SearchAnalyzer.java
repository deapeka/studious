import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class SearchAnalyzer {

	final static String[] PRONOUNS = { "he", "she", "they" };
	final static String[] FLAGS = { " the following", " key", " important",
			" affect", " consist", " can", " express", " contain", " provide",
			" way", " must", "demonstrat", " mean", " about", " often",
			" role", " vital", " central", " work", " stud", " know", " clear",
			" only", " always", " detail", " critical", " struct",
			" monumental", " vast", " signif", " deal", " achiev", " enhanc" };
	final static String[] EXAMPLE_FLAGS = { "for example", "for instance" };
	final static String[] DEFINITION_FLAGS = { " defin" };
	final static String[] FILTERS = { "...", "full story", "welcome",
			"learn more", "more information", "contact us", "let's",
			"read about", "article", "news", "www", ".com", "\"[", " ^ ",
			" ads", "toolbar", "?", "coming soon", "aside from", "reference",
			"can be found in", "site ", "comment", " I ", "you", "we " };
	final static String[] REMOVE_TAGS = { "Additionally, ", "Therefore, ",
			"[citation needed]", "Also, ", "Instead, " };
	final static String KEYWORD_FILTERS = "the be and of a an any has are were more into have to it I that for you he with on do say this they at but we his from that not n't n't by she or as what go their can who get if would her all my make about know will as up one time there year so think when which them some me people take out into just see him your come could now than like other how then its our two more these want way look first also new because day more use no man find here thing give many well only those tell one very her even back any good woman through us life child there work down may after should call world over school still try in as last ask need too feel three when state never become between high really something most another much family own out leave put old while mean on keep student why let great same big group begin seem country help talk where turn problem every start hand might American show part about against place over such again few case most week company where system each right program hear so question during work since within though";
	final static double SPECIAL_CHARACTER_WEIGHT = 20;
	final static double PUNCTUATION_BASIC_WEIGHT = 2;
	final static double PUNCTUATION_MORE_WEIGHT = 5;
	final static double LENGTH_WEIGHT = 0.1;
	final static int LENGTH_THRESHOLD_MIN = 50;
	final static int LENGTH_THRESHOLD_MAX = 255;
	final static double COMPLEXITY_DENSITY_LIMIT = 0.3;
	final static double SIMILARITY_THRESHOLD = 0.5;

	static Properties props;
	static StanfordCoreNLP pipeline;
	static LexicalizedParser lp;
	static TokenizerFactory<Word> tf;
	static TreebankLanguagePack languagePack;
	static boolean setup = false;

	public static void setupParser() {
		props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
		lp = LexicalizedParser.loadModel();
		tf = PTBTokenizer.factory();
		languagePack = new PennTreebankLanguagePack();
	}

	public static ArrayList<String> getKeywords(String str) {

		if (!setup) {
			setupParser();
			setup = true;
		}

		List<Word> tokens = tf.getTokenizer(new StringReader(str)).tokenize();
		Tree t = lp.parse(tokens);

		GrammaticalStructure structure = languagePack
				.grammaticalStructureFactory().newGrammaticalStructure(t);
		Collection<TypedDependency> typedDependencies = structure
				.typedDependenciesCollapsed();

		ArrayList<String> keywords = new ArrayList<String>();
		ArrayList<String> nsubjs = new ArrayList<String>();
		ArrayList<String> amods = new ArrayList<String>();
		ArrayList<String> agents = new ArrayList<String>();
		for (TypedDependency td : typedDependencies) {
			if (td.reln().equals(EnglishGrammaticalRelations.NOMINAL_SUBJECT))
				nsubjs.add(td.toString());
			else if (td.reln().equals(
					EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER))
				amods.add(td.toString());
			else if (td.reln().equals(EnglishGrammaticalRelations.AGENT))
				agents.add(td.toString());
		}
		for (String nsubj : nsubjs) {
			String keyword = nsubj.substring(nsubj.indexOf(", ") + 2,
					nsubj.lastIndexOf("-"));
			for (String amod : amods) {
				if (nsubj.substring(nsubj.indexOf(", ") + 2,
						nsubj.lastIndexOf(")")).equals(
						amod.substring(amod.indexOf("(") + 1,
								amod.indexOf(", "))))
					keyword = amod.substring(amod.indexOf(", ") + 2,
							amod.lastIndexOf("-"))
							+ " " + keyword;
			}
			keywords.add(keyword);
		}
		for (String agent : agents) {
			String keyword = agent.substring(agent.indexOf(", ") + 2,
					agent.lastIndexOf("-"));
			for (String amod : amods) {
				if (agent.substring(agent.indexOf(", ") + 2,
						agent.lastIndexOf(")")).equals(
						amod.substring(amod.indexOf("(") + 1,
								amod.indexOf(", "))))
					keyword = amod.substring(amod.indexOf(", ") + 2,
							amod.lastIndexOf("-"))
							+ " " + keyword;
			}
			keywords.add(keyword);
		}
		return keywords;
	}

	public static double complexity(String str) {
		String PUNCTUATION_BASIC = ",.!?-'\"():;";
		String PUNCTUATION_MORE = "/{}[]<>";
		int special_character_count = 0;
		int punctuation_basic_count = 0;
		int punctuation_more_count = 0;
		double complexity = 0;
		for (int i = 0; i < str.length(); i++) {
			if (!(Character.isLetterOrDigit(str.charAt(i)) || Character
					.isSpaceChar(str.charAt(i)))) {
				if (PUNCTUATION_BASIC.contains(str.charAt(i) + ""))
					punctuation_basic_count++;
				else if (PUNCTUATION_MORE.contains(str.charAt(i) + ""))
					punctuation_more_count++;
				else
					special_character_count++;
			}
		}
		complexity += punctuation_basic_count * PUNCTUATION_BASIC_WEIGHT;
		complexity += punctuation_more_count * PUNCTUATION_MORE_WEIGHT;
		complexity += special_character_count * SPECIAL_CHARACTER_WEIGHT;
		complexity += str.length() * LENGTH_WEIGHT;
		DecimalFormat df = new DecimalFormat("#.##");
		return Double.valueOf(df.format(complexity));
	}

	public static double complexityDensity(String str) {
		return complexity(str) / str.length();
	}

	public void pushToSQL(ArrayList<Phrase> phrases, String db) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/studious", "neelvirdy",
					"kryptiatk1");
			for (Phrase p : phrases) {
				stmt = conn.prepareStatement(p.getSQLInsertQuery(db));
				stmt.executeUpdate();
			}
			if (stmt != null && conn != null) {
				stmt.close();
				stmt = null;

				conn.close();
				conn = null;
			}

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		System.out.println(phrases.size() + " "
				+ (phrases.size() == 1 ? "phrase" : "phrases") + " pushed to "
				+ db + ".");
	}

	public void pushToSQL(Map<Phrase, Integer> phraseWeights, String db) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/studious", "neelvirdy",
					"kryptiatk1");
			for (Phrase p : phraseWeights.keySet()) {
				String content = p.getContent().replace("\\", "\\\\");
				content = content.replace("\'", "\\" + "\'");
				String source = p.getSource().replace("\\", "\\\\");
				source = source.replace("\'", "\\" + "\'");
				String keywords = p.getKeywords().toString().replace("\\", "\\\\");
				keywords = keywords.replace("\'", "\\" + "\'");
				stmt = conn
						.prepareStatement("INSERT IGNORE INTO weighedPhrases VALUES ('"
								+ content
								+ "', '"
								+ p.getSource()
								+ "', '"
								+ p.getKeywords()
								+ "', "
								+ phraseWeights.get(p) + ");");
				stmt.executeUpdate();
			}
			if (stmt != null && conn != null) {
				stmt.close();
				stmt = null;

				conn.close();
				conn = null;
			}

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		System.out.println(phraseWeights.size() + " "
				+ (phraseWeights.size() == 1 ? "phrase" : "phrases")
				+ " pushed to " + db + ".");
	}

	public ArrayList<Phrase> loadFromSQL(String db, boolean filter) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String query = "SELECT * FROM " + db;
		ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/studious", "neelvirdy",
					"kryptiatk1");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				// System.out.println("debug " + rs.getString(1));
				String content = rs.getString(1);
				String source = rs.getString(2);
				String keywordsString = rs.getString(3);
				ArrayList<String> keywords = new ArrayList<String>(
						Arrays.asList(keywordsString.split(", ")));
				Phrase p = new Phrase(content, source, keywords);
				if (filter) {
					if (testFilters(p)) {
						p.filter();
						phrases.add(p);
					}
				} else
					phrases.add(p);
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		System.out.println(phrases.size() + " "
				+ (phrases.size() == 1 ? "phrase" : "phrases")
				+ " loaded from " + db + ".");
		return phrases;
	}

	public ArrayList<Phrase> loadFromSQL(String db, String prompt,
			boolean filter) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		prompt = prompt.replace("\\", "\\\\");
		prompt = prompt.replace("\'", "\\" + "\'");
		String query = "SELECT * FROM " + db + " WHERE content LIKE '%"
				+ prompt + "%'";
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/studious", "neelvirdy",
					"kryptiatk1");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				// System.out.println("debug " + rs.getString(1));
				String content = rs.getString(1);
				String source = rs.getString(2);
				String keywordsString = rs.getString(3);
				ArrayList<String> keywords = new ArrayList<String>(
						Arrays.asList(keywordsString.split(", ")));
				Phrase p = new Phrase(content, source, keywords);
				if (filter) {
					if (testFilters(p)) {
						p.filter();
						phrases.add(p);
					}
				} else
					phrases.add(p);
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		System.out.println(phrases.size() + " "
				+ (phrases.size() == 1 ? "phrase" : "phrases")
				+ " loaded from " + db + ".");
		return phrases;
	}

	public void deleteAllSQL(String db) {
		Connection conn = null;
		PreparedStatement stmt = null;
		int numDeleted = 0;
		String query = "DELETE FROM " + db + ";";
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/studious", "neelvirdy",
					"kryptiatk1");
			stmt = conn.prepareStatement(query);
			numDeleted = stmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		System.out.println(numDeleted + " "
				+ (numDeleted == 1 ? "phrase" : "phrases") + " deleted from "
				+ db + ".");
	}

	public static boolean matchesPrompt(String sentence, String prompt) {
		String[] promptWords = prompt.split(" ");
		for (String promptWord : promptWords)
			if (StringUtils.containsIgnoreCase(sentence, promptWord))
				return true;
		return false;
	}

	public static boolean testFilters(Phrase p) {
		String content = p.getContent();
		if (flagged(content) && !filtered(content) && !tooProper(content)
				&& content.length() >= LENGTH_THRESHOLD_MIN
				&& content.length() <= LENGTH_THRESHOLD_MAX
				&& complexityDensity(content) < COMPLEXITY_DENSITY_LIMIT
				&& p.getKeywords().size() > 0)
			return true;
		return false;
	}

	public static boolean flagged(String str) {
		for (String flag : FLAGS)
			if (StringUtils.containsIgnoreCase(str, flag))
				return true;
		return false;
	}

	public static boolean exampleFlagged(String str) {
		for (String flag : EXAMPLE_FLAGS)
			if (StringUtils.containsIgnoreCase(str, flag))
				return true;
		return false;
	}

	public static boolean definitionFlagged(String str) {
		for (String flag : DEFINITION_FLAGS)
			if (StringUtils.containsIgnoreCase(str, flag))
				return true;
		return false;
	}

	public static boolean filtered(String str) {
		for (String filter : FILTERS)
			if (StringUtils.containsIgnoreCase(str, filter))
				return true;
		return false;
	}

	public static boolean tooProper(String sentence) {
		StringTokenizer st = new StringTokenizer(sentence);
		int properCount = 0;
		int wordCount = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (isProper(token) || token.toUpperCase() == token)
				properCount++;
			wordCount++;
		}
		return (properCount + 0.0) / wordCount >= 0.3;
	}

	public static HashMap<String, String> mapNouns(ArrayList<Phrase> phrases) {
		HashMap<String, String> mapNouns = new HashMap<String, String>();
		HashMap<String, Integer> pronounCount = new HashMap<String, Integer>();
		ValueComparator pronounVC = new ValueComparator(pronounCount);
		TreeMap<String, Integer> sortedPronounCount = new TreeMap<String, Integer>(
				pronounVC);
		HashMap<String, Integer> properNounCount = new HashMap<String, Integer>();
		ValueComparator properNounVC = new ValueComparator(properNounCount);
		TreeMap<String, Integer> sortedProperNounCount = new TreeMap<String, Integer>(
				properNounVC);
		StringBuilder sb = new StringBuilder();
		for (Phrase p : phrases)
			sb.append(p.getContent() + "\n");
		String data = sb.toString();
		StringTokenizer st = new StringTokenizer(data);
		ArrayList<String> tokens = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			tokens.add(token);
		}
		for (int i = 0; i < tokens.size(); i++) {
			for (String pronoun : PRONOUNS)
				if (StringUtils.containsIgnoreCase(tokens.get(i), pronoun))
					if (pronounCount.containsKey(pronoun))
						pronounCount
								.put(pronoun, pronounCount.get(pronoun) + 1);
					else
						pronounCount.put(pronoun, 1);
			if (isProper(tokens.get(i))) {
				String properNoun = tokens.get(i);
				int j = 1;
				while (isProper(tokens.get(i + j))) {
					properNoun += " " + tokens.get(i + j);
					j++;
				}
				if (properNounCount.containsKey(properNoun))
					properNounCount.put(properNoun,
							properNounCount.get(properNoun) + 1);
				else
					properNounCount.put(properNoun, 1);
			}
		}
		sortedPronounCount.putAll(pronounCount);
		sortedProperNounCount.putAll(properNounCount);
		if (sortedPronounCount.keySet().size() > 0
				&& sortedProperNounCount.keySet().size() > 0)
			mapNouns.put(sortedPronounCount.firstKey(),
					sortedProperNounCount.firstKey());
		// System.out.println(mapNouns);
		return mapNouns;
	}

	public static boolean isProper(String str) {
		return StringUtils.isAllUpperCase(str.charAt(0) + "")
				&& StringUtils.isAllLowerCase(str.substring(1));
	}

}

class ValueComparator implements Comparator<String> {

	HashMap<String, Integer> base;

	public ValueComparator(HashMap<String, Integer> base) {
		this.base = base;
	}

	public int compare(String a, String b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		}
	}
}
