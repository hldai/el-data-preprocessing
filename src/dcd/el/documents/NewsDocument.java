// author: DHL brnpoem@gmail.com

package dcd.el.documents;

import java.io.BufferedReader;
import java.io.IOException;

import edu.zju.dcd.edl.io.IOUtils;

public class NewsDocument {
	public void load(String fileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		StringBuilder sb = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		text = new String(sb);
	}
	
	public String getText() {
		return text;
	}
	
	private String text = null;
}
