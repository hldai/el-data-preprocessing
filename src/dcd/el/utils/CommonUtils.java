// author: DHL brnpoem@gmail.com

package dcd.el.utils;

import java.util.Comparator;

public class CommonUtils {
	public static class StringToIntComparator implements Comparator<String> {

		@Override
		public int compare(String sl, String sr) {
			int vl = Integer.valueOf(sl), vr = Integer.valueOf(sr);
			return vl - vr;
		}
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
