// author: DHL brnpoem@gmail.com

package dcd.el.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;

import edu.zju.dcd.edl.io.IOUtils;

public class RDFRetrieve {
	
	// search for lines in files that contains @subStr
	// @fileNameFilterStr filters out files out to be searched
	public static LinkedList<String> searchPath(String path, String subStr, 
			String fileNameFilterStr) {
		LinkedList<String> results = new LinkedList<String>();
		
		File dir = new File(path);
		
		for (String sf : dir.list()) {
//			if (!sf.equals("webpages-m-00000.nt.gz"))
//				continue;
			
			if (fileNameFilterStr != null && !sf.contains(fileNameFilterStr))
				continue;
			
			System.out.println(sf);
			searchFile(path + "/" + sf, subStr, results);
		}
		
		return results;
	}
	
	
	// See a few lines at the beginning of a file
	public void peekFile(String filePath, int numLines) {
		BufferedReader bufReader = IOUtils.getGZIPBufReader(filePath);
		
		try {
			int cnt = 0;
			String sline;
			while (cnt < numLines && (sline = bufReader.readLine()) != null) {
				System.out.println(sline);
				++cnt;
			}
			
			bufReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public LinkedList<String> searchFile(String filePath, String subStr) {
		LinkedList<String> results = new LinkedList<String>();
		searchFile(filePath, subStr, results);
		return results;
	}
	
	// TODO obsolete
	public static void searchFile(String filePath, String subStr, LinkedList<String> results) {		
		try {
			BufferedReader bufReader = IOUtils.getGZIPBufReader(filePath);
			
			// TODO to BigInteger
			BigInteger bigCnt = BigInteger.valueOf(0);
			int cnt = 0;
			String sline;
			while ((sline = bufReader.readLine()) != null) {
				if (sline.contains(subStr)) {
//					System.out.println(sline);
					System.out.println("Found on line " + cnt);
					results.add(sline);
					break;
				}
				
				++cnt;
				
				if (cnt == 100000000) {
					bigCnt = bigCnt.add(BigInteger.valueOf(cnt));
					cnt = 0;
				}
			}
			bigCnt = bigCnt.add(BigInteger.valueOf(cnt));
			
			bufReader.close();

			System.out.println(bigCnt + " lines searched.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
