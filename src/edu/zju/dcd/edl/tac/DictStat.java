// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl.tac;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

import edu.zju.dcd.edl.cg.AliasDictWithIndex;
import edu.zju.dcd.edl.config.ConfigUtils;
import edu.zju.dcd.edl.config.IniFile;
import edu.zju.dcd.edl.io.IOUtils;

public class DictStat {
	public static void run(IniFile config) {
		IniFile.Section sect = config.getSection("dict_stat");
		if (sect == null)
			return;

		AliasDictWithIndex dict = ConfigUtils.getAliasDict(sect);
		MidToEidMapper mteMapper = ConfigUtils.getMidToEidMapper(sect);

		String queryFileName = sect.getValue("query_file"), goldResultFileName = sect
				.getValue("gold_file"),
				errorListFileName = sect.getValue("error_list_file");
		genDictStat(dict, mteMapper, queryFileName, goldResultFileName, errorListFileName);
	}

	public static void genDictStat(AliasDictWithIndex dict,
			MidToEidMapper mapper, String queryFileName,
			String goldResultFileName, String errorListFileName) {
//		QueryReader queryReader = new QueryReader(queryFileName);
//		BufferedReader goldReader = IOUtils
//				.getUTF8BufReader(goldResultFileName);
//		BufferedWriter errorWriter = IOUtils.getUTF8BufWriter(errorListFileName, false);
//		int queryCnt = 0, inKbCnt = 0, candidateCnt = 0, hitCnt = 0, inKbHitCnt = 0;
//		int nilCnt = 0;
//		int maxQueryNameLen = 0;
//		Query query = null;
//
//		try {
//			goldReader.readLine(); // skip first line
//
//			while ((query = queryReader.nextQuery(true)) != null) {
//				if (query.name.length() > maxQueryNameLen) {
//					maxQueryNameLen = query.name.length();
//				}
//				
//				String goldLine = goldReader.readLine();
//
//				++queryCnt;
//
//				LinkedList<ByteArrayString> mids = dict.getMids(query.name);
//				if (mids != null) {
//					candidateCnt += mids.size();
//				}
//
//				String[] goldVals = goldLine.split("\t");
//				if (goldVals[1].startsWith("NIL")) {
//					++hitCnt;
//					++nilCnt;
//				} else {
//					++inKbCnt;
//					if (mids != null) {
//						boolean hitFlg = false;
//						for (ByteArrayString mid : mids) {
//							String eid = mapper.getEid(mid);
//							if (eid != null && eid.equals(goldVals[1])) {
//								++hitCnt;
//								++inKbHitCnt;
//								hitFlg = true;
//								break;
//							}
//						}
//						if (!hitFlg) {
//							errorWriter.write(query.docId + "\t" + query.name + "\t" + goldVals[1] + "\t1\n");
//						}
//					} else {
//						// System.out.println(query.name + "\t" + goldVals[1]);
//						errorWriter.write(query.docId + "\t" + query.name + "\t" + goldVals[1] + "\t0\n");
//					}
//				}
//				
//				if (queryCnt % 500 == 0) {
//					System.out.println(queryCnt);
//				}
//			}
//
//			goldReader.close();
//			errorWriter.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		double recall = (double) hitCnt / queryCnt, inKbRecall = (double) inKbHitCnt
//				/ inKbCnt, avgNumCandidates = (double) candidateCnt / queryCnt;
//		System.out.println(queryCnt + " queries. " + nilCnt
//				+ " of which are NILs. (" + (double) nilCnt / queryCnt + ")");
//		System.out.println("max query name length: " + maxQueryNameLen);
//		System.out.println("Recall: " + recall);
//		System.out.println("In KB Recall: " + inKbRecall);
//		System.out.println("Average number of candidates: " + avgNumCandidates);
	}
}
