// author: DHL brnpoem@gmail.com

package dcd.el.dict;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import dcd.el.ELConsts;
import dcd.el.io.ByteLineReader;
import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;
import dcd.el.utils.CommonUtils;

public class DictGen {

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
