// author: DHL brnpoem@gmail.com

package dcd.el.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.lang3.StringEscapeUtils;

public class CommonUtils {
	public static class StringToIntComparator implements Comparator<String> {

		@Override
		public int compare(String sl, String sr) {
			int vl = Integer.valueOf(sl), vr = Integer.valueOf(sr);
			return vl - vr;
		}
	}
	
	public static int[] genNonRepeatingRandom(int max, int num) {
		if (num > max)
			return null;
		
		int[] vals = new int[num];
		
		Random rnd = new Random();
		HashSet<Integer> generated = new HashSet<Integer>();
		int cnt = 0;
		while (cnt < num) {
			int val = rnd.nextInt(max);
			if (generated.add(val)) {
				vals[cnt] = val;
				++cnt;
			}
		}
		
		return vals;
	}
	
	public static long getLittleEndianLong(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return buf.asLongBuffer().get();
	}
	
	public static int getLittleEndianInt(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return buf.asIntBuffer().get();
	}
	
	public static float[] getLittleEndianFloatArray(byte[] bytes) {
		float[] vals = new float[bytes.length / Float.BYTES];
		
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.asFloatBuffer().get(vals);
		
		return vals;
	}
	
	public static String unescapeHtml(String str) {
		str = str.replaceAll("&nbsp;", " ");
		return StringEscapeUtils.unescapeHtml4(str);
	}
	
	public static String handleWiki(String str) {
		String s = null;
		return s;
	}
	
	// a line: 
	// <subject>	<predicate>	<object>	.
	// does not necessarily work for all kind of files 
	public static String getFieldFromLine(String line, int fieldIdx) {
		int begPos = 0, endPos = 0;
		
		for (int i = 0; i < fieldIdx; ++i) {
			begPos = nextTabPos(line, begPos);

			if (begPos < 0)
				return null;
			
			++begPos;
		}
		
		endPos = nextTabPos(line, begPos);
		return line.substring(begPos, endPos);
	}
	
	public static int countLines(String str) {
		if (str == null) return 0;
		int cnt = 0;
		int len = str.length();
		for (int pos = 0; pos < len; ++pos) {
			char ch = str.charAt(pos);
			if (ch == '\n') {
				++cnt;
			}
		}
		
		return cnt;
	}

//	public static void stringToByteArr(String s, byte[] bytes) {
//		Arrays.fill(bytes, (byte) 0);
//
//		byte[] tmp = s.getBytes();
//		for (int i = 0; i < tmp.length; ++i) {
//			bytes[i] = tmp[i];
//		}
//	}
	
	private static int nextTabPos(String sline, int begPos) {
		while (begPos < sline.length() && (sline.charAt(begPos) != '\t')) {
			++begPos;
		}
		
		return begPos;
	}
}
