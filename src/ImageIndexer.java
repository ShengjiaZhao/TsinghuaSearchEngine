import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;   
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.parsers.*; 

public class ImageIndexer {
	private Analyzer analyzer; 
    private IndexWriter indexWriter;
    private float averageLength=1.0f;
    private String docPath = "/home/shengjia/heritrix-3.2.0/jobs/JobTest/latest/mirror";
    
    public ImageIndexer(String indexDir){
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
    public void indexFiles(){
		try{
			File topFolder = new File(docPath);
			if (!topFolder.exists()) {
				System.out.println("Document folder doesn't exist");
				return;
			} 
			int fileCount = 0, failCount = 0;
			averageLength = 0.0f;
			String[] domains = topFolder.list();
			for (String domain : domains) {
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
						if (this.addDocument(domain, curPath))
							fileCount++;
						else
							failCount++;
						System.out.println("Parsed: " + fileCount + " out of " + (fileCount + failCount) + " files");
					}
				}
			}
			averageLength /= indexWriter.numDocs();
			System.out.println("average length = "+averageLength);
			System.out.println("total "+indexWriter.numDocs()+" documents");
			indexWriter.close();
			return;
		}	catch(Exception e){
			e.printStackTrace();
		}			
	}
    
    private boolean addDocument(String domain, String path) throws IOException {
		String fullPath = docPath + "/" + domain + "/" + path;
		if (!new File(fullPath).isFile()) {
			System.out.println("Unexpected missing file " + fullPath);
			return false;
		}
		Pattern p = Pattern.compile(".*\\.(htm|html)");
		Matcher m = p.matcher(path);
		if (!m.find()) {
			return false;
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
				encoding = "UTF8";
			} else {
				return false;
			}
		} else {
			return false;
		}
		//System.out.println(encoding);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fullPath), encoding));
		String totalStr = "", str;
		while ((str = in.readLine()) != null) {
			//System.out.println(str);
			//System.out.println(str.length());
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
		//System.out.println(totalStr);
		Document document  =   new  Document();
		Field htmlTextField = new Field("htmlText" , totalStr ,Field.Store.YES, Field.Index.ANALYZED);
		htmlTextField.setBoost(1.0f);		// Set this as the page rank value for the webpage
		String urlPath = "http://" + domain + "/" + path;
		Field urlPathField = new Field("url", urlPath, Field.Store.YES, Field.Index.ANALYZED);
		System.out.println(urlPath);
		document.add(htmlTextField);
		document.add(urlPathField);
		indexWriter.addDocument(document);
		averageLength += totalStr.length();
		return true;
    }
    
	public static void main(String[] args) {
		ImageIndexer indexer=new ImageIndexer("forIndex/index");
		indexer.indexFiles();
		indexer.saveGlobals("forIndex/global.txt");
	}
}
