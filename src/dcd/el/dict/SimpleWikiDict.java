package dcd.el.dict;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;

public class SimpleWikiDict {
	private class WidCandidates {
		public int[] wids = null;
	}
	
	public SimpleWikiDict(String aliasFileName, String widCandidatesFileName) {
		System.out.println("loading dict...");
		DataInputStream dis0 = IOUtils.getBufferedDataInputStream(aliasFileName);
		DataInputStream dis1 = IOUtils.getBufferedDataInputStream(widCandidatesFileName);
		try {
			int numAliases = dis0.readInt();
			aliases = new ByteArrayString[numAliases];
			indices = new int[numAliases];
			widCandidatesArr = new WidCandidates[numAliases];
			
			for (int i = 0; i < numAliases; ++i) {
				aliases[i] = new ByteArrayString();
				aliases[i].fromFileWithByteLen(dis0);
				indices[i] = dis0.readInt();
				
				int len = dis0.readInt();
				widCandidatesArr[i] = new WidCandidates();
				widCandidatesArr[i].wids = new int[len];
				for (int j = 0; j < len; ++j) {
					widCandidatesArr[i].wids[j] = dis1.readInt();
				}
			}
			
			dis0.close();
			dis1.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}
	
	public int[] getCandidates(String alias) {
		alias = alias.toLowerCase();
		ByteArrayString bas = new ByteArrayString(alias);
		int pos = Arrays.binarySearch(aliases, bas);
		if (pos < 0)
			return null;
		return widCandidatesArr[indices[pos]].wids;
	}
	
	ByteArrayString[] aliases = null;
	int[] indices = null;
	WidCandidates[] widCandidatesArr = null;
}
