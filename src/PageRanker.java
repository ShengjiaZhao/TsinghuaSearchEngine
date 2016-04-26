import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PageRanker {
	private List<String> docPaths = new ArrayList<String>();
	private List<String> linkPatterns = new ArrayList<String>();
	private List<List<Integer>> linkPointers = new ArrayList<List<Integer>>();
	private List<Double> pageRanks = new ArrayList<Double>();
	private List<List<Integer>> inlinkPointers = new ArrayList<List<Integer>>();
	
	public PageRanker() {
		try {
			loadLinks();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public PageRanker(List<String> linkPath, List<String> linkPattern) {
		docPaths = linkPath;
		linkPatterns = linkPattern;
		for (int i = 0; i < docPaths.size(); i++) {
			linkPointers.add(new ArrayList<Integer>());
			inlinkPointers.add(new ArrayList<Integer>());
			pageRanks.add(0.0);
		}
	}
	
	public List<Double> computePageRank() throws IOException {
		for (int docIndex = 0; docIndex < docPaths.size(); docIndex++) {
			System.out.println(docIndex);
			String docPath = docPaths.get(docIndex);
			if (!new File(docPath).isFile()) {
				System.out.println("Error: unexpected missing file");
				continue;
			}
			String content = new String(Files.readAllBytes(Paths.get(docPath)));
			for (int index = 0; index < linkPatterns.size(); index++) {
				//System.out.println(linkPatterns.get(index));
				if (content.contains(linkPatterns.get(index))) {
					linkPointers.get(docIndex).add(index);
					System.out.print(index + " ");
				}
			}
			if (linkPointers.get(docIndex).size() != 0)
				System.out.println("");
		}
		outToInLinks();
		compute(0.8);
		saveLinks();
		return pageRanks;
	}
	
	private void outToInLinks() {
        // Change the link mapping to index
        for (int index = 0; index < linkPointers.size(); index++) {
            for (Integer pointer : linkPointers.get(index)) {
            	inlinkPointers.get(pointer).add(index);
            }
        }
	}
	
	private void compute(double alpha) {
		for (int index = 0; index < linkPointers.size(); index++)
			pageRanks.set(index, 1.0 / linkPointers.size());
		
        int iterCount = 0;
        boolean changed = true;
        while (changed) {
            double maxChange = 0.0;
            changed = false;
            for (int index = 0; index < linkPointers.size(); index++) {
                double linkSum = 0.0;
                for (Integer inlink : inlinkPointers.get(index)) {
                	linkSum += pageRanks.get(inlink) / linkPointers.get(inlink).size();
                }
                if (inlinkPointers.get(index).size()  != 0) {
                    double newRank = alpha / (double) linkPointers.size() + (1 - alpha) * linkSum;
                    double curChange = Math.abs(newRank - pageRanks.get(index)) / pageRanks.get(index);
                    if (curChange > 0.01)
                        changed = true;
                    if (curChange > maxChange) {
                        maxChange = curChange;
                    }
                    pageRanks.set(index, newRank);
                }
            }
            System.out.println(++iterCount + "-th iteration, max change is " + maxChange);
        }
	}
	
	private void saveLinks() throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("PageLink.txt")));
		for (int docIndex = 0; docIndex < docPaths.size(); docIndex++) {
			out.print(docIndex + " " + linkPatterns.get(docIndex) + " " + pageRanks.get(docIndex) + " ");
			for (Integer linkIndex : linkPointers.get(docIndex)) {
				out.print(linkIndex + " ");
			}
			out.println("");
		}
		out.close();
	}
	
	private void loadLinks() throws IOException {
		linkPointers.clear();
		linkPatterns.clear();
		pageRanks = new ArrayList<Double>();
		BufferedReader in = new BufferedReader(new FileReader("PageLink.txt"));
		String str;
		while ((str = in.readLine()) != null) {
			String[] strList = str.split(" ");
			if (strList.length < 1) {
				System.out.println("Incorrect input format");
				return;
			}
			List<Integer> ptrList = new ArrayList<Integer>();
			System.out.println(strList.length);
			for (int ptr = 3; ptr < strList.length; ptr++) {
				ptrList.add(Integer.parseInt(strList[ptr]));
			}
			linkPatterns.add(strList[1]);
			linkPointers.add(ptrList);
			inlinkPointers.add(new ArrayList<Integer>());
			pageRanks.add(Double.parseDouble(strList[2]));
		}
		outToInLinks();
	}
	
	private void debugPrint() {
		for (int index = 0; index < pageRanks.size(); index++) {
			System.out.println(index + " " + pageRanks.get(index) + " " + linkPatterns.get(index));
		}
	}
	public static void main(String[] args) {
		PageRanker ranker = new PageRanker();
		//ranker.compute(0.8);
		ranker.debugPrint();
	}
}
