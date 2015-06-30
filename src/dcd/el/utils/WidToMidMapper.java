package dcd.el.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import dcd.el.io.IOUtils;

public class WidToMidMapper {
	public WidToMidMapper(String widToMidFileName) {
		int numWids = IOUtils.getNumLinesFor(widToMidFileName);
		wids = new int[numWids];
		mids = new String[numWids];
		BufferedReader reader = IOUtils.getUTF8BufReader(widToMidFileName);
		try {
			for (int i = 0; i < numWids; ++i) {
				String line = reader.readLine();
				wids[i] = Integer.valueOf(CommonUtils.getFieldFromLine(line, 1));
				mids[i] = CommonUtils.getFieldFromLine(line, 0);
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getMid(int wid) {
		int pos = Arrays.binarySearch(wids, wid);
		if (pos < 0)
			return null;
		return mids[pos];
	}
	
	int[] wids;
	String[] mids;
}
