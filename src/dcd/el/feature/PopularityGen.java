// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.io.DataOutputStream;
import java.io.IOException;

import dcd.el.io.IOUtils;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;

public class PopularityGen {
	public static void genPopularityFile(String wikiArticleWordCntFileName, String maxWordCountFileName,
			String dstFileName) {
		ItemReader reader = new ItemReader();
		reader.open(wikiArticleWordCntFileName, false);
		
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		
		int maxWordCount = IOUtils.getIntValueFromFile(maxWordCountFileName);
		Item idItem = null,
				wcItem = null;
		int cnt = 0;
		try {
			while ((idItem = reader.readNextItem()) != null) {
				++cnt;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
				
				reader.readNextItem();
				wcItem = reader.readNextItem();
				
				if (wcItem.numLines == 0) {
					continue;
				}
				
				int len = 0;
				String[] lines = wcItem.value.split("\n");
				for (String line : lines) {
					String[] vals = line.split("\t");
					len += Integer.valueOf(vals[1]);
				}
				dos.writeInt(Integer.valueOf(idItem.value)); // wid
				float popularity = (float)len / maxWordCount;
				dos.writeFloat(popularity);
//				dos.writeInt(len);
				
//				if (cnt == 10) break;
			}
			
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		reader.close();
		
//		System.out.println("max len: " + maxLen);
	}
}
