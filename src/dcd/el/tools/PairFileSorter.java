// author: DHL brnpoem@gmail.com

package dcd.el.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import edu.zju.dcd.edl.ELConsts;
import edu.zju.dcd.edl.io.IOUtils;
import dcd.el.objects.Pair;
import dcd.el.utils.CommonUtils;

@Deprecated
public class PairFileSorter {
	public static class ValuePair {
		public String v1 = null;
		public String v2 = null;
	}
	
	public static class PairComparator implements Comparator<ValuePair> {
		public PairComparator(int cmpField) {
			this.cmpField = cmpField;
		}

		@Override
		public int compare(ValuePair vpl, ValuePair vpr) {
			return cmpField == 0 ? vpl.v1.compareTo(vpr.v1) 
					: vpl.v2.compareTo(vpr.v2);
		}
		
		private int cmpField = 0;
	}
	
	public static class IntValuePairComparator implements Comparator<Pair<String, Integer>> {

		@Override
		public int compare(Pair<String, Integer> pl,
				Pair<String, Integer> pr) {
			return pl.value - pr.value;
		}
	}
	
//	public static String TMP_FILE_PATH = "d:/projects/temp_data";

	public static int NUM_CHARS_LIM = 1024 * 1024 * 256;
	public static int NUM_VP_LIM = 12000000;
	
	public static void directSortIntValueFile(String fileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		int lineCnt = 0;
		String line = null;
		LinkedList<Pair<String, Integer>> plist = new LinkedList<Pair<String, Integer>>();
		Pair<String, Integer> vp = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				vp = new Pair<String, Integer>();
				vp.key = vals[0];
				vp.value = Integer.valueOf(vals[1]);
				plist.add(vp);
				
				++lineCnt;
			}
			
			reader.close();
			
			System.out.println(lineCnt + " lines read.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		IntValuePairComparator cmp = new IntValuePairComparator();
		Collections.sort(plist, cmp);
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		try {
			for (Pair<String, Integer> p : plist) {
				writer.write(p.key + "\t" + p.value + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		IOUtils.writeNumLinesFileFor(dstFileName, lineCnt);
	}

	public static void pairFileSort(String srcFileName, int sortFieldIdx,
			String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);

		int lineCnt = 0;
		int tmpFileCnt = 0;
		int charCnt = 0;
		String line = null;
		LinkedList<ValuePair> pfsList = new LinkedList<ValuePair>();
		PairComparator cmp = new PairComparator(sortFieldIdx);
		ValuePair vp = null;

		String tmpFileName = null;
		boolean flgExceeded = true, needTmpFileSort = false;
		try {
			while (flgExceeded) {
				flgExceeded = false;
				
				System.out.println("Reading " + srcFileName);
				int vpCnt = 0;
				while (!flgExceeded && (line = reader.readLine()) != null) {
					++lineCnt;
//					if (line.equals("Khasi¨C&")) {
//						System.out.println(lineCnt);
//					}
				
					vp = new ValuePair();
					String[] vals = line.split("\t");
					vp.v1 = vals[0];
					vp.v2 = vals[1];
//					vp.v1 = IOUtils.getFieldFromLine(line, 0);
//					vp.v2 = IOUtils.getFieldFromLine(line, 1);

					pfsList.add(vp);
					++vpCnt;
					if (vpCnt % 1000000 == 0)
						System.out.println(vpCnt + "\t" + charCnt);
					
					charCnt += line.length();
					flgExceeded = charCnt > NUM_CHARS_LIM || vpCnt > NUM_VP_LIM;
				}
				
				System.out.println("Sorting " + tmpFileCnt);
				Collections.sort(pfsList, cmp);
				System.out.println("Done.");
				
				if (flgExceeded || tmpFileCnt != 0) {
					needTmpFileSort = true;
					// write to temp file ...
					
					tmpFileName = ELConsts.TMP_FILE_PATH + "/s" + DEC_FORMAT.format(tmpFileCnt);
					writePairListToFile(pfsList, tmpFileName);
					
					++tmpFileCnt;
					charCnt = 0;
					pfsList.clear();
				}
			}
			
			reader.close();
		} catch (Exception e) {
			System.out.println(lineCnt);
			e.printStackTrace();
		}
		
		if (needTmpFileSort) {
			// sort temp files
			System.out.println("Sorting temp files...");
			sortTempFiles(tmpFileCnt, sortFieldIdx, dstFileName);
			System.out.println("Done.");
		} else {
			// direct sort
			System.out.println("Doing direct sort.");
			writePairListToFile(pfsList, dstFileName);
			System.out.println("Done.");
		}
	}
	
	public static boolean checkPairListFileOrder(String fileName, int fieldIdx, boolean fullyCheck) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		try {
			int cnt = 0;
			String line = null;
			String preField = null, curField = null, preSubField = null, curSubField = null;
			while ((line = reader.readLine()) != null) {
				curField = CommonUtils.getFieldFromLine(line, fieldIdx);
				curSubField = CommonUtils.getFieldFromLine(line, 1 - fieldIdx);
				
				if (preField != null && curField.compareTo(preField) < 0) {
					System.out.println("line " + cnt + ": Not properly ordered.");
					return false;
				}
				
				if (fullyCheck && preField != null && curField.equals(preField)) {
					if (preSubField.compareTo(curSubField) > 0) {
						System.out.println("Not properly ordered.");
						return false;
					}
				}
				
				preField = curField;
				preSubField = curSubField;
				++cnt;
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("File properly ordered.");
		return true;
	}
	
	private static void writePairListToFile(LinkedList<ValuePair> list, String dstFileName) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		
		try {
//			String prev1 = null;
			for (ValuePair vp : list) {
//				System.out.println(vp.v1 + "\t" + vp.v2);
//				if (prev1 == null || !vp.v1.equals(prev1))
				writer.write(vp.v1 + "\t" + vp.v2 + "\n");
//				prev1 = vp.v1;
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void sortTempFiles(int numTmpFiles, int sortFieldIdx, String dstFileName) {
		BufferedReader[] readers = new BufferedReader[numTmpFiles];
		
		String tmpFileName = null;
		for (int i = 0; i < numTmpFiles; ++i) {
			tmpFileName = ELConsts.TMP_FILE_PATH + "/s" + DEC_FORMAT.format(i);
			
			readers[i] = IOUtils.getUTF8BufReader(tmpFileName);
		}
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		
		try {
			String[] lines = new String[numTmpFiles];
			for (int i = 0; i < numTmpFiles; ++i) {
				lines[i] = readers[i].readLine();
			}
			
			int minPos = 0;
			while (minPos > -1) {
				minPos = getMinIdx(lines, sortFieldIdx);
				
				if (minPos > -1) {
					writer.write(lines[minPos] + "\n");
					
					lines[minPos] = readers[minPos].readLine();
				}
			}
			
			
			for (int i = 0; i < numTmpFiles; ++i) {
				readers[i].close();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int getMinIdx(String[] lines, int sortFieldIdx) {
		int pos = -1;
		String curField = null, preField = null;
		for (int i = 0; i < lines.length; ++i) {
			if (lines[i] != null) {
				if (pos == -1) {
					preField = CommonUtils.getFieldFromLine(lines[i], sortFieldIdx);
					pos = i;
				} else {
					curField = CommonUtils.getFieldFromLine(lines[i], sortFieldIdx);
					if (preField.compareTo(curField) > 0) {
						pos = i;
						preField = curField;
					}
				}
			}
		}
		
		return pos;
	}
	
	private static final DecimalFormat DEC_FORMAT = new DecimalFormat("0000");
}
