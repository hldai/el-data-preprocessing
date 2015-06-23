// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class MapEntryComparator implements
		Comparator<Map.Entry<String, Integer>> {

	@Override
	public int compare(Entry<String, Integer> el, Entry<String, Integer> er) {
		return el.getKey().compareTo(er.getKey());
	}

}
