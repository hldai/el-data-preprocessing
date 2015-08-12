// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import dcd.el.ELConsts;
import dcd.el.io.IOUtils;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;
import dcd.el.io.ItemWriter;
import dcd.el.io.PairListFile;
import dcd.el.io.PairListFile.StringDoubleArray;
import dcd.el.utils.TupleFileTools;
import dcd.el.wiki.WikiReaderStripped;

public class TfIdfGen {
	public static int MAX_WORD_LEN = 25;
	
	
	public static void genTfIdfFileMem(String wikiArticleWordCntFileName, String idfFileName,
			String dstFileName) {
		TfIdfExtractor tfIdfExtractor = new TfIdfExtractor(idfFileName);

		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dstFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		
		Item idItem = null, wcItem = null;
		int cnt = 0;
		ItemReader reader = new ItemReader(wikiArticleWordCntFileName, false);
		while ((idItem = reader.readNextItem()) != null) {
			++cnt;
			if (cnt % 100000 == 0)
				System.out.println(cnt);
			
			int wid = Integer.valueOf(idItem.value);
			
			reader.readNextItem();
			wcItem = reader.readNextItem();
			
			if (wcItem.numLines == 0) {
				continue;
			}
			
			String[] lines = wcItem.value.split("\n");
			TfIdfExtractor.WordCount[] wordCounts = new TfIdfExtractor.WordCount[lines.length];
			int docTermCnt = 0, indexCnt = 0;
			for (String line : lines) {
				String[] vals = line.split("\t");
				int termCnt = Integer.valueOf(vals[1]);
				docTermCnt += termCnt;
				
				int idx = tfIdfExtractor.getTermIndex(vals[0]);
				if (idx > -1) {
					TfIdfExtractor.WordCount wc = new TfIdfExtractor.WordCount();
					wc.count = termCnt;
					wc.index = idx;
					wordCounts[indexCnt++] = wc;
				}
			}
			
			if (indexCnt > 0) {
				Arrays.sort(wordCounts, 0, indexCnt);
				TfIdfFeature feature = tfIdfExtractor.getTfIdf(wordCounts, indexCnt, docTermCnt);
				try {
					dos.writeInt(wid);
				} catch (IOException e) {
					e.printStackTrace();
				}
				feature.toFile(dos);
			}
		}
		
		reader.close();
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public static void genTfIdfFile(String wikiArticleWordCntFileName, String idfFileName,
			String dstFileName) {
		System.out.println("Loading idf file...");
		StringDoubleArray sda = PairListFile.loadStringDoublePairFile(idfFileName);
		System.out.println("Done.");
		String[] terms = sda.keys;
		double[] idfVals = sda.values;
		
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dstFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Item idItem = null;
		Item wcItem = null;
		ItemReader reader = new ItemReader(wikiArticleWordCntFileName, false);
		int cnt = 0;
		int preWid = -1, wid = -1;
		boolean endLoop = false;
		while (!endLoop && (idItem = reader.readNextItem()) != null) {
			++cnt;
			if (cnt % 1000000 == 0)
				System.out.println(cnt);
			
			wid = Integer.valueOf(idItem.value);
			if (wid < preWid) {
				System.out.println("Article id not properly ordered!");
				break;
			}
			preWid = wid;

			reader.readNextItem();
			wcItem = reader.readNextItem();
			
			if (wcItem.numLines == 0) {
				continue;
			}
						
			String[] lines = wcItem.value.split("\n");
			int[] indices = new int[lines.length];
			int[] termCnts = new int[lines.length];
			int docTermCnt = 0, indexCnt = 0;
			int preIdx = -1;
			for (String line : lines) {
				String[] vals = line.split("\t");
				int termCnt = Integer.valueOf(vals[1]);
				docTermCnt += termCnt;
				
				int idx = Arrays.binarySearch(terms, vals[0]);
				if (idx > -1) {
					termCnts[indexCnt] = termCnt;
					indices[indexCnt++] = idx;
					if (idx < preIdx) {
						System.out.println("Terms not properly ordered!");
						endLoop = true;
						break;
					}
					preIdx = idx;
				}
			}
			
			if (indexCnt > 0)
				writeTfIdf(dos, wid, indexCnt, indices, termCnts, docTermCnt, idfVals);
//			if (cnt == 10) break;
		}
		reader.close();
		
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeTfIdf(DataOutputStream dos, int wid, int numTerms, int[] termIndices, int[] termCnts,
			int numDocTerms, double[] idfVals) {
		try {
			dos.writeInt(wid);
			dos.writeInt(numTerms);
			double tfidf = 0;
			
			for (int i = 0; i < numTerms; ++i) {
				dos.writeInt(termIndices[i]);
//				if (i > numTerms - 10) System.out.println(termIndices[i]);
			}
//			for (int idx : termIndices) {
//				dos.writeInt(idx);
//			}
			
			for (int i = 0; i < numTerms; ++i) {
				tfidf = (double)termCnts[i] / numDocTerms * idfVals[termIndices[i]];
				dos.writeDouble(tfidf);
//				if (i < 10) System.out.println(tfidf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genIdfFile(String wordCntFileName,
			String wikiArticleWordCntFileName, String dstFileName) {
		final String numDocFileName = ELConsts.TMP_FILE_PATH + "/num_docs.txt";
		final String termDocCntFileName = ELConsts.TMP_FILE_PATH + "/term_doc_cnt.txt";
		
		String[] words = null;
		int[] cnts = null;
		int numDocs = -1;
		
		System.out.println("Loading words...");
		words = loadWords(wordCntFileName);
		System.out.println("Done.");
		cnts = new int[words.length];
		
		numDocs = getTermDocCnts(wikiArticleWordCntFileName, words, cnts);
		
		writeTermDocCntFile(termDocCntFileName, words, cnts);
		
		IOUtils.writeIntValueFile(numDocFileName, numDocs);
		
//		numDocs = IOUtils.getIntValueFromFile(numDocFileName);
//		System.out.println("Num docs: " + numDocs);
//		PairListFile.StringIntArray sia = PairListFile.loadStringIntPairFile(termDocCntFileName);
//		words = sia.keys;
//		cnts = sia.values;
		
		genIdfFile(words, cnts, numDocs, dstFileName);
	}

	public static String[] loadWords(String wordCntFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(wordCntFileName);

		String[] words = null;
		String line = null;
		try {
			line = reader.readLine();
			int numWords = Integer.valueOf(line);
			words = new String[numWords];

			String[] vals = null;
			for (int i = 0; i < numWords; ++i) {
				line = reader.readLine();
				vals = line.split("\t");
				words[i] = vals[0];
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return words;
	}

	public static void filterFullWordCountFile(String wordCntFileName,
			String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(wordCntFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		if (writer == null)
			return;

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				int wc = Integer.valueOf(vals[1]);
				if (wc > 1 && vals[0].charAt(0) != '*'
						&& vals[0].charAt(0) != '&') {
					writer.write(line + "\n");
				}
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genFullWordCountFile(String wikiArticleWordCntFileName,
			String dstFileName) {
		System.out.println("Generating all words cnt tmp file...");
		final String allWordsTmpFilePath = ELConsts.TMP_FILE_PATH
				+ "/all_words_cnt.txt";
		genAllWordsTmpFile(wikiArticleWordCntFileName, allWordsTmpFilePath);
		System.out.println("Done.");

		final String sortedAllWordsTmpFilePath = ELConsts.TMP_FILE_PATH
				+ "/all_words_cnt_sorted.txt";
//		PairFileSort.pairFileSort(allWordsTmpFilePath, 0,
//				sortedAllWordsTmpFilePath);
		TupleFileTools.sort(allWordsTmpFilePath, sortedAllWordsTmpFilePath, new TupleFileTools.SingleFieldComparator(0));

		System.out.println("Merging...");
		mergeSortedWordCntFile(sortedAllWordsTmpFilePath, dstFileName);
		System.out.println("Done.");
	}

	public static void genWikiWordCountFile(String wikiTextFileName,
			String dstFileName) {
		WikiReaderStripped wrs = new WikiReaderStripped();
		wrs.open(wikiTextFileName, false, true);
		ItemWriter writer = new ItemWriter(dstFileName, true);

		int cnt = 0;
		Item wcItem = new Item(); // tmp
		wcItem.key = "WORDCOUNT";
		while (wrs.nextPage()) {
			// System.out.println(wrs.getTitle());

			TreeMap<String, Integer> m = BagOfWords.toBagOfWords(wrs.getText());

			wcItem.numLines = 0;
			StringBuilder wcList = new StringBuilder();
			boolean isFirst = true;
			for (Map.Entry<String, Integer> entry : m.entrySet()) {
				if (isFirst)
					isFirst = false;
				else
					wcList.append("\n");

				wcList.append(entry.getKey()).append("\t")
						.append(entry.getValue());
				++wcItem.numLines;
			}
			wcItem.value = new String(wcList);// (new String(wcList)).trim();
			writer.writeItem(wrs.getIdItem());
			writer.writeItem(wrs.getTitleItem());
			writer.writeItem(wcItem);

			++cnt;

			// if (cnt == 100) break;

			if (cnt % 1000000 == 0) {
				System.out.println(cnt);
			}
		}

		wrs.close();
		writer.close();
		System.out.println(cnt + " articles processed.");
	}

	private static void genAllWordsTmpFile(String wikiArticleWordCntFileName,
			String allWordsTmpFilePath) {
		ItemReader reader = new ItemReader(wikiArticleWordCntFileName, false);

		BufferedWriter writer = IOUtils.getUTF8BufWriter(
				allWordsTmpFilePath, false);

		Item wcListItem = null;

		int cnt = 0;
		while (reader.readNextItem() != null) {
			reader.readNextItem();
			wcListItem = reader.readNextItem();

			try {
				if (wcListItem.value.length() > 0) {
					String[] lines = wcListItem.value.split("\n");
					for (String line : lines) {
						String[] vals = line.split("\t");
						if (!wordShouldBeFiltered(vals[0])) {
							writer.write(line + "\n");
						}
					}
				}

				// writer.write(wcListItem.value);
				// int len = wcListItem.value.length();
				// if (len > 0 && wcListItem.value.charAt(len - 1) != '\n') {
				// writer.write("\n");
				// }
			} catch (IOException e) {
				e.printStackTrace();
			}

			++cnt;
			if (cnt % 1000000 == 0)
				System.out.println(cnt);
			// if (cnt == 50000) break;
		}

		reader.close();
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean wordShouldBeFiltered(String word) {
		if (word.length() > MAX_WORD_LEN)
			return true;

		if (word.length() <= 0 || word.charAt(0) == '#')
			return true;

		boolean hasEngChar = false;
		for (int i = 0; i < word.length(); ++i) {
			char ch = word.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
				hasEngChar = true;
			}
		}

		if (!hasEngChar)
			return true;

		if (word.startsWith("http://") || word.startsWith("https://"))
			return true;

		if (word.charAt(0) == '<' && word.charAt(word.length() - 1) == '>')
			return true;

		return false;
	}

	private static void mergeSortedWordCntFile(String wordCntFilePath,
			String dstFilePath) {
		BufferedReader reader = IOUtils.getUTF8BufReader(wordCntFilePath);
		BufferedWriter writer = IOUtils
				.getUTF8BufWriter(dstFilePath, false);

		String line = null;
		try {
			line = reader.readLine();
			String[] vals = line.split("\t");
			String curWord = vals[0];
			int curWordCnt = Integer.valueOf(vals[1]);

			while ((line = reader.readLine()) != null) {
				vals = line.split("\t");
				int wcnt = Integer.valueOf(vals[1]);

				if (vals[0].equals(curWord)) {
					curWordCnt += wcnt;
				} else {
					writer.write(curWord + "\t" + curWordCnt + "\n");
					curWord = vals[0];
					curWordCnt = wcnt;
				}
			}
			writer.write(curWord + "\t" + curWordCnt + "\n");

			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	
	private static void genIdfFile(String[] words, int[] cnts, int numDocs, String dstFileName) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			int len = words.length;
			double logNumDocs = Math.log(numDocs);
			for (int i = 0; i < len; ++i) {
				double idf = logNumDocs - Math.log(cnts[i]);
				
				writer.write(words[i] + "\t" + idf + "\n");
			}
			
			writer.close();
			
			IOUtils.writeNumLinesFileFor(dstFileName, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int getTermDocCnts(String wikiArticleWordCntFileName, String[] words, int[] cnts) {
		Item wcItem = null;
		ItemReader reader = new ItemReader(wikiArticleWordCntFileName, false);
		int cnt = 0;
		while (reader.readNextItem() != null) {
			reader.readNextItem();
			wcItem = reader.readNextItem();
			
			String[] lines = wcItem.value.split("\n");
			for (String line : lines) {
				String[] vals = line.split("\t");
				int pos = Arrays.binarySearch(words, vals[0]);
				if (pos > -1) {
					++cnts[pos];
				}
			}
			
			++cnt;
			if (cnt % 1000000 == 0)
				System.out.println(cnt);
//			if (cnt == 10) break;
		}
		reader.close();
		
		return cnt;
	}
	
	private static void writeTermDocCntFile(String fileName, String[] words, int[] cnts) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(fileName, false);
		try {
			int len = words.length;
			for (int i = 0; i < len; ++i) {
				writer.write(words[i] + "\t" + cnts[i] + "\n");
			}
			
			writer.close();
			
			IOUtils.writeNumLinesFileFor(fileName, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
