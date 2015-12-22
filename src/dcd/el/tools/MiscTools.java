// author: DHL brnpoem@gmail.com

package dcd.el.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;

import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.utils.WidMidMapper;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.StringTransformer;
import dcd.el.utils.TupleFileTools;

public class MiscTools {
	public static final String TOPIC_PREFIX = "<http://rdf.basekb.com/ns/m.";
	public static final String TOPIC_PREFIX_FULL = "<http://rdf.freebase.com/ns/m.";
	
	public static final String ENWIKI_URL_PREFIX = "http://en.wikipedia.org";
	public static final String ENWIKI_ID_URL_PREFIX = "http://en.wikipedia.org/wiki/index.html?curid=";
	public static final String ENWIKI_NON_ID_URL_PREFIX = "http://en.wikipedia.org/wiki/";
	
	public static final String WEBPAGE_PREDICATE_FULL = "<http://rdf.freebase.com/ns/common.topic.topic_equivalent_webpage>";
	public static final String NAME_PREDICATE_FULL = "<http://rdf.freebase.com/ns/type.object.name>";
	public static final String ALIAS_PREDICATE_FULL = "<http://rdf.freebase.com/ns/common.topic.alias>";
	
	public static final int NUM_MAPPED_MID = 4307602;
	
	public static final int NAME_LEN_LIMIT = 200;
	
	public static final char[] EN_CONTENT_SUFFIX = { '@', 'e', 'n' };
	
	public static final int MAX_NUM_WIKI_ID = 15500000;
	
	private static class WidToMidTransformer implements StringTransformer {
		public WidToMidTransformer(int idx, WidMidMapper widToMid) {
			this.idx = idx;
			this.widToMid = widToMid;
		}

		@Override
		public String transform(String str) {
			String[] vals = str.split("\t");
			int wid = Integer.valueOf(vals[idx]);
			String mid = widToMid.getMid(wid);
			if (mid == null)
				return null;
			vals[idx] = mid;
			StringBuilder stringBuilder = new StringBuilder();
			boolean isFirst = true;
			for (int i = 0; i < vals.length; ++i) {
				if (isFirst)
					isFirst = false;
				else
					stringBuilder.append("\t");
				stringBuilder.append(vals[i]);
			}
			
			return new String(stringBuilder);
		}
		
		int idx;
		WidMidMapper widToMid = null;
	}
	
	public static void widToMidInTupleFile(String fileName, 
			String widToMidFileName, String dstFileName) {
		WidMidMapper widToMid = new WidMidMapper(widToMidFileName);
		WidToMidTransformer transformer = new WidToMidTransformer(0, widToMid);
		TupleFileTools.transformLines(fileName, transformer, dstFileName);
	}
	
	public static void retrieveFromFreebasePath(String path, String subStr, 
			String fileNameFilter, String dstFileName) {
		LinkedList<String> results = RDFRetrieve.searchPath(path, subStr, fileNameFilter);

		writeSearchResults(results, dstFileName);
	}
	
	public static void searchFile(String fileName, String dstStr) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains(dstStr)) {
					System.out.println(line);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// find all lines with @subStr in @fileName file
	// write result to @dstFileName
	public static void retrieveFromRdf(String fileName, String subStr, 
			String dstFileName) {
		LinkedList<String> results = new LinkedList<String>();
		
		RDFRetrieve.searchFile(fileName, subStr, results);
		
		writeSearchResults(results, dstFileName);
	}
	
	public static boolean freebaseOrderCheck(String freebasePath, String nameFilter) {
		File dir = new File(freebasePath);
		
		for (String sf : dir.list()) {
			
			if (nameFilter != null && !sf.contains(nameFilter)) {
				continue;
			}
			
			System.out.println("Checking " + sf);
			
			if (!freebaseFileOrderCheck(freebasePath + '/' + sf)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean freebaseFileOrderCheck(String fileName) {
		String sline = null;
		String curFirstPart = null, preFirstPart = null;
		try {
			BufferedReader reader = IOUtils.getGZIPBufReader(fileName);
			
			while ((sline = reader.readLine()) != null) {
				curFirstPart = getFirstPart(sline);
				
				if (preFirstPart != null && curFirstPart.compareTo(preFirstPart) < 0) {
					System.out.println(preFirstPart + ' ' + curFirstPart);
					return false;
				}
				
				preFirstPart = curFirstPart;
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public static void genFreebaseMidList(String freebasePath, String nameFilter, String dstFileName) {
		File dir = new File(freebasePath);
		
		int fileCnt = 0;
		for (String sf : dir.list()) {
			if (sf.contains(nameFilter)) {
				++fileCnt;
			}
			//processMidInFile(freebasePath + "/" + sf);
		}
		
		BufferedReader[] readers = new BufferedReader[fileCnt];
		int idx = 0;
		for (String sf : dir.list()) {
			if (sf.contains(nameFilter)) {
				readers[idx++] = IOUtils.getGZIPBufReader(freebasePath + '/' + sf);
			}
		}
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		int cnt = 0;
		String[] slines = new String[fileCnt];
		try {
			for (int i = 0; i < readers.length; ++i) {
				while ((slines[i] = readers[i].readLine()) != null 
						&& !slines[i].startsWith(TOPIC_PREFIX)) ;
				
				slines[i] = slines[i].split("\t")[0];
				slines[i] = slines[i].substring(TOPIC_PREFIX.length(), slines[i].length() - 1);
				
//				writer.write(slines[i] + "\n");
			}
			
			int minIdx = -1;
			String preMid = null;
			while ((minIdx = getMinStringIdx(slines)) > -1) {
				if (preMid == null || !preMid.equals(slines[minIdx])) {
					writer.write(slines[minIdx] + "\n");
					preMid = slines[minIdx];
				}
				
				while ((slines[minIdx] = readers[minIdx].readLine()) != null 
						&& !slines[minIdx].startsWith(TOPIC_PREFIX)) ;
				
				if (slines[minIdx] != null) {
					slines[minIdx] = slines[minIdx].split("\t")[0];
					slines[minIdx] = slines[minIdx].substring(TOPIC_PREFIX.length(),
							slines[minIdx].length() - 1);
				}
				
				++cnt;
				if (cnt % 1000000 == 0) {
					System.out.println(cnt);
				}
			}
			
			writer.close();
			for (BufferedReader r : readers) {
				r.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genFreebaseFullDumpName(String freebaseFileName,
			String dstFileName) {
		genFreebaseFullDumpPartInfo(freebaseFileName, NAME_PREDICATE_FULL, dstFileName);
	}
	
	public static void genFreebaseFullDumpAlias(String freebaseFileName,
			String dstFileName) {
		genFreebaseFullDumpPartInfo(freebaseFileName, ALIAS_PREDICATE_FULL, dstFileName);
	}
	
	public static int maxNameLen(String midToNameFile) {
		BufferedReader reader = IOUtils.getUTF8BufReader(midToNameFile);
		
		int maxLen = 0;
//		String maxLenLine = null;
		int lenCnt = 0;
		try {
			String line = null, name = null;
			while ((line = reader.readLine()) != null) {
				name = CommonUtils.getFieldFromLine(line, 1);
				if (name.length() > maxLen) {
					maxLen = name.length();
//					maxLenLine = line;
				}
				
				if (name.length() > NAME_LEN_LIMIT) {
					++lenCnt;
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		System.out.println(maxLenLine);
		System.out.println(lenCnt + " names has length longer than " + NAME_LEN_LIMIT);
		
		return maxLen;
	}
	
	// get mid and name/alias pairs
	public static void genFullFreebaseEnName(String srcFileName, String dstFileName, 
			boolean toLowerCase) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			int rcnt = 0;
			int wcnt = 0;
			
			String line = null, mid = null, nameVal = null, nameValWashed = null;
			while ((line = reader.readLine()) != null) {
				mid = getMidFromLine(line, true);
				if (mid != null) {
					// TODO has \t in content?
					nameVal = CommonUtils.getFieldFromLine(line, 2);
					if (contentIsEnglish(nameVal)) {
//						nameValWashed = nameVal.substring(1, nameVal.length() - EN_CONTENT_SUFFIX.length - 1);
//						
//						if (nameValWashed.contains("\\")) {
//							writer.write(mid + '\t' + nameValWashed + '\n');
//							++wcnt;
//						}
						nameValWashed = washText(nameVal);
						
						
						// names with length longer than NAME_LEN_LIMIT are filtered
						if (nameValWashed != null && nameValWashed.length() <= NAME_LEN_LIMIT) {
							if (toLowerCase) {
								nameValWashed = nameValWashed.toLowerCase();
							}
							
							writer.write(mid + '\t' + nameValWashed + '\n');
							++wcnt;
						}
					}
				}
				
				++rcnt;
				
//				if (wcnt == 1000)
//					break;
			}
			
			reader.close();
			writer.close();
			
			System.out.println(rcnt + " lines processed. " 
					+ wcnt + " lines written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genFreebaseFullDumpPartInfo(String freebaseFileName,
			String predicate, String dstFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(freebaseFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			String line = null;
//			String mid = null;
//			String url = null;
			int ircnt = 0;
			int iwcnt = 0;
			BigInteger rcnt = BigInteger.valueOf(0);
			while ((line = reader.readLine()) != null) {
				if (predicate.equals(CommonUtils.getFieldFromLine(line, 1))) {
					writer.write(line + '\n');
					
					++iwcnt;
				}
				
				++ircnt;
				
				if (ircnt == 1e8) {
					rcnt = rcnt.add(BigInteger.valueOf(ircnt));
					ircnt = 0;
					
					System.out.println(rcnt + " lines processed. " + iwcnt + " lines written.");
				}
				
//				if (iwcnt == 10) {
//					break;
//				}
				
			}
			rcnt = rcnt.add(BigInteger.valueOf(ircnt));
			
			System.out.println(rcnt + " lines processed.");
			System.out.println(iwcnt + " lines written.");
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void genFreebaseFullDumpWebpageList(String freebaseFileName,
			String dstFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(freebaseFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			String line = null;
			String mid = null;
			String url = null;
			int ircnt = 0;
			int iwcnt = 0;
			BigInteger rcnt = BigInteger.valueOf(0);
			while ((line = reader.readLine()) != null) {
				if (WEBPAGE_PREDICATE_FULL.equals(CommonUtils.getFieldFromLine(line, 1))) {
					mid = getMidFromLine(line, true);
					if (mid == null) {
						System.out.println("Not good format.");
						System.out.println(line);
					} else {
						url = CommonUtils.getFieldFromLine(line, 2);
						writer.write(mid + "\t"
								+ url.substring(1, url.length() - 1) + "\n");
						++iwcnt;
					}
				}
				
				++ircnt;
				
				if (ircnt == 1e9) {
					rcnt = rcnt.add(BigInteger.valueOf(ircnt));
					ircnt = 0;
				}
				
//				if (ircnt == 10) {
//					break;
//				}
				
			}
			rcnt = rcnt.add(BigInteger.valueOf(ircnt));
			
			System.out.println(rcnt + " lines processed.");
			System.out.println(iwcnt + " lines written.");
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// TODO some wikipedia pages missing match
	// map freebase mid to wikipedia page url
	public static void mapFreebaseToWiki(String freebasePath, String dstFileName) {
		final String NAME_FILTER = "webpages";
		
		File dir = new File(freebasePath);
		
		int fileCnt = 0;
		for (String fileName : dir.list()) {
			if (fileName.contains(NAME_FILTER)) {
				++fileCnt;
			}
		}
		
		BufferedReader[] readers = new BufferedReader[fileCnt];
		
		int idx = 0;
		for (String fileName : dir.list()) {
			if (fileName.contains(NAME_FILTER)) {
				readers[idx++] = IOUtils.getGZIPBufReader(freebasePath 
						+ "/" + fileName);
			}
		}
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		String[] mids = new String[fileCnt];
		String[] urls = new String[fileCnt];
		String sline;
		try {
			// read first lines from files
			for (int i = 0; i < readers.length; ++i) {
				while ((sline = readers[i].readLine()) != null) {
					if (sline.startsWith(TOPIC_PREFIX)) {
						urls[i] = CommonUtils.getFieldFromLine(sline, 2);
						urls[i] = urls[i].substring(1, urls[i].length() - 1);
						
						if (urls[i].startsWith(ENWIKI_URL_PREFIX)) {
							mids[i] = getMidFromLine(sline, false);
							break;
						}
					} else {
						System.out.println("non-topic with webpage:");
						System.out.println(sline);
					}
				}
			}
			

			int cnt = 0;
			int minIdx = -1;
			String preMid = null;
			String preUrl = null;
			while ((minIdx = getMinStringIdx(mids)) > -1) {
				if (preMid == null || (!preMid.equals(mids[minIdx]) 
						|| !preUrl.equals(urls[minIdx]))) {
					writer.write(mids[minIdx] + "\t" + urls[minIdx] + "\n");
					++cnt;
				}
				
				preMid = mids[minIdx];
				preUrl = urls[minIdx];
				
				// read the minIdx file
				mids[minIdx] = null;
				while ((sline = readers[minIdx].readLine()) != null) {
					if (sline.startsWith(TOPIC_PREFIX)) {
						urls[minIdx] = CommonUtils.getFieldFromLine(sline, 2);
						urls[minIdx] = urls[minIdx].substring(1, urls[minIdx].length() - 1);
						
						if (urls[minIdx].startsWith(ENWIKI_URL_PREFIX)) {
							mids[minIdx] = getMidFromLine(sline, false);
							break;
						}
					} else {
						System.out.println("non-topic with webpage:");
						System.out.println(sline);
					}
				}
				
//				if (cnt == 100)
//					break;
				if (cnt % 1000000 == 0) {
					System.out.println(cnt);
				}
			}
			
			writer.close();
			for (BufferedReader r : readers) {
				r.close();
			}
			
			System.out.println(cnt + " values written");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void splitMappedWikiUrlList(String midToWikiMapFile, String dstFileNameId, String dstFileNameNonId) {
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWikiMapFile);
		BufferedWriter writerId = IOUtils.getUTF8BufWriter(dstFileNameId);
		BufferedWriter writerNonId = IOUtils.getUTF8BufWriter(dstFileNameNonId);
		
		try {
			int cnt = 0;
			String line = null, mid = null, url = null;
			while ((line = reader.readLine()) != null) {
				mid = CommonUtils.getFieldFromLine(line, 0);
				url = CommonUtils.getFieldFromLine(line, 1); // get url
				if (url.startsWith(ENWIKI_ID_URL_PREFIX)) {
					writerId.write(mid + '\t' + url.substring(ENWIKI_ID_URL_PREFIX.length()) + '\n');
					++cnt;
				} else {
					writerNonId.write(mid + '\t' + url.substring(ENWIKI_NON_ID_URL_PREFIX.length()) + '\n');
				}
			}
			
			reader.close();
			writerId.close();
			writerNonId.close();
			
			System.out.println(cnt + " lines written to id only file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//TODO
	public static void genMappedWikiIdList(String midToWikiMapFile, String dstFileName) {
		final int NUM_MAPPED = 4060467;
		String[] ids = new String[NUM_MAPPED];
		
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWikiMapFile);
		
		try {
			String sline;
			int idx = 0;
			while ((sline = reader.readLine()) != null) {
				ids[idx] = CommonUtils.getFieldFromLine(sline, 1);
//				ids[idx] = 
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// two type of urls: http://en.wikipedia.org/wiki/Corinth,_Texas and
	// http://en.wikipedia.org/wiki/index.html?curid=135765
	// we want http://en.wikipedia.org/wiki/index.html?curid=135765
	public static void washFreebaseWebpageListFile(String srcFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			String sline = null, url = null, mid = null;
			String preMid = null, preUrl = null;
			int cnt = 0, wcnt = 0, nonIdCnt = 0;
			while ((sline = reader.readLine()) != null) {
				mid = CommonUtils.getFieldFromLine(sline, 0);
				url = CommonUtils.getFieldFromLine(sline, 1);
				
				if (preMid != null && !mid.equals(preMid) && preUrl.startsWith(ENWIKI_URL_PREFIX)) {
					writer.write(preMid + "\t" + preUrl + "\n");
					++wcnt;
					if (!preUrl.startsWith(ENWIKI_ID_URL_PREFIX)) {
						++nonIdCnt;
					}
				}
				
				if (mid.equals(preMid) && url.startsWith(ENWIKI_ID_URL_PREFIX)) {
					preUrl = url;
				}
				if (!mid.equals(preMid)) {
					preUrl = url;
				}
				
				preMid = mid;
				
				
				++cnt;
				
				if (cnt % 1e6 == 0) {
					System.out.println(cnt + " lines processed.");
				}
				
//				if (cnt == 1000)
//					break;
			}
			
			if (preUrl.startsWith(ENWIKI_URL_PREFIX)) {
				writer.write(preMid + "\t" + preUrl + "\n");
				++wcnt;
				if (!preUrl.startsWith(ENWIKI_ID_URL_PREFIX)) {
					++nonIdCnt;
				}
			}
			
			reader.close();
			writer.close();
			
			System.out.println(cnt + " urls processed.");
			System.out.println(wcnt + " urls written.");
			System.out.println(nonIdCnt + " non-ID urls");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void checkMidListFile(String fileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		try {
			int cnt = 0;
			String sline = null;
			String preLine = null;
			while ((sline = reader.readLine()) != null) {
				if (preLine != null && sline.compareTo(preLine) < 0) {
					System.out.println("Error on Line " + String.valueOf(cnt));
					System.out.println(preLine + " " + sline);
					return ;
				}
				
				++cnt;
			}
			
			System.out.println(cnt + " lines checked.");
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void peekUTF8File(String fileName, int numLines) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		try {
			int cnt = 0;
			String sline = null;
			while (cnt < numLines && (sline = reader.readLine()) != null) {
				System.out.println(sline);
				
				++cnt;
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genMissMatchWikiList(String midToWikiFile, String compWikiIdListFile) {
		final int NUM_MATCHED_ID = 4307602;
		final int NUM_COMP_WIKI_ID = 15436971;
		
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWikiFile);
		
		int[] matchedIds = new int[NUM_MATCHED_ID];
		int[] compIds = new int[NUM_COMP_WIKI_ID];
		
		String line = null;
		int matchedCnt = 0;
		int compCnt = 0;
		try {
			int lenPrefix = ENWIKI_ID_URL_PREFIX.length();
			while ((line = reader.readLine()) != null) {
				matchedIds[matchedCnt++] = Integer.valueOf(CommonUtils.getFieldFromLine(line, 1).substring(lenPrefix));
			}
			
			reader.close();


			reader = IOUtils.getUTF8BufReader(compWikiIdListFile);
			while ((line = reader.readLine()) != null) {
				compIds[compCnt++] = Integer.valueOf(line);
				if (compCnt > 1 && compIds[compCnt - 1] < compIds[compCnt - 2]) {
					System.out.println("warning: ID not properly ordered");
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Arrays.sort(matchedIds);

		int cnt = 0;
		for (int v : compIds) {
			int idx = Arrays.binarySearch(matchedIds, v);
			if (idx < 0) {
				System.out.println(v + " not matched.");
				
				++cnt;
				
				if (cnt == 100)
					break;
			}
		}
	}
	
	// get the popularities of freebase topics from a wiki popularity file.
	// I find here that some freebase to wiki page mappings are bad.
	// But there aren't much of them.
	public static void genFreebasePopFromWiki(String midToWikiIdMapFile, String wikiIdPopFile,
			String dstFileName) {
		int[] ids = new int[MAX_NUM_WIKI_ID];
		int[] pops = new int[MAX_NUM_WIKI_ID];
		
		MidPopularityPair[] mpps = new MidPopularityPair[NUM_MAPPED_MID]; 
		
		loadWikiPopFile(wikiIdPopFile, ids, pops);
		
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWikiIdMapFile);
		String line = null, mid = null;
		int wikiId;
		
		try {
			int cnt = 0;
			int mppCnt = 0;
			while ((line = reader.readLine()) != null) {
				mid = CommonUtils.getFieldFromLine(line, 0);
				wikiId = Integer.valueOf(CommonUtils.getFieldFromLine(line, 1));
				
				int pos = Arrays.binarySearch(ids, wikiId);
				if (pos > -1) {
					mpps[mppCnt] = new MidPopularityPair();
					mpps[mppCnt].setMid(mid);
					mpps[mppCnt].setPopularity(pops[pos]);
					++mppCnt;
//					writer.write(mid + "\t" + pops[pos] + "\n");
				} else {
					System.out.println(wikiId + " has no popularity!");
				}
				
				++cnt;
				
//				if (cnt == 100) break;
			}
			
			reader.close();

			
			System.out.println("Sorting...");
			Arrays.sort(mpps, 0, mppCnt);
			System.out.println("Done.");
			
			BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
			for (int i = 0; i < mppCnt; ++i) {
				writer.write(mpps[i].getMid() + "\t" + mpps[i].getPopularity() + "\n");
			}
			
			writer.close();
			System.out.println(cnt + " lines processed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// TODO alias
	public static void processFreebaseEnNames(String fileName, String dstFileName) {
		final int MAX_NUM_NAMES = 44114102;
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		String[] names = new String[MAX_NUM_NAMES];
//		LinkedList<String> names = new LinkedList<String>();
		String line = null, name = null;
		int cnt = 0;
		try {
			while ((line = reader.readLine()) != null) {
//				System.out.println(line);
				name = CommonUtils.getFieldFromLine(line, 1);
				
				names[cnt] = name;
				
				++cnt;
				
//				if (cnt == 30) break;
				if (cnt % 1000000 == 0) {
					System.out.println(cnt);
				}
			}
			
//			for (String s : names) {
//				System.out.println(s);
//			}
			System.out.println("read finished.");
			
			reader.close();

			System.out.println("Sorting...");
			Arrays.sort(names, 0, cnt);
			System.out.println("Done.");
			
			BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
			
			for (String s : names) {
				writer.write(s + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// TODO obsolete
	public static void mergePairFiles(String[] fileNames, String dstFileName) {
		try {
			BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
			
			char[] buf = new char[1024];
			int rcnt = 0;
			for (String fileName : fileNames) {
				BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
				
				while ((rcnt = reader.read(buf)) > 0) {
					writer.write(buf, 0, rcnt);
				}
				
				reader.close();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void searchItem(String fileName, String str) {
		ItemReader reader = new ItemReader(fileName, false);
		
		Item item = null, preItem = null;
		while ((item = reader.readNextItem()) != null) {
//			System.out.println(item.key + " " + item.numLines);
			if (item.value.contains(str)) {
				System.out.println(preItem.value);
//				System.out.println(item.value);
				
//				break;
			}
			
			preItem = item;
		}
		
		reader.close();
	}
	
	private static void loadWikiPopFile(String fileName, int[] ids, int[] pops) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		String line = null, sid = null, spop = null;
		
		try {
			int cnt = 0;
			while ((line = reader.readLine()) != null) {
				sid = CommonUtils.getFieldFromLine(line, 0);
				spop = CommonUtils.getFieldFromLine(line, 1);
				
				ids[cnt] = Integer.valueOf(sid);
				pops[cnt] = Integer.valueOf(spop);
				
				if (cnt > 0 && ids[cnt] < ids[cnt - 1]) {
					System.out.println("ID not properly ordered! On line " + cnt + ". " 
							+ ids[cnt] + " " + pops[cnt]);
				}
				
				++cnt;
//				if (cnt == 10) {
//					break;
//				}
			}
			
			reader.close();
			
			System.out.println(cnt + " lines read from " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private static void loads

	private static int getMinStringIdx(String[] slines) {
		int minIdx = 0;
		while (minIdx < slines.length && slines[minIdx] == null) ++minIdx;
		
		if (minIdx == slines.length)
			return -1;
		
		for (int i = 1; i < slines.length; ++i) {
			if (slines[i] != null 
					&& slines[i].compareTo(slines[minIdx]) < 0) {
				minIdx = i;
			}
		}
		
		return minIdx;
	}
	
	private static String getFirstPart(String str) {
		int pos = 0;
		while (pos < str.length() && str.charAt(pos) != '\t') ++pos;
		
		return str.substring(0, pos - 1);
	}
	
	private static String getMidFromLine(String sline, boolean isFullDump) {
		String prefix = isFullDump ? TOPIC_PREFIX_FULL : TOPIC_PREFIX;
		
		if (!sline.startsWith(prefix))
			return null;
		
		int lenPrefix = prefix.length();
		int endpos = lenPrefix;
		while (endpos < sline.length() && sline.charAt(endpos) != '>') {
			++endpos;
		}
		
		return sline.substring(lenPrefix, endpos);
	}
	
	private static void writeSearchResults(LinkedList<String> results, String dstFileName) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			for (String s : results) {
				writer.write(s + '\n');
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean contentIsEnglish(String content) {
		int len = content.length();
		for (int i = 0; i < EN_CONTENT_SUFFIX.length; ++i) {
			if (content.charAt(len - 3 + i) != EN_CONTENT_SUFFIX[i]) {
				return false;
			}
		}
		return true;
	}
	
	// "Theme from \"A Summer Place\""@en  -> Theme from "A Summer Place"
	// TODO further process?
	private static String washText(String text) {
		String tmp = null;
		tmp = text.substring(1, text.length() - EN_CONTENT_SUFFIX.length - 1);
		
//		result = result.replaceAll("\\\\\"", "\"");
//		result = result.replaceAll("\\\\t", " ");
		
		 // could be different from freebase on web
		tmp = handleEscape(tmp);
		if (tmp == null) return null;
		tmp = handleEscape(tmp);
		if (tmp == null) return null;
		tmp = handleEscape(tmp);
		if (tmp == null) return null;
		
		int pos = 0;
		char curChar;
		
		// deal with repeating spaces
		StringBuilder textBuilder = new StringBuilder();
		textBuilder.append(tmp.charAt(0));
		pos = 1;
		while (pos < tmp.length()) {
			curChar =  text.charAt(pos);
			if (!(curChar == ' ' && tmp.charAt(pos - 1) == ' ')) {
				textBuilder.append(curChar);
			}
			++pos;
		}
		
		return tmp.trim();
	}
	
	private static String handleEscape(String text) {
		String tmp = text;
		
		StringBuilder textBuilder = new StringBuilder();
		int pos = 0;
		char curChar, nextChar;
		while (pos < tmp.length() - 1) {
			curChar = tmp.charAt(pos);
			if (curChar == '\\') {
				nextChar = tmp.charAt(pos + 1);
				switch (nextChar) {
				case '"':
				case '\\':
				case '\'':
					textBuilder.append(nextChar);
					pos += 2;
					break;
				case 't':
				case 'n':
				case 'r':
					textBuilder.append(' ');
					pos += 2;
					break;
				default:
					//System.out.println("not good!. " + nextChar + ' ' + text);
					textBuilder.append('\\');
					++pos;
				}
			} else {
				textBuilder.append(curChar);
				++pos;
			}
		}
		
		if (pos < tmp.length()) {
			textBuilder.append(tmp.charAt(pos));
		}
		
		tmp = textBuilder.toString().trim();
		
		if (tmp.length() < 1) {
			return null;
		}
		
		return tmp;
	}
}
