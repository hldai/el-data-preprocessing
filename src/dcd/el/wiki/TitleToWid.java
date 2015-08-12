package dcd.el.wiki;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;

public class TitleToWid {
	public TitleToWid(String titleToWidFileName) {
		System.out.println("Loading " + titleToWidFileName);
		DataInputStream dis = IOUtils.getBufferedDataInputStream(titleToWidFileName);
		try {
			int cnt = dis.readInt();
			System.out.println(cnt);
			titles = new ByteArrayString[cnt];
			wids = new int[cnt];
			for (int i = 0; i < cnt; ++i) {
				titles[i] = new ByteArrayString();
				titles[i].fromFileWithShortByteLen(dis);
				wids[i] = dis.readInt();
			}
			
			dis.close();
			System.out.println("done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getWid(String title) {
		ByteArrayString byteTitle = new ByteArrayString(title);
		return getWid(byteTitle);
	}
	
	public int getWid(ByteArrayString title) {
		int pos = Arrays.binarySearch(titles, title);
		if (pos < 0)
			return -1;
		return wids[pos];
	}
	
	public ByteArrayString[] titles = null;
	public int[] wids = null;
}
