import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class MyCrawler extends WebCrawler {

	private final static Pattern FILTERS = Pattern
			.compile(".*(\\.(css|js|bmp|gif|jpe?g"
					+ "|png|tiff?|mid|mp2|mp3|mp4"
					+ "|wav|avi|mov|mpeg|ram|m4v|pdf"
					+ "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	/**
	 * You should implement this function to specify whether the given url
	 * should be crawled or not (based on your crawling logic).
	 */
	@Override
	public boolean shouldVisit(WebURL url) {
		String href = url.getURL().toLowerCase();
		return !FILTERS.matcher(href).matches()
				&& (href.startsWith("http://en.wikipedia.org/")
						|| href.startsWith("http://hyperphysics.phy-astr.gsu.edu/")
						|| href.startsWith("http://www.britannica.com/") || href
							.startsWith("http://www.encyclopediaofmath.org/")
							|| href.startsWith("http://docs.oracle.com/javase/tutorial/java/"))
				&& !href.substring(href.indexOf(".org")).contains(":")
				&& !href.contains("%") && !href.contains("&") && !href.contains("index.html");
	}

	/**
	 * This function is called when a page is fetched and ready to be processed
	 * by your program.
	 */
	@Override
	public void visit(Page page) {
		if (!setup) {
			setupStanfordParse();
			setup = true;
		}
		String url = page.getWebURL().getURL();
		System.out.println("URL: " + url);

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();

			Result result = extractHTML(html, url);
			result.pushToSQL(result.getPhrases(), "phrases");
		}
	}

	public Result extractHTML(String html, String source) {
		Result result = new Result();
		Document doc = Jsoup.parse(html);
		String data = readDoc(doc);
		data = parse(data);
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		iterator.setText(data);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
				.next()) {
			String sentence = data.substring(start, end);
			if(sentence.length() <= 255 && source.length() <= 255){
				StringTokenizer st = new StringTokenizer(sentence);
				ArrayList<String> keywords = new ArrayList<String>();
				while(st.hasMoreTokens())
					keywords.add(st.nextToken());
				if(keywords.toString().length() <= 255){
					Phrase p = new Phrase(sentence, source, keywords);
					result.add(p);
				}
			}
		}
		return result;
	}

	public static String readDoc(Document doc) {
		try {
			Element content = doc.body();
			Elements p = content.getElementsByTag("p");
			StringBuilder pConcatenated = new StringBuilder();
			for (Element x : p) {
				pConcatenated.append(x.text() + " ");
			}
			return parse(pConcatenated.toString());
		} catch (Exception e) {
			return "";
		}
	}

	public static String parse(String str) {
		str = removeSquareBrackets(str);
		str.trim();
		str = stanfordParse(str);
		str.trim();
		return str;
	}

	public static String removeSquareBrackets(String str) {
		int left;
		int right;
		while ((left = str.indexOf("[")) != -1)
			if ((right = str.indexOf("]")) != -1 && right > left)
				str = str.replace(str.substring(left, right + 1), "");
		return str;
	}

	static Properties props;
	static StanfordCoreNLP pipeline;
	static boolean setup = false;

	public static void setupStanfordParse() {
		props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
	}

	public static String stanfordParse(String str) {
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(str);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		StringBuilder parsedText = new StringBuilder();
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			parsedText.append(sentence.get(TextAnnotation.class) + "\n");
		}
		return parsedText.toString();
	}
}