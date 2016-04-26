import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;   
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.*;
//import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.parsers.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document; 	//Warning: this collides with lucene.document


public class Indexer {
	private Analyzer analyzer; 
    private IndexWriter indexWriter;
    private float averageLength=1.0f;
    private String docPath = "/home/shengjia/heritrix-3.2.0/jobs/JobTest/latest/mirror";
    
    public Indexer(String indexDir){
    	analyzer = new IKAnalyzer();
    	try{
    		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    		Directory dir = FSDirectory.open(new File(indexDir));
    		iwc.setSimilarity(new DefaultSimilarity());
    		indexWriter = new IndexWriter(dir, iwc);
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    public void saveGlobals(String filename){
    	try{
    		PrintWriter pw=new PrintWriter(new File(filename));
    		pw.println(averageLength);
    		pw.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
	
	/** 
	 * Index all the files in the root directory
	 */
    public void indexFiles() throws Exception {
		File topFolder = new File(docPath);
		if (!topFolder.exists()) {
			System.out.println("Document folder doesn't exist");
			return;
		} 
		List<String> linkPatterns = new ArrayList<String>();
		List<String> linkPaths = new ArrayList<String>();
		List<String> documentContents = new ArrayList<String>();
		List<String> documentTitles = new ArrayList<String>();
		
		int fileCount = 0, failCount = 0;
		averageLength = 0.0f;
		String[] domains = topFolder.list();
		for (String domain : domains) {
			//if (fileCount > 1000)
			//	break;
			Stack<String> pathStack = new Stack<String>();
			pathStack.push("");
			while (!pathStack.empty()) {
				String curPath = pathStack.pop();
				String fullPath = curPath == "" ? docPath + "/" + domain : docPath + "/" + domain + "/" + curPath;
				File curFile = new File(fullPath);
				if (!curFile.exists()) {
					System.out.println("Document " + fullPath + " doesn't exist");
					return;
				}
				if (curFile.isDirectory()) {
					String[] subFiles = curFile.list();
					for (String subFile : subFiles) {
						pathStack.push(curPath == "" ? subFile : curPath + "/" + subFile);
					}
				} else if (curFile.isFile()) {
					Document doc = this.addDocument(domain, curPath);
					if (doc != null) {
						documentContents.add(doc.text());
						documentTitles.add(doc.title());
						String linkPattern = curPath.replace("index.html", "").replace("index.htm", "");
						if (linkPattern.endsWith("/"))
							linkPattern = linkPattern.substring(0, linkPattern.length() - 1);
						linkPatterns.add(linkPattern == "" ? domain : domain + "/" + linkPattern);
						linkPaths.add(fullPath);
						fileCount++;
					} else
						failCount++;
					System.out.println("Parsed: " + fileCount + " out of " + (fileCount + failCount) + " files");
				}
			}
		}
		// Compute page rank
		PageRanker ranker = new PageRanker(linkPaths, linkPatterns);
		List<Double> ranks = ranker.computePageRank();
		
		for (int index = 0; index< linkPaths.size(); index++) {
			org.apache.lucene.document.Document document  =  new  org.apache.lucene.document.Document();
			Field htmlTextField = new Field("htmlText", documentContents.get(index), Field.Store.NO, Field.Index.ANALYZED);
			htmlTextField.setBoost(new Float(ranks.get(index)));		// Set this as the page rank value for the webpage
			Field docTitleField = new Field("title", documentTitles.get(index), Field.Store.YES, Field.Index.ANALYZED);
			docTitleField.setBoost(new Float(ranks.get(index) * 2));
			String urlPath = "http://" + linkPatterns.get(index);
			Field urlPathField = new Field("url", urlPath, Field.Store.YES, Field.Index.ANALYZED);
			document.add(htmlTextField);
			document.add(docTitleField);
			document.add(urlPathField);
			indexWriter.addDocument(document);
			averageLength += documentContents.get(index).length();
		}
		averageLength /= indexWriter.numDocs();
		System.out.println("average length = "+averageLength);
		System.out.println("total "+indexWriter.numDocs()+" documents");
		indexWriter.close();
	}
    
    private Document addDocument(String domain, String path) throws IOException {
		String fullPath = path == "" ? docPath + "/" + domain : docPath + "/" + domain + "/" + path;
		if (!new File(fullPath).isFile()) {
			System.out.println("Unexpected missing file " + fullPath);
			return null;
		}
		Pattern p = Pattern.compile(".*\\.(htm|html)");
		Matcher m = p.matcher(fullPath);
		if (!m.find()) {
			return null;
		} 
    	p = Pattern.compile("charset\\s*=\\s*([0-9a-zA-Z]+)");
		m = p.matcher(new String(Files.readAllBytes(Paths.get(fullPath))));
		String encoding;
		if (m.find()) {
			String encodingStr = m.group(1);
			if (encodingStr.equalsIgnoreCase("gb2312") ||
					encodingStr.equalsIgnoreCase("gbk")) {
				encoding = "GBK";
			} else if (encodingStr.equalsIgnoreCase("utf") ||
					encodingStr.equalsIgnoreCase("utf8")) {
				encoding = "UTF-8";
			} else {
				return null;
			}
		} else {
			return null;
		}
		//System.out.println(encoding);
		File input = new File(fullPath); 
		Document doc = Jsoup.parse(input, encoding, "http://" + domain);
		return doc;
		//return doc.text();

		/*
		BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fullPath), encoding));
		String totalStr = "", str;
		while ((str = in.readLine()) != null) {
			if (str.length() > 50000) {
				continue;
			}
			int beginIndex = -1;
			for (int j = 0; j < str.length();) {
		        int codepoint = str.codePointAt(j);				        
		        if (codepoint >= 0x4e00 && codepoint <= 0x9fa5) {//aracter.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
		            if (beginIndex < 0)
		            	beginIndex = j;
		        } else {
		        	if (beginIndex >= 0) {
		        		if (j - beginIndex >= 4)
		        			totalStr += str.substring(beginIndex, j).trim() + " ";
		        		beginIndex = -1;
		        	}
		        }
		        j += Character.charCount(codepoint);
			}
		}
		in.close();
		return totalStr;
		*/
    }
    
	public static void main(String[] args) {
		Indexer indexer=new Indexer("forIndex/index");
		try {
			indexer.indexFiles();
		} catch (Exception e) {
			e.printStackTrace();
		}
		indexer.saveGlobals("forIndex/global.txt");
	}
}
