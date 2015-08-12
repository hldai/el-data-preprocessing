// author: DHL brnpoem@gmail.com

package dcd.el.wiki;

import dcd.el.io.Item;
import dcd.el.io.ItemReader;

public class WikiReaderStripped {
	public static String TITLE_KEY = "TITLE";
	public static String TEXT_KEY = "TEXT";
	
	public void open(String fileName, boolean isGZip, boolean hasID) {
		this.hasID = hasID;
		reader = new ItemReader(fileName, isGZip);
	}
	
	public void close() {
		reader.close();
	}
	
	public boolean nextPage() {
		if (hasID) {
			idItem = reader.readNextItem();
			
			if (idItem == null)
				return false;
		}
		
		titleItem = reader.readNextItem();
		if (titleItem == null)
			return false;
		
		textItem = reader.readNextItem();
		if (textItem == null)
			return false;
		
		return true;
	}
	
	public Item getIdItem() {
		return idItem;
	}
	
	public Item getTitleItem() {
		return titleItem;
	}
	
	public Item getTextItem() {
		return textItem;
	}
	
	public String getId() {
		return idItem.value;
	}
	
	public String getTitle() {
		return titleItem.value;
	}
	
	public String getText() {
		return textItem.value;
	}
	
	private boolean hasID = false;
	private Item idItem = null;
	private Item titleItem = null;
	private Item textItem = null;
	
	private ItemReader reader = null;
}
