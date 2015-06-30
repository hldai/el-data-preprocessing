// author: DHL brnpoem@gmail.com

package dcd.el.dict;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import dcd.el.ELConsts;
import dcd.el.io.ByteLineReader;
import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TupleFileTools;

public class DictGen {
	private static class MidToAliasCntMapper {
		public MidToAliasCntMapper(String midToAliasCntFileName) {
			int numLines = IOUtils.getNumLinesFor(midToAliasCntFileName);
			mids = new String[numLines];
			cnts = new int[numLines];
			BufferedReader reader = IOUtils.getUTF8BufReader(midToAliasCntFileName);
			try {
				for (int i = 0; i < numLines; ++i) {
					String line = reader.readLine();
					mids[i] = CommonUtils.getFieldFromLine(line, 0);
					cnts[i] = Integer.valueOf(CommonUtils.getFieldFromLine(line, 1));
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public int getAliasCnt(String mid) {
			int pos = Arrays.binarySearch(mids, mid);
			if (pos < 0) {
				System.err.println("mid not found");
				return 0;
			}
			return cnts[pos];
		}
		
		private String[] mids;
		private int[] cnts;
	}

	// TODO uncomment
	public static void mergeMidAliasCntFiles(String wikiMidAliasCntFileName,
			String freebaseMidAliasFileName, String dstMidEachAliasCntFileName,
			String dstMidAliasCntFileName) {
		String tmpFileName0 = Paths.get(ELConsts.TMP_FILE_PATH, "merged_fb_wiki_mid_alias_cnt.txt").toString();
		BufferedWriter writer = IOUtils.getUTF8BufWriter(tmpFileName0, false);
		BufferedReader reader = IOUtils.getUTF8BufReader(freebaseMidAliasFileName);
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\t1\n");
			}
			reader.close();
			
			reader = IOUtils.getUTF8BufReader(wikiMidAliasCntFileName);
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\n");
			}
			reader.close();
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tmpFileName1 = Paths.get(ELConsts.TMP_FILE_PATH, "merged_fb_wiki_mid_alias_cnt_ord_mid.txt").toString();
		genMergedMidAliasCntFiles(tmpFileName0, tmpFileName1, dstMidAliasCntFileName);
		TupleFileTools.SingleFieldComparator comparator = new TupleFileTools.SingleFieldComparator(1);
		TupleFileTools.sort(tmpFileName1, dstMidEachAliasCntFileName, comparator);
	}
	
	private static void genMergedMidAliasCntFiles(String fileName, String dstMidEachAliasCntFileName,
			String dstMidAliasCntFileName) {
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH, "sorted_mid_alias_cnt.txt").toString();
		int[] fieldIdxes = { 0, 1 };
		TupleFileTools.MultiStringFieldComparator comparator = new TupleFileTools.MultiStringFieldComparator(fieldIdxes);
		TupleFileTools.sort(fileName, tmpFileName, comparator);
		BufferedReader reader = IOUtils.getUTF8BufReader(tmpFileName);
		BufferedWriter writer0 = IOUtils.getUTF8BufWriter(dstMidEachAliasCntFileName, false);
		BufferedWriter writer1 = IOUtils.getUTF8BufWriter(dstMidAliasCntFileName, false);
		try {
			String line = null;
			String curAlias = null, curMid = null;
			int curMidEachAliasCnt = 0, curMidAliasCnt = 0;
			int midCnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				String mid = vals[0];
				int midAliasCnt = Integer.valueOf(vals[2]);
				if (curMid != null && mid.equals(curMid)) {
					curMidAliasCnt += midAliasCnt;
					if (curAlias.equals(vals[1])) {
						curMidEachAliasCnt += midAliasCnt;
					} else {
						writer0.write(curMid + "\t" + curAlias + "\t" + curMidEachAliasCnt + "\n");
						curAlias = vals[1];
						curMidEachAliasCnt = midAliasCnt;
					}
				} else {
					++midCnt;
					if (curMid != null) {
						writer0.write(curMid + "\t" + curAlias + "\t" + curMidEachAliasCnt + "\n");
						writer1.write(curMid + "\t" + curMidAliasCnt + "\n");
					}
					
					curMid = mid;
					curAlias = vals[1];
					curMidEachAliasCnt = midAliasCnt;
					curMidAliasCnt = midAliasCnt;
				}
			}
			writer0.write(curMid + "\t" + curAlias + "\t" + curMidEachAliasCnt + "\n");
			writer1.write(curMid + "\t" + curMidAliasCnt + "\n");
			
			reader.close();
			writer0.close();
			writer1.close();
			
			IOUtils.writeNumLinesFileFor(dstMidAliasCntFileName, midCnt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class MidPseEntry implements Comparable<MidPseEntry> {
		@Override
		public int compareTo(MidPseEntry entry) {
			return this.mid.compareTo(entry.mid);
		}
		
		public ByteArrayString mid = null;
		public float pse = 0;
	}
	
	// with probability of s given e
	public static void genDictWithPse(String midEachAliasCntFileName, String midAliasCntFileName,
			String dstAliasFile,
			String dstMidFile, int numIndices, String dstAliasIndexFile) {
		System.out.println("Loading mid to alias cnt file...");
		MidToAliasCntMapper midToAliasCntMapper = new MidToAliasCntMapper(midAliasCntFileName);
		System.out.println("done.");
		
		BufferedReader reader = IOUtils.getUTF8BufReader(midEachAliasCntFileName);
		BufferedWriter nameWriter = IOUtils.getUTF8BufWriter(dstAliasFile);
		if (nameWriter == null) return ;

		int cnt = 0, aliasCnt = 0;
		try {
			System.out.println("Writing alias and mid list...");
			
			DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstMidFile);

			String line = null;
			String mid = null, name = null, preName = null;
			int begPos = 0;
			LinkedList<MidPseEntry> aliasEntries = new LinkedList<MidPseEntry>();

			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					System.out.println("warning: empty line!");
				}

				name = CommonUtils.getFieldFromLine(line, 1);

				if (name.contains("\t")) {
					System.out.println("Name has \\t in it!");
					continue;
				}
				if (name.length() > ELConsts.MAX_ALIAS_LEN) {
					continue;
				}

				if (preName != null && !name.equals(preName)) {
					nameWriter.write(aliasEntries.size() + "\n");
					writeMidPseEntries(dos, aliasEntries);
					begPos += aliasEntries.size();
					aliasEntries.clear();
				}
				if (preName == null || !name.equals(preName)) {
					nameWriter.write(name + "\t" + begPos + "\t");
					++aliasCnt;
//					len = 0;
				}

				MidPseEntry midPseEntry = new MidPseEntry();
				mid = CommonUtils.getFieldFromLine(line, 0);
				midPseEntry.mid = new ByteArrayString(mid, ELConsts.MID_BYTE_LEN);
				
				int eachAliasCnt = Integer.valueOf(CommonUtils.getFieldFromLine(line, 2));
				int ne = midToAliasCntMapper.getAliasCnt(mid);
				midPseEntry.pse = (float)eachAliasCnt / ne;
				aliasEntries.add(midPseEntry);

				preName = name;
				++cnt;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
				// if (cnt == 100) break;
			}

			nameWriter.write(aliasEntries.size() + "\n");
			writeMidPseEntries(dos, aliasEntries);

			System.out.println(cnt + " lines read.");
			System.out.println(aliasCnt + " aliases written.");

			reader.close();
			nameWriter.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int indexCnt = genAliasIndices(dstAliasFile, aliasCnt, numIndices, dstAliasIndexFile);
		IOUtils.writeNumLinesFileFor(dstAliasIndexFile, indexCnt);
	}
	
	private static void writeMidPseEntries(DataOutputStream dos, LinkedList<MidPseEntry> aliasEntries) {
		Collections.sort(aliasEntries);
		try {
			for (MidPseEntry entry : aliasEntries) {
				entry.mid.toFileWithFixedLen(dos, ELConsts.MID_BYTE_LEN);
				dos.writeFloat(entry.pse);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// midNameFile:
	// [mid without m.]\t[alias]
	public static void genDict(String midNameFile, String dstAliasFile,
			String dstMidFile, int numIndices, String dstAliasIndexFile) {
		BufferedReader reader = IOUtils.getUTF8BufReader(midNameFile);
		BufferedWriter nameWriter = IOUtils.getUTF8BufWriter(dstAliasFile);
		
		if (nameWriter == null) return ;

		int cnt = 0, aliasCnt = 0;

		try {
			System.out.println("Writing alias and mid list...");
			
			DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstMidFile);

			String line = null;
			String mid = null, name = null, preName = null;
			int begPos = 0;
			LinkedList<ByteArrayString> candidateMids = new LinkedList<ByteArrayString>();

			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					System.out.println("warning: empty line!");
				}

				mid = CommonUtils.getFieldFromLine(line, 0);
				name = CommonUtils.getFieldFromLine(line, 1);

				if (name.contains("\t")) {
					System.out.println("Name has \\t in it!");
					continue;
				}
				if (name.length() > ELConsts.MAX_ALIAS_LEN) {
					continue;
				}

				if (preName != null && !name.equals(preName)) {
					nameWriter.write(candidateMids.size() + "\n");
					writeMids(dos, candidateMids);
					begPos += candidateMids.size();
					candidateMids.clear();
				}
				if (preName == null || !name.equals(preName)) {
					nameWriter.write(name + "\t" + begPos + "\t");
					++aliasCnt;
//					len = 0;
				}

				ByteArrayString midByte = new ByteArrayString(mid, ELConsts.MID_BYTE_LEN);
				candidateMids.add(midByte);

				preName = name;
				++cnt;

				// if (cnt == 100) break;
			}

			nameWriter.write(candidateMids.size() + "\n");
			writeMids(dos, candidateMids);

			System.out.println(cnt + " lines read.");
			System.out.println(aliasCnt + " aliases written.");

			reader.close();
			nameWriter.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int indexCnt = genAliasIndices(dstAliasFile, aliasCnt, numIndices, dstAliasIndexFile);
		IOUtils.writeNumLinesFileFor(dstAliasIndexFile, indexCnt);
	}
	
	private static void writeMids(DataOutputStream dos, LinkedList<ByteArrayString> mids) {
		Collections.sort(mids);
		for (ByteArrayString mid : mids) {
			mid.toFileWithFixedLen(dos, ELConsts.MID_BYTE_LEN);
		}
	}

	public static int genAliasIndices(String aliasFileName, int numAlias,
			int numIndices, String dstFileName) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		int aliasCnt = 0, indexCnt = 0, nextHitAliasCnt = 0;
		String line = null;
		ByteLineReader reader = new ByteLineReader();
		reader.open(aliasFileName);

		try {
			while ((line = reader.nextLine()) != null) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				
				if (aliasCnt == nextHitAliasCnt) {
					String[] vals = line.split("\t");
					writer.write(vals[0] + "\t" + reader.getCurLinePos() + "\n");
					++indexCnt;
					nextHitAliasCnt = (int)(((double)numAlias / numIndices) * indexCnt);
				}

				++aliasCnt;
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		reader.close();
		
		return indexCnt;
	}

	public static int getMaxMidLen(String midNameFile) {
		BufferedReader reader = IOUtils.getUTF8BufReader(midNameFile);

		int maxLen = -1;

		try {
			String line = null;
			String mid = null;

			while ((line = reader.readLine()) != null) {
				mid = CommonUtils.getFieldFromLine(line, 0);
				if (mid.length() > maxLen) {
					maxLen = mid.getBytes().length;
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Max mid length: " + maxLen);
		return maxLen;
	}

	// remove duplicate rows in ordered file fileName.
	@Deprecated
	public static void removeDuplicates(String srcFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			String line = null, preLine = null;
			while ((line = reader.readLine()) != null) {
				if (preLine == null || !line.equals(preLine)) {
					writer.write(line + "\n");
				}

				preLine = line;
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
