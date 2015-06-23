// author: DHL brnpoem@gmail.com

package dcd.el.dict;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

import dcd.el.ELConsts;
import dcd.el.io.IOUtils;

// load the whole alias dictionary to memory
public class AliasDictMem implements AliasDict {
	
	public AliasDictMem(String aliasFileName, int numAliases, String midFileName) {
		System.out.println("Loading file " + aliasFileName + " ...");
		loadAliasFile(aliasFileName, numAliases);
		System.out.println("Done.");
		
		try {
			raFile = new RandomAccessFile(midFileName, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public LinkedList<String> getMids(String alias) {
		int idx = Arrays.binarySearch(aliases, alias);
		
		if (idx < 0)
			return null;
		

//		System.out.println(aliases[idx]);
//		System.out.println(begPoses[idx]);
//		System.out.println(lens[idx]);
		
		int begPos = begPoses[idx], len = lens[idx];
		LinkedList<String> mids = new LinkedList<String>();
		byte[] bytes = new byte[ELConsts.MID_BYTE_LEN];
		try {
			raFile.seek(begPos);
			for (int i = 0; i < len; ++i) {
				raFile.read(bytes);
//				System.out.println(new String(bytes));
				mids.add(new String(bytes));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mids;
	}
	
	public void close() {
		try {
			raFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadAliasFile(String fileName, int numAliases) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
//		aliases = new String[numAliases];
		begPoses = new int[numAliases];
		lens = new int[numAliases];
		
		try {
			String line = null;
			int aliasCnt = 0, lineCnt = 0;
			while (aliasCnt < numAliases && (line = reader.readLine()) != null) {
				switch (lineCnt % 3) {
				case 0:
//					aliases[aliasCnt] = line;
					break;
				case 1:
					begPoses[aliasCnt] = Integer.valueOf(line);
					break;
				case 2:
					lens[aliasCnt] = Integer.valueOf(line);
					++aliasCnt;
					break;
				}
				
				if (lineCnt % 3 == 0 && aliasCnt % 1000000 == 0) {
					System.out.println(aliasCnt);
				}
				
				++lineCnt;
			}
			
			if (aliasCnt < numAliases) {
				System.out.println("warning: number of aliases fewer than designated!");
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	RandomAccessFile raFile = null;

	private String[] aliases = null;
	private int[] begPoses = null;
	private int[] lens = null;
}
