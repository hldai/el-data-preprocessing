// author: DHL brnpoem@gmail.com

package dcd.el.wiki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.zju.dcd.edl.ELConsts;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TupleFileTools;
import dcd.el.feature.TfIdfExtractor;
import dcd.el.feature.TfIdfFeature;
import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;
import dcd.el.io.ItemWriter;

public class WikiTools {
	public static final String ANCHOR_CNTS_ITEM_KEY = "ANCHOR_CNTS";
	
	public static class Anchor implements Comparable<Anchor> {

		@Override
		public int compareTo(Anchor anchorRight) {
			int cmp = target.compareTo(anchorRight.target);
			if (cmp != 0)
				return cmp;
			return desc.compareTo(anchorRight.desc);
		}

		public boolean equals(Anchor anchorRight) {
			return target.equals(anchorRight.target)
					&& desc.equals(anchorRight.desc);
		}

		public String target = null;
		public String desc = null;
	}
	
	public static class TitleWidEntry implements Comparable<TitleWidEntry> {
		public ByteArrayString title = null;
		public int wid = 0;
		
		@Override
		public int compareTo(TitleWidEntry er) {
			return title.compareTo(er.title);
		}
	}
	
	public static class AnchorCntComparator implements Comparator<String> {

		@Override
		public int compare(String sl, String sr) {
			String[] valsl = sl.split("\t"), valsr = sr.split("\t");
			int widl = Integer.valueOf(valsl[0]), widr = Integer.valueOf(valsr[0]);
			if (widl != widr)
				return widl - widr;
			return valsl[1].compareTo(valsr[1]);
		}
	}
	
	private static class TfIdfEntry implements Comparable<TfIdfEntry> {
		public int termIndex;
		public float tf;
		public float idf;
		public float tfIdf;
		
		@Override
		public int compareTo(TfIdfEntry entry) {
			if (tfIdf < entry.tfIdf) {
				return 1;
			}
			return tfIdf == entry.tfIdf ? 0 : -1;
			
//			if (idf > entry.idf) {
//				return 1;
//			}
//			return idf == entry.idf ? 0 : -1;
		}
	}
	
	public static void test() {
//		TitleToWid titleToWid = new TitleToWid("d:/data/el/wiki/title_wid_with_redirect.bsi");
//		int wid = titleToWid.getWid("Anarchist");
//		System.out.println(wid);
		
//		String articleAnchorFileName = "d:/data/el/json_wiki/article_anchor_list.txt";
//		BufferedWriter writer = IOUtils.getUTF8BufWriter("d:/data/el/results_1.txt", false);
		String articleAnchorFileName = "d:/data/el/wiki/cleaned_article_anchors.txt";
		BufferedWriter writer = IOUtils.getUTF8BufWriter("d:/data/el/results_0.txt", false);
		ItemReader itemReader = new ItemReader(articleAnchorFileName, false);
		Item idItem = null;
		int cnt = 0;
		while ((idItem = itemReader.readNextItem()) != null) {
			itemReader.readNextItem();
			
			Item anchorsItem = itemReader.readNextItem();
			String[] lines = anchorsItem.value.split("\n");
			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				
				String lc = line.toLowerCase();
				String desc = CommonUtils.getFieldFromLine(lc, 1);
				if (desc.equals("anarchism")) {
					try {
						writer.write(idItem.value + "\n");
						writer.write(line + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			++cnt;
			if (cnt % 100000 == 0) {
				System.out.println(cnt);
			}
		}
		
		itemReader.close();
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static final String LINK_PATTERN = "\\[\\[(.*?)\\|(.*?)\\]\\]";
	private static final Pattern REDIRECT_PAGE_PATTERN = Pattern.compile("<redirect\\s*title=\"(.*?)\"\\s*/>");
	
	private static final int MIN_NUM_WORDS_IN_TRAINING_SENTENCE = 10;
	
	private static final String TEXT_PAGE_PATTERN = "\\s*<page>.*?" + "<title>(.*?)</title>.*?"
			+ "<id>(.*?)</id>.*?" + "<revision>\\s*" + "<id>(.*?)</id>.*?"
			+ "<timestamp>(.*?)</timestamp>.*?"
			+ "<text\\s*xml:space=\"preserve\">(.*?)</text>.*?"
			+ "<sha1>(.*?)</sha1>.*?" + "</page>";
	
	private static final String EXTRACTED_PAGE_PATTERN = "<doc.*?<page>\\s*"
			+ "<title>(.*?)</title>.*?"
			+ "<id>(\\d*?)</id>"
			+ ".*?<text xml:space=\"preserve\">(.*?)</text>.*?</doc>\\s*";
	
	private static final String CATEGORE_LINK_PATTERN = "\\[\\[Category:.*?\\]\\]";
	private static final String FILE_LINK_PATTERN = "\\[\\[File:.*?\\]\\]";
	
	private static StringBuilder nextExtractedPage(BufferedReader reader) throws IOException {
		String line = null;
		StringBuilder page = new StringBuilder();
		boolean hasStarted = false;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("<doc id=")) {
				hasStarted = true;
			}
			
			if (hasStarted) {
				page.append(line).append("\n");
				if (line.equals("</doc>")) {
					break;
				}
			}
		}
		
		return hasStarted ? page : null;
	}
	
	public static void extractKeywordsForEntities(String wikiArticleWordCntFileName, 
			String idfFileName, String dstFileName) {
		final float minIdf = 2;
		TfIdfExtractor tfIdfExtractor = new TfIdfExtractor(idfFileName);
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		int cnt = 0;
		Item idItem = null, wcItem = null;
		ItemReader reader = new ItemReader(wikiArticleWordCntFileName, false);
		while ((idItem = reader.readNextItem()) != null) {
			++cnt;
			if (cnt % 100000 == 0)
				System.out.println(cnt);
			
			reader.readNextItem();

			int wid = Integer.valueOf(idItem.value);
			
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
//				Arrays.sort(wordCounts, 0, indexCnt);
				TfIdfFeature feature = tfIdfExtractor.getTfIdf(wordCounts, indexCnt, docTermCnt);
				TfIdfEntry[] tfIdfEntries = new TfIdfEntry[feature.termIndices.length];
				for (int i = 0; i < feature.termIndices.length; ++i) {
					tfIdfEntries[i] = new TfIdfEntry();
					tfIdfEntries[i].termIndex = feature.termIndices[i];
					tfIdfEntries[i].tf = feature.tfs[i];
					tfIdfEntries[i].idf = feature.idfs[i];
					tfIdfEntries[i].tfIdf = feature.tfs[i] * feature.idfs[i];
				}
				
				Arrays.sort(tfIdfEntries);
				writeEntityKeyWords(writer, wid, tfIdfEntries, tfIdfExtractor, minIdf);
//				System.out.println(wid);
//				for (int i = 0; i < 50 && i < tfIdfEntries.length; ++i) {
//					if (tfIdfEntries[i].idf < minIdf)
//						continue;
//					System.out.println(tfIdfExtractor.getTerm(tfIdfEntries[i].termIndex)
//							+ "\t" + tfIdfEntries[i].tf + "\t" + tfIdfEntries[i].idf);
//				}
			}
			
//			if (cnt == 10)
//				break;
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeEntityKeyWords(BufferedWriter writer, int wid, TfIdfEntry[] tfIdfEntries,
			TfIdfExtractor tfIdfExtractor, float minIdf) {
		try {
			writer.write(wid + "\n");

			for (int i = 0; i < 50 && i < tfIdfEntries.length; ++i) {
				if (tfIdfEntries[i].idf < minIdf)
					continue;
				if (i != 0)
					writer.write(" ");
				writer.write(tfIdfExtractor.getTerm(tfIdfEntries[i].termIndex).toString());
			}
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void cleanWikiTextData(String wikiTextFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(wikiTextFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		
		StringBuilder page = null;
		Pattern p = Pattern.compile(EXTRACTED_PAGE_PATTERN, Pattern.DOTALL);
		long cnt = 0, missMatchCnt = 0;
		try {
			while ((page = nextExtractedPage(reader)) != null) {
				Matcher m = p.matcher(page);
				if (m.matches()) {
					String title = m.group(1), id = m.group(2), text = m.group(3).trim();
					if (title.contains("\n")) {
						System.out.println("Title has \\n!");
						break;
					}
					if (title.startsWith("Wikipedia:")
							|| text.startsWith("#REDIRECT")) {
						continue;
					}
					
					text = text.replaceAll(FILE_LINK_PATTERN, "");
					text = text.replaceAll(CATEGORE_LINK_PATTERN, "");
					writer.write(title + "\n");
					writer.write(id + "\n");
					int numLines = CommonUtils.countLines(text);
					writer.write((numLines + 1) + "\n");
					writer.write(text + "\n");
				} else {
					++missMatchCnt;
				}
				
				++cnt;
//				if (cnt == 100)
//					break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}
			
			reader.close();
			writer.close();

			System.out.println(cnt + " pages");
			System.out.println(missMatchCnt + " missed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genTrainingDataFromWiki(String allWikiTextFileName, 
			String midToWidFileName, String widTitleFileName, String dstFileName) {
		int[] wids = loadMappedWids(midToWidFileName);
		TitleWidEntry[] titleWidEntries = loadTitleWidEntries(wids, widTitleFileName);
		System.out.println(titleWidEntries.length + " title wid pairs");
		
		Arrays.sort(titleWidEntries);
		
		BufferedReader reader = IOUtils.getUTF8BufReader(allWikiTextFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		try {
			long cnt = 0;
			while (reader.readLine() != null) {  // read title
				reader.readLine();  // read wid
				int numLines = Integer.valueOf(reader.readLine());
				String line = null;
				Pattern linkPattern = Pattern.compile(LINK_PATTERN);
				for (int i = 0; i < numLines; ++i) {
					line = reader.readLine();
					genTrainingDataFromTextWithLinks(line, linkPattern, titleWidEntries, writer);
				}
				
				++cnt;
//				if (cnt == 10)
//					break;
				if (cnt % 10000 == 0)
					System.out.println(cnt);
			}
			
//			int cnt = 0;
//			String docText = null;
//			Pattern pagePattern = Pattern.compile(TEXT_PAGE_PATTERN, Pattern.DOTALL);
//			Pattern linkPattern = Pattern.compile(LINK_PATTERN);
//			while ((docText = nextDoc(reader)) != null) {
//				if (pageIsRedirect(docText))
//					continue;
////				System.out.println(docText);
//				Matcher matcher = pagePattern.matcher(docText);
//				if (matcher.find()) {
////					System.out.println(matcher.group(5));
//					String[] lines = matcher.group(5).split("\n");
//					for (String line : lines) {
//						genTrainingDataFromTextWithLinks(line, linkPattern, titleWidEntries, writer);
//					}
//				}
//				
//				++cnt;
////				if (cnt == 2)
////					break;
//				if (cnt % 100000 == 0)
//					System.out.println(cnt);
//			}
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int[] loadMappedWids(String midToWidFileName) {
		int numLines = IOUtils.getNumLinesFor(midToWidFileName);
		int[] wids = new int[numLines];
		
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWidFileName);
		try {
			String line = null;
			for (int i = 0; i < numLines; ++i) {
				line = reader.readLine();
				wids[i] = Integer.valueOf(CommonUtils.getFieldFromLine(line, 1));
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return wids;
	}
	
	private static TitleWidEntry[] loadTitleWidEntries(int[] wids, String widTitleFileName) {
//		TitleWidEntry[] entries = new TitleWidEntry[wids.length];
		LinkedList<TitleWidEntry> entryList = new LinkedList<TitleWidEntry>();
		BufferedReader reader = IOUtils.getUTF8BufReader(widTitleFileName);
		
		try {
			String line = null;
			int widPos = 0;
			while ((line = reader.readLine()) != null) {
				int curWid = Integer.valueOf(CommonUtils.getFieldFromLine(line, 0));
				if (curWid > wids[widPos]) {
					while (widPos < wids.length && curWid > wids[widPos])
						++widPos;
				}
				
				if (widPos >= wids.length)
					break;
				
				if (curWid == wids[widPos]) {
					TitleWidEntry entry = new TitleWidEntry();
					entry.wid = curWid;
					entry.title = new ByteArrayString(CommonUtils.getFieldFromLine(line, 1));
					entryList.add(entry);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return entryList.toArray(new TitleWidEntry[entryList.size()]);
	}
	
	private static void genTrainingDataFromTextWithLinks(String text, Pattern linkPattern, 
			TitleWidEntry[] titleWidEntries, BufferedWriter writer) {
		if (CommonUtils.countWords(text) < MIN_NUM_WORDS_IN_TRAINING_SENTENCE) {
			return;
		}
		
		StringBuilder textBuilder = new StringBuilder();
		StringBuilder mentions = new StringBuilder();
		Matcher linkMatcher = linkPattern.matcher(text);
		TitleWidEntry tmpEntry = new TitleWidEntry();
		int begPos = 0, mentionCnt = 0, left = 0, right = 0;
		while (linkMatcher.find()) {
			textBuilder.append(text.substring(begPos, linkMatcher.start()));
			left = textBuilder.length();
			begPos = linkMatcher.end();
			textBuilder.append(linkMatcher.group(2));
			right = textBuilder.length() - 1;
			
			if (linkMatcher.group(1).length() == 0)
				continue;
			
			StringBuilder targetBuilder = new StringBuilder();
			targetBuilder.append(Character.toUpperCase(linkMatcher.group(1).charAt(0)));
			if (linkMatcher.group(1).length() > 1) {
				targetBuilder.append(linkMatcher.group(1).substring(1));
			}
			String target = new String(targetBuilder);
			
			tmpEntry.title = new ByteArrayString(target);
			int pos = Arrays.binarySearch(titleWidEntries, tmpEntry);
			if (pos > -1) {
				mentions.append(titleWidEntries[pos].wid + "\n");
				mentions.append(left + "\t" + right + "\n");
				++mentionCnt;
			}
		}
		
		if (mentionCnt > 0) {
			if (begPos < text.length()) {
				textBuilder.append(text.substring(begPos));
			}
			
			String newText = new String(textBuilder);
			if (CommonUtils.countWords(newText) < MIN_NUM_WORDS_IN_TRAINING_SENTENCE) {
				return;
			}
			
			try {
				writer.write(new String(textBuilder) + "\n");
				writer.write(mentionCnt + "\n");
				writer.write(new String(mentions));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean pageIsRedirect(String page) {
		Matcher matcher = REDIRECT_PAGE_PATTERN.matcher(page);
		if (matcher.find())
			return true;
		return false;
	}
	
	private static String nextDoc(BufferedReader reader) {
		StringBuilder text = null;
		String line = null;
		try {
			boolean flg = false;
			while ((line = reader.readLine()) != null) {
				if (flg) {
					if (line.equals("</doc>")) {
						break;
					}
					text.append(line).append("\n");
				} else if (line.startsWith("<doc ")) {
					reader.readLine();
					flg = true;
					text = new StringBuilder();
				}
			}
			
			if (!flg)
				return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(text);
	}
	
	public static void genAnchorCntFile(String articleAnchorFileName, String titleWidFileName, String dstFileName) {
		TitleToWid titleToWid = new TitleToWid(titleWidFileName);
		
		String tmpFilePath0 = Paths.get(ELConsts.TMP_FILE_PATH, "anchor_cnts.txt").toString();
		System.out.println("gen unreduced anchor cnts file...");
		genUnreducedAnchorCnts(articleAnchorFileName, titleToWid, tmpFilePath0);
		System.out.println("done.");
		
		String tmpFilePath1 = Paths.get(ELConsts.TMP_FILE_PATH, "archor_cnts_sorted.txt").toString();
		System.out.println("sorting...");
		TupleFileTools.sort(tmpFilePath0, tmpFilePath1, new AnchorCntComparator());
		System.out.println("done.");
		
		reduceSortedAnchorCntsFile(tmpFilePath1, dstFileName);
	}
	
	private static void reduceSortedAnchorCntsFile(String fileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		try {
			String line = null;
			String curWid = null, curDesc = null;
			int curAnchorCnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				if (curWid == null || !(curWid.equals(vals[0]) && curDesc.equals(vals[1]))) {
					if (curWid != null) {
						writer.write(curWid + "\t" + curDesc + "\t" + curAnchorCnt + "\n");
					}
					
					curWid = vals[0];
					curDesc = vals[1];
					curAnchorCnt = Integer.valueOf(vals[2]);
				} else {
					curAnchorCnt += Integer.valueOf(vals[2]);
				}
			}
			writer.write(curWid + "\t" + curDesc + "\t" + curAnchorCnt + "\n");
			
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void genUnreducedAnchorCnts(String articleAnchorFileName, TitleToWid titleToWid, String dstFileName) {
		ItemReader itemReader = new ItemReader(articleAnchorFileName, false);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		try {
			int cnt = 0;
			while (itemReader.readNextItem() != null) {
				itemReader.readNextItem();
				
				Item anchorsItem = itemReader.readNextItem();
				String[] lines = anchorsItem.value.split("\n");
				for (String line : lines) {
					line = line.trim();
					if (line.equals(""))
						continue;
					
					String[] vals = line.split("\t");
					if (vals.length != 3) {
						System.out.println("bad format");
						System.out.println(line);
					} else {
						int wid = titleToWid.getWid(vals[0]);
						if (wid > 0) {
//							writer.write(wid + "\t" + vals[1].toLowerCase() + "\t" + vals[2] + "\n");
							writer.write(wid + "\t" + vals[1] + "\t" + vals[2] + "\n");
						}
					}
				}
				
				++cnt;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		itemReader.close();
	}
	
	public static void genTitleWidFileWithRedirect(String titleWidFileName, String redirectFileName,
			String dstTextFileName,
			String dstFileName) {
		TitleToWid titleToWid = new TitleToWid(titleWidFileName);
		LinkedList<TitleWidEntry> titleWidList = new LinkedList<TitleWidEntry>();
		for (int i = 0; i < titleToWid.titles.length; ++i) {
			TitleWidEntry entry = new TitleWidEntry();
			entry.title = titleToWid.titles[i];
			entry.wid = titleToWid.wids[i];
			titleWidList.add(entry);
		}

		System.out.println("processing redirect file...");
		BufferedReader reader = IOUtils.getUTF8BufReader(redirectFileName);
		int lineCnt = 0, hitCnt = 0;
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				++lineCnt;
				String[] vals = line.split("\t");
				if (vals.length != 2) {
					System.out.println("bad format");
				} else {
					int wid = titleToWid.getWid(vals[1]);
					if (wid > 0) {
						TitleWidEntry entry = new TitleWidEntry();
						entry.title = new ByteArrayString(vals[0]);
						entry.wid = wid;
						titleWidList.add(entry);
						++hitCnt;
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
		System.out.println(lineCnt + " lines. " + hitCnt + " hit.");
		
		System.out.println("sorting...");
		Collections.sort(titleWidList);
		System.out.println("done.");
		
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(titleWidList.size());
			for (TitleWidEntry entry : titleWidList) {
				entry.title.toFileWithShortLen(dos);
				dos.writeInt(entry.wid);
			}
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH, "tmp_wid_title.txt").toString();
		BufferedWriter writer = IOUtils.getUTF8BufWriter(tmpFileName, false);
		try {
			for (TitleWidEntry entry : titleWidList) {
				writer.write(entry.wid + "\t" + entry.title.toString() + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Comparator<String> cmp = new TupleFileTools.SingleFieldComparator(0, 
				new CommonUtils.StringToIntComparator());
		TupleFileTools.sort(tmpFileName, dstTextFileName, cmp);
	}
	
	public static void genTitleToWidFile(String widTitleTextFileName, String dstFileName) {
		int numLines = IOUtils.getNumLinesFor(widTitleTextFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(widTitleTextFileName);
		TitleWidEntry[] entries = new TitleWidEntry[numLines];
		int cnt = 0;
		try {
			System.out.println("reading...");
			for (int i = 0; i < numLines; ++i) {
				String line = reader.readLine();
				String[] vals = line.split("\t");
				if (vals.length != 2) {
					System.out.println("format not right");
				} else {
					entries[cnt] = new TitleWidEntry();
					entries[cnt].wid = Integer.valueOf(vals[0]);
					entries[cnt].title = new ByteArrayString(vals[1]);
					++cnt;
				}
			}
			
			reader.close();
			System.out.println("done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Arrays.sort(entries, 0, cnt);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(cnt);
			for (int i = 0; i < cnt; ++i) {
				entries[i].title.toFileWithShortLen(dos);
				dos.writeInt(entries[i].wid);
			}
			
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void cleanArticleAnchorFile(String fileName,
			String dstFileName) {
		ItemReader reader = new ItemReader(fileName, false);
		ItemWriter writer = new ItemWriter(dstFileName, false);
		// BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		Item idItem = null, titleItem = null;
		Item anchorCntsItem = new Item();
		anchorCntsItem.key = ANCHOR_CNTS_ITEM_KEY;
		Item anchorListItem = null;
		int cnt = 0, wcnt = 0;
		boolean flg = true;
		while ((idItem = reader.readNextItem()) != null && flg) {
			titleItem = reader.readNextItem();
			anchorListItem = reader.readNextItem();

			String[] lines = anchorListItem.value.split("\n");
			Anchor[] anchors = new Anchor[lines.length];
			int anchorCnt = 0;
			for (String line : lines) {
				line = CommonUtils.unescapeHtml(line).trim();
				if (line.length() == 0)
					continue;

				Anchor anchor = getAnchorFromPairLine(line);
				if (anchor == null)
					continue;
				
				anchors[anchorCnt++] = anchor;
				if (anchors[anchorCnt - 1].target.equals("Binomial theorem") && anchors[anchorCnt - 1].desc.equals("Newton")) {
					System.out.println(idItem.value);
					System.out.println(titleItem.value);
				}
			}

			writer.writeItem(idItem);
			writer.writeItem(titleItem);
			anchorCntsItem.value = getAnchorCntsString(anchors, anchorCnt).trim();
			writer.writeItem(anchorCntsItem);

			++cnt;
//			if (cnt == 10)
//				break;
			if (cnt % 100000 == 0)
				System.out.println(cnt);
		}

		reader.close();
		writer.close();

		System.out.println(cnt + " pages processed.");
		System.out.println(wcnt + " lines written.");
	}

	private static Anchor getAnchorFromPairLine(String line) {
		Anchor anchor = null;
		if (line.contains("\t")) {
			String[] vals = line.split("\t");
			if (vals.length > 2) {
				System.out.println("More than two \\t!");
			} else {
				String val0 = vals[0].trim();
				if (val0.length() > 0) {
					anchor = new Anchor();
					anchor.target = new String(firstCharToUpperCase(val0));
					anchor.desc = vals[1].trim();
				}
			}
		} else {
			anchor = new Anchor();
			anchor.target = firstCharToUpperCase(line);
			anchor.desc = line;
		}
		return anchor;
	}
	
	private static String firstCharToUpperCase(String str) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(Character.toUpperCase(str.charAt(0)));
		if (str.length() > 1)
			stringBuilder.append(str.substring(1));
		return new String(stringBuilder);
	}

	private static String getAnchorCntsString(Anchor[] anchors, int anchorsCnt) {
		Arrays.sort(anchors, 0, anchorsCnt);
		StringBuilder stringBuilder = new StringBuilder();
		int curAnchorCnt = 0;
		Anchor curAnchor = null;
		for (int i = 0; i < anchorsCnt; ++i) {
			Anchor anchor = anchors[i];
			if (curAnchor == null) {
				curAnchor = anchor;
				curAnchorCnt = 1;
			} else {
				if (anchor.equals(curAnchor)) {
					++curAnchorCnt;
				} else {
					stringBuilder.append(curAnchor.target).append("\t")
							.append(curAnchor.desc).append("\t")
							.append(curAnchorCnt).append("\n");
					curAnchor = anchor;
					curAnchorCnt = 1;
				}
			}
		}
		if (curAnchor != null)
			stringBuilder.append(curAnchor.target).append("\t")
					.append(curAnchor.desc).append("\t").append(curAnchorCnt)
					.append("\n");
		return new String(stringBuilder);
	}

	public static void checkDumpSymbols(String wikiFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(wikiFileName);

		try {
			String line = null;

			long cnt = 0;
			while ((line = reader.readLine()) != null) {
				if (line.contains("&apos;&apos;")
						|| line.contains("&#39;&#39;")) {
					System.out.println("double apos! " + line);
					break;
				}
				if (line.contains("&#91;&#91;")) {
					System.out.println("double [! " + line);
					break;
				}

				++cnt;
				// System.out.println(line);
				// if (cnt == 10) break;
			}

			reader.close();

			System.out.println(cnt + " lines checked.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void extractXMLPages(String gzWikiFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(gzWikiFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			// String page = WikiReaderXML.nextPage(reader);
			int cnt = 0;
			String page = null;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				writer.write(page);
				++cnt;

				// if (cnt == 10) break;
				if (cnt % 100000 == 0) {
					System.out.println(cnt);
				}
			}

			reader.close();
			writer.close();

			System.out.println(cnt + " pages read and written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void filterRedirect(String gzWikiFileName,
			String dstFileName, String redListFileName, String weirdPageFileName) {
		// BufferedReader reader = IOUtils.getGZIPBufReader(gzWikiFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(gzWikiFileName);
		BufferedWriter nredWikiWriter = IOUtils.getUTF8BufWriter(dstFileName);
		BufferedWriter redListWriter = IOUtils
				.getUTF8BufWriter(redListFileName);
		BufferedWriter wpWriter = IOUtils.getUTF8BufWriter(weirdPageFileName);

		if (nredWikiWriter == null || redListWriter == null || wpWriter == null)
			return;

		try {
			Matcher m = null;
			String page = null;
			String title = null;
			long cnt = 0, redCnt = 0;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				title = getTitle(page);
				if (title == null) {
					System.out.println("Failed to find title!");
					wpWriter.write(page);
				}

				m = REDIRECT_PAGE_PATTERN.matcher(page);
				if (m.find()) {
					redListWriter.write(title + "\t" + m.group(1) + "\n");
					++redCnt;
					// System.out.println(m.group(1));
				} else {
					nredWikiWriter.write(page);
				}

				++cnt;
				// if (cnt == 10) break;
				if (cnt % 100000 == 0) {
					System.out.println(cnt);
				}
			}

			reader.close();
			nredWikiWriter.close();
			redListWriter.close();
			wpWriter.close();

			System.out.println(cnt + " pages processed.");
			System.out.println(redCnt + " redirect pages.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void stripWikiXML(String xmlWikiFileName, String dstFileName,
			String weirdPageFileName) {
		stripWikiXML(xmlWikiFileName, dstFileName, weirdPageFileName, false);
	}

	public static void searchWikiXMLPageById(String fileName, String wid,
			String dstFileName) {
//		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedReader reader = IOUtils.getGZIPBufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		if (writer == null)
			return;

		try {
			String page = null;
			Matcher m = null;
			boolean endLoop = false;
			int cnt = 0;
			while (!endLoop && (page = WikiReaderXML.nextPage(reader)) != null) {
				m = matchPage(page);
				if (m.find()) {
					if (m.group(2).trim().equals(wid)) {
						System.out.println("HIT");
						endLoop = true;
					}
					
					++cnt;
					if (cnt % 100000 == 0) {
						System.out.println(cnt + ": " + m.group(2).trim());
					}
				}
			}

			reader.close();

			// article is found.
			if (endLoop) {
				writer.write(page);
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void stripWikiXML(String xmlWikiFileName, String dstFileName,
			String weirdPageFileName, boolean withId) {
		BufferedReader reader = IOUtils.getUTF8BufReader(xmlWikiFileName);
		BufferedWriter dstWriter = IOUtils.getUTF8BufWriter(dstFileName);
		BufferedWriter wpWriter = IOUtils.getUTF8BufWriter(weirdPageFileName);

		if (dstWriter == null || wpWriter == null) {
			return;
		}

		try {
			String page = null;
			long cnt = 0, matchedCnt = 0;
			Matcher m = null;
			int numTitleLines = 0, numTextLines = 0;
			String title = null, text = null;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				m = matchPage(page);
				if (m.find()) {
					if (withId) {
						String id = m.group(2).trim();

						if (!idOK(id)) {
							System.out.println(id + " #id not OK");
							wpWriter.write(page);
						} else {
							dstWriter.write("ID 1\n");
							dstWriter.write(id + "\n");
						}
					}

					title = m.group(1).trim();
					text = m.group(5).trim();

					numTitleLines = CommonUtils.countLines(title);
					if (numTitleLines != 0) {
						System.out.println("title line larger than 0");
						wpWriter.write(page);
						continue;
					}

					dstWriter.write("TITLE 1\n");
					dstWriter.write(title + "\n");

					// will add a '\n' at the end
					numTextLines = CommonUtils.countLines(text) + 1;
					dstWriter.write("TEXT " + numTextLines + "\n");
					dstWriter.write(text + "\n");

					++matchedCnt;
				} else {
					System.out.println("Unmatched page!");
					wpWriter.write(page);
				}

				++cnt;

				// if (cnt == 15) break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}

			reader.close();
			dstWriter.close();
			wpWriter.close();

			System.out.println(cnt + " pages processed.\n" + matchedCnt
					+ " pages matched and written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genAnchorList(String wikiFilePath,
			String anchorListFilePath) {
		WikiReaderStripped wrs = new WikiReaderStripped();
		wrs.open(wikiFilePath, false, true);

		ItemWriter writer = new ItemWriter(anchorListFilePath, false);
//		writer.open(anchorListFilePath);

		Item anchorListItem = new Item();
		anchorListItem.key = "ANCHOR";
		int cnt = 0;
		while (wrs.nextPage()) {
			// System.out.println(wrs.getTitleItem().value);
			writer.writeItem(wrs.getIdItem());
			writer.writeItem(wrs.getTitleItem());

			anchorListItem.value = getAnchorList(wrs.getTextItem().value);
			writer.writeItem(anchorListItem);

			++cnt;
			// if (cnt == 2) break;
			if (cnt % 100000 == 0)
				System.out.println(cnt);
		}

		wrs.close();
		writer.close();

		System.out.println(cnt + " pages processed.");
	}

	// TODO remove
	public static void genAnchorAliasList(String anchorListFilePath,
			String dstFilePath) {
		ItemReader reader = new ItemReader(anchorListFilePath, false);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFilePath);

		Item anchorListItem = null;
		int cnt = 0, wcnt = 0;
		try {
			while (reader.readNextItem() != null) {
				reader.readNextItem();
				anchorListItem = reader.readNextItem();

				String[] lines = anchorListItem.value.split("\n");
				for (String line : lines) {
					if (line.contains("\t")) {
						String[] vals = line.split("\t");
						if (vals.length > 2) {
							System.out.println("More than two \\t!");
						} else {
							writer.write(vals[1] + "\t" + vals[0] + "\n");
							++wcnt;
						}
					}
				}

				++cnt;
				// if (cnt == 10) break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		reader.close();
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(cnt + " pages processed.");
		System.out.println(wcnt + " lines written.");
	}

	public static void mergeCharListFiles(String file0, String file1,
			String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(file0);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		int cnt = 0;
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\n");

				++cnt;
			}
			System.out.println(cnt + " lines processed.");

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		reader = IOUtils.getUTF8BufReader(file1);

		try {
			cnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				if (vals.length == 3) {
					writer.write(vals[2] + "\t" + vals[0] + "\n");
				}

				++cnt;
			}
			System.out.println(cnt + " lines processed.");

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getAnchorList(String text) {
		StringBuilder sb = new StringBuilder();

		// Pattern p = Pattern
		// .compile("\\[\\[([^\\[\\]]*?)\\|([^\\[\\]]*?)\\]\\]");
		Pattern p = Pattern.compile("\\[\\[([^\\[\\]]*?)\\]\\]");
		Matcher m = p.matcher(text);
		boolean isFirst = true;
		while (m.find()) {
			String mstr = m.group(1);
			int vbarPos = 0;
			while (vbarPos < mstr.length() && mstr.charAt(vbarPos) != '|')
				++vbarPos;

			String target = mstr.substring(0, vbarPos).trim();
			// System.out.println(target);
			if (legalName(target) && properTarget(target)) {
				if (isFirst)
					isFirst = false;
				else
					sb.append("\n");

				sb.append(target);

				if (vbarPos == mstr.length())
					continue;

				String name = mstr.substring(vbarPos + 1, mstr.length()).trim();
				if (legalName(name)) {
					sb.append("\t").append(name);
				}
			}
		}

		return new String(sb);
	}

	private static boolean idOK(String id) {
		for (int i = 0; i < id.length(); ++i) {
			if (id.charAt(i) < '0' || id.charAt(i) > '9') {
				return false;
			}
		}

		return true;
	}

	private static String getTitle(String page) {
		Pattern p = Pattern.compile("<title>(.*?)</title>");
		Matcher m = p.matcher(page);
		if (m.find()) {
			return m.group(1);
		}

		return null;
	}

	private static Matcher matchPage(String page) {
		Pattern p = Pattern.compile("\\s*<page>.*?" + "<title>(.*?)</title>.*?"
				+ "<id>(.*?)</id>.*?" + "<revision>\\s*" + "<id>(.*?)</id>.*?"
				+ "<timestamp>(.*?)</timestamp>.*?"
				+ "<text\\s*xml:space=\"preserve\">(.*?)</text>.*?"
				+ "<sha1>(.*?)</sha1>.*?" + "</page>", Pattern.DOTALL);
		return p.matcher(page);
	}

	private static boolean legalName(String name) {
		if (name.length() == 0)
			return false;

		char ch;
		for (int i = 0; i < name.length(); ++i) {
			ch = name.charAt(i);
			if (ch == '\t' || ch == '\n')
				return false;
		}

		return true;
	}

	// this method is not fully implemented
	// but it shall be fine
	private static boolean properTarget(String target) {
		String beg = target.substring(0, Integer.min(10, target.length()));
		beg = beg.toLowerCase();

		return !(beg.charAt(0) == ':' || beg.startsWith("file:")
				|| beg.startsWith("category:") || beg.startsWith("wikt:"));
	}
}
