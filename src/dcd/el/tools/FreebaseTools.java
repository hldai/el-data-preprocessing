package dcd.el.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dcd.el.ELConsts;
import dcd.el.io.IOUtils;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TupleFileTools;

public class FreebaseTools {
	public static final String MID_PREFFIX = "<http://rdf.freebase.com/ns/m.";
	public static final String PERSON_TYPE_SUFFIX = ">\t<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>\t<http://rdf.freebase.com/ns/people.person>\t.";

	
	public static void filterPersonLastNameList(String lastNameListFileName,
			String dstFileName) {
		HashMap<String, Integer> nameCnt = new HashMap<String, Integer>();
		BufferedReader reader = IOUtils.getUTF8BufReader(lastNameListFileName);
		String line = null;
		try {
			int lineCnt = 0;
			while ((line = reader.readLine()) != null) {
				String lastName = CommonUtils.getFieldFromLine(line, 1).toLowerCase();
				Integer cnt = nameCnt.get(lastName);
				if (cnt == null)
					nameCnt.put(lastName, 1);
				else
					nameCnt.put(lastName, cnt + 1);
				
				if (lineCnt % 100000 == 0)
					System.out.println(lineCnt);
				++lineCnt;
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH, "last_name_cnts.txt").toString();
		BufferedWriter writer = IOUtils.getUTF8BufWriter(tmpFileName);
		try {
			for (Map.Entry<String, Integer> entry : nameCnt.entrySet()) {
				writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CommonUtils.StringToIntComparator cmp = new CommonUtils.StringToIntComparator();
		TupleFileTools.sort(tmpFileName, dstFileName, new TupleFileTools.SingleFieldComparator(1, cmp));
	}
	
	public static void genPersonLastNameList(String nameListFileName,
			String personListFileName, String dstFileName) {
		int numPersons = IOUtils.getNumLinesFor(personListFileName);
		String[] personMids = new String[numPersons];
		BufferedReader reader = IOUtils.getUTF8BufReader(personListFileName);
		try {
			for (int i = 0; i < numPersons; ++i) {
				personMids[i] = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		reader = IOUtils.getUTF8BufReader(nameListFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		String line = null;
		long cnt = 0, wcnt = 0;
		try {
			while ((line = reader.readLine()) != null) {
				String mid = CommonUtils.getFieldFromLine(line, 0);
				int pos = Arrays.binarySearch(personMids, mid);
				if (pos > -1) {
					String fullName = CommonUtils.getFieldFromLine(line, 1);
					String[] parts = fullName.split(" ");
					if (parts.length > 1) {
						writer.write(mid + "\t" + parts[parts.length - 1] + "\n");
						++wcnt;
					}
				}
				
				++cnt;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
//				if (wcnt == 10) break;
			}
			
			System.out.println(cnt + " names processed.");
			System.out.println(wcnt + " last names written.");
			reader.close();
			writer.close();
			
			IOUtils.writeNumLinesFileFor(dstFileName, wcnt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genPersonMidList(String dumpFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(dumpFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			String line = null;
			long cnt = 0;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(MID_PREFFIX)
						&& line.endsWith(PERSON_TYPE_SUFFIX)) {
					String mid = line.substring(MID_PREFFIX.length(),
							line.length() - PERSON_TYPE_SUFFIX.length());
					writer.write(mid + "\n");
					++cnt;

					if (cnt % 100000 == 0)
						System.out.println(cnt + " person found.");
					// if (cnt == 50) break;
				}
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void searchFile(String filePath, String subStr,
			String resultFileName) {
		try {
			BufferedReader bufReader = IOUtils.getGZIPBufReader(filePath);
			BufferedWriter writer = IOUtils.getUTF8BufWriter(resultFileName);

			BigInteger bigCnt = BigInteger.valueOf(0);
			int cnt = 0;
			String sline;
			while ((sline = bufReader.readLine()) != null) {
				if (sline.contains(subStr)) {
					System.out.println(sline);
					System.out.println("Found on line " + cnt);
					writer.write(sline + "\n");
					// break;
				}

				++cnt;

				if (cnt == 100000000) {
					bigCnt = bigCnt.add(BigInteger.valueOf(cnt));
					cnt = 0;
				}
			}
			bigCnt = bigCnt.add(BigInteger.valueOf(cnt));

			bufReader.close();
			writer.close();

			System.out.println(bigCnt + " lines searched.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
