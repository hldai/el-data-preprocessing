// author: DHL brnpoem@gmail.com

package dcd.el.io;

import java.io.BufferedWriter;
import java.io.IOException;

import edu.zju.dcd.edl.io.IOUtils;

public class ItemWriter {
	public ItemWriter(String fileName, boolean checkExistence) {
		writer = IOUtils.getUTF8BufWriter(fileName, checkExistence);
	}
	
	public void open(String fileName) {
		writer = IOUtils.getUTF8BufWriter(fileName);
	}
	
	public void writeItem(Item item) {
		try {
			item.fixNumLines();
			writer.write(item.key + " " + item.numLines + "\n");
			writer.write(item.value);
			if (item.numLines > 0)
				writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	BufferedWriter writer = null;
}
