// author: DHL brnpoem@gmail.com

package dcd.el.wiki;

import java.io.BufferedReader;
import java.io.IOException;

public class WikiReaderXML {
	public static String nextPage(BufferedReader reader) {
		String line = null;
		StringBuilder pageStr = new StringBuilder();
		try {
			while ((line = reader.readLine()) != null) {
				if (line.equals("  <page>")) {
					pageStr.append(line).append("\n");
				} else if (pageStr.length() > 0) {
					pageStr.append(line).append("\n");
				} else {
					System.out.println(line);
					System.out.println("xml file not formated as expected!");
				}
				
				if (line.equals("  </page>")) {
					return new String(pageStr);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
