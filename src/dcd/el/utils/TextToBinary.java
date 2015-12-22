// author: DHL brnpoem@gmail.com

package dcd.el.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;

public class TextToBinary {
	public static class BinaryStringPair implements
			Comparable<BinaryStringPair> {
		@Override
		public int compareTo(BinaryStringPair vr) {
			return this.str.compareTo(vr.str);
		}

		public ByteArrayString str = null;
	}

	public static class BinaryStringString extends BinaryStringPair {
		public ByteArrayString value = null;
	}

	public static class BinaryStringDouble implements
			Comparable<BinaryStringDouble> {
		@Override
		public int compareTo(BinaryStringDouble vr) {
			return this.str.compareTo(vr.str);
		}

		public ByteArrayString str;
		public double value;
	}

	public static void stringStringToBinary(String srcFileName, int len1,
			int len2, String dstFileName) {
		int numLines = IOUtils.getNumLinesFor(srcFileName);
		BinaryStringString[] pairList = new BinaryStringString[numLines];

		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		try {
			String line = null;
			for (int i = 0; i < numLines; ++i) {
				line = reader.readLine();
				String[] vals = line.split("\t");
				BinaryStringString bss = new BinaryStringString();
				bss.str = new ByteArrayString(vals[0], len1);
				bss.value = new ByteArrayString(vals[1], len2);
				pairList[i] = bss;
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Arrays.sort(pairList);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(numLines);
			for (BinaryStringString bss : pairList) {
				bss.str.toFileWithFixedLen(dos, len1);
				bss.value.toFileWithFixedLen(dos, len2);
			}
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// convert a string double text file to binary file
	// e.g. enwiki_idf.txt to enwiki_idf.sd
	public static void stringDoubleToBinary(String srcFileName,
			String dstFileName) {
		int numLines = IOUtils.getNumLinesFor(srcFileName);
		BinaryStringDouble[] bsds = new BinaryStringDouble[numLines];
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		try {
			String line = null;
			for (int i = 0; i < numLines; ++i) {
				line = reader.readLine();

				String[] vals = line.split("\t");
				BinaryStringDouble bsd = new BinaryStringDouble();
				bsd.str = new ByteArrayString(vals[0]);
				bsd.value = Double.valueOf(vals[1]);
				bsds[i] = bsd;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Arrays.sort(bsds);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(bsds.length);
			for (BinaryStringDouble bsd : bsds) {
				dos.write(bsd.str.bytes.length);
				dos.write(bsd.str.bytes);
				dos.writeDouble(bsd.value);
			}

			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
