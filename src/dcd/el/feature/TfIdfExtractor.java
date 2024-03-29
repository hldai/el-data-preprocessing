// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;
import dcd.el.utils.TokenizeUtils;

public class TfIdfExtractor {
	public static class WordCount implements Comparable<WordCount> {
		public int index = -1;
		public int count = -1;
		
		@Override
		public int compareTo(WordCount wcr) {
			return index - wcr.index;
		}
	}
	
	public TfIdfExtractor(String idfFileName) {
		System.out.println("Loading idf file...");
		DataInputStream dis = IOUtils.getBufferedDataInputStream(idfFileName);
		
		try {
			int numTerms = dis.readInt();
			terms = new ByteArrayString[numTerms];
			idfs = new double[numTerms];
			for (int i = 0; i < numTerms; ++i) {
				terms[i] = new ByteArrayString();
				terms[i].fromFileWithByteLen(dis);
				idfs[i] = dis.readDouble();
				if (idfs[i] < 2) {
					System.out.println(terms[i].toString() + "\t" + idfs[i]);
				}
			}
			
			dis.close();
			System.out.println("Done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public TfIdfFeature getTfIdf(String text) {
		if (text == null)
			return null;
		
//		TreeMap<String, Integer> termCntsMap = BagOfWords.toBagOfWords(text);
		TreeMap<String, Integer> termCntsMap = TokenizeUtils.toBagOfWords(text);

		int docTermCnt = 0;
		TfIdfFeature feat = new TfIdfFeature();
		int[] termIndices = new int[termCntsMap.size()];
		int[] termCnts = new int[termCntsMap.size()];
		int cnt = 0;
		ByteArrayString bas = new ByteArrayString();
		for (Map.Entry<String, Integer> entry : termCntsMap.entrySet()) {
			docTermCnt += entry.getValue();
			
			bas.fromString(entry.getKey());
			int pos = Arrays.binarySearch(terms, bas);
//			System.out.println(entry.getKey() + "\t" + pos);
			if (pos > -1) {
				termIndices[cnt] = pos;
				termCnts[cnt] = entry.getValue();
				++cnt;
			}
		}

		feat.termIndices = Arrays.copyOf(termIndices, cnt);
//		feat.values = new double[cnt];
		feat.tfs = new float[cnt];
		feat.idfs = new float[cnt];
		for (int i = 0; i < cnt; ++i) {
//			System.out.println(termCnts[i] + "\t" + docTermCnt + "\t" + idfs[termIndices[i]]);
//			feat.values[i] = (double) termCnts[i] / docTermCnt * idfs[termIndices[i]];
			feat.tfs[i] = (float) termCnts[i] / docTermCnt;
			feat.idfs[i] = (float) idfs[termIndices[i]];
		}

		return feat;
	}

	public TfIdfFeature getTfIdf(int numTerms, int[] termIndices, int[] termCnts,
			int numDocTerms) {
		TfIdfFeature feature = new TfIdfFeature();
		feature.termIndices = termIndices;
		for (int i = 0; i < numTerms; ++i) {
//			feature.values[i] = (double)termCnts[i] / numDocTerms * idfs[termIndices[i]];
			feature.tfs[i] = (float) termCnts[i] / numDocTerms;
			feature.idfs[i] = (float) idfs[termIndices[i]];
		}
		return feature;
	}
	
	public TfIdfFeature getTfIdf(WordCount[] wordCounts, int numWords, int numDocTerms) {
		TfIdfFeature feature = new TfIdfFeature();
		feature.termIndices = new int[numWords];
//		feature.values = new double[numWords];
		feature.tfs = new float[numWords];
		feature.idfs = new float[numWords];
		for (int i = 0; i < numWords; ++i) {
			feature.termIndices[i] = wordCounts[i].index;
//			System.out.println(wordCounts[i].count + " " + idfs[wordCounts[i].index]);
//			feature.values[i] = (double)wordCounts[i].count / numDocTerms * idfs[wordCounts[i].index];
			feature.tfs[i] = (float)wordCounts[i].count / numDocTerms;
			feature.idfs[i] = (float)idfs[wordCounts[i].index];
		}
		return feature;
	}
	
	public int getTermIndex(String term) {
		return Arrays.binarySearch(terms, new ByteArrayString(term));
	}
	
	public ByteArrayString getTerm(int idx) {
		return terms[idx];
	}
	
	private ByteArrayString[] terms = null;
	private double[] idfs = null;
}
