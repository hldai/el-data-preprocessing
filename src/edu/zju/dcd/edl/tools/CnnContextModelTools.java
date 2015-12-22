package edu.zju.dcd.edl.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import dcd.el.objects.Document;
import dcd.el.objects.LinkingResult;
import dcd.el.utils.TokenizeUtils;
import edu.zju.dcd.edl.ELConsts;
import edu.zju.dcd.edl.cg.CandidatesRetriever;
import edu.zju.dcd.edl.cg.CandidatesRetriever.CandidateWithPopularity;
import edu.zju.dcd.edl.cg.CandidatesRetriever.CandidatesOfMention;
import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;
import edu.zju.dcd.edl.tac.MidToEidMapper;
import edu.zju.dcd.edl.tac.QueryReader;
import edu.zju.dcd.edl.utils.WidMidMapper;
import edu.zju.dcd.edl.wordvec.WordVectorSet;

public class CnnContextModelTools {
	private static class MentionEntry {
		public int begPos, endPos;
		public int[] candidates = null;
	}

	private static class CandidatePopularityEntry implements
			Comparable<CandidatePopularityEntry> {
		public int wid;
		public float popularity;

		@Override
		public int compareTo(CandidatePopularityEntry entry) {
			if (popularity > entry.popularity) {
				return -1;
			} else if (popularity == entry.popularity) {
				return 0;
			}
			return 1;
		}
	}
	
	private static class EntityPopularityEntry implements
			Comparable<EntityPopularityEntry> {
		public int wid = 0;
		public float popularity;

		@Override
		public int compareTo(EntityPopularityEntry entry) {
			return wid - entry.wid;
		}
	}
	
	private static class ResultEntryForTrainingDataGen {
		public int inKbCnt = 0;
		public int singleHitCnt = 0;
		public int hitCnt = 0;
	}

	// TODO not needed?
	public static void genRtsRnnTrainingData(String inFileName,
			String dstFileName) {
		DataInputStream dis = IOUtils.getBufferedDataInputStream(inFileName);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);

		try {
			long paragraphCnt = 0;
			while (dis.available() > 0) {
				++paragraphCnt;

				int numWords = dis.readInt();
				int[] wordIndices = new int[numWords];
				for (int i = 0; i < numWords; ++i) {
					wordIndices[i] = dis.readInt();
				}

				boolean writeFlg = false;
				int numMentions = dis.readInt();
				MentionEntry[] mentionEntries = new MentionEntry[numMentions];
				for (int i = 0; i < numMentions; ++i) {
					mentionEntries[i] = new MentionEntry();
					mentionEntries[i].begPos = dis.readInt();
					mentionEntries[i].endPos = dis.readInt();
					int numCandidates = dis.readInt();
					if (numCandidates > 1)
						writeFlg = true;
					mentionEntries[i].candidates = new int[numCandidates];
					for (int j = 0; j < numCandidates; ++j) {
						mentionEntries[i].candidates[j] = dis.readInt();
					}
				}

				if (!writeFlg) {
					continue;
				}

			}

			dis.close();
			dos.close();
			
			System.out.println(paragraphCnt + " paragraphs");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genTextTrainingDataFromTac(
			CandidatesRetriever candidatesRetriever, MidToEidMapper midToEidMapper, 
			String midWidFileName, String queryFileName, String goldFileName,
			String docDir, String dstFileName) {
		LinkingResult.ComparatorOnQueryId cmpOnQueryId = new LinkingResult.ComparatorOnQueryId();
		LinkingResult[] goldResults = LinkingResult.getGroudTruth(goldFileName);
		Arrays.sort(goldResults, cmpOnQueryId);

		WidMidMapper widToMidMapper = new WidMidMapper(midWidFileName);
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		final int maxNumCandidates = 30;
		int queryCnt = 0, inKbCnt = 0, hitCnt = 0, singleHitCnt = 0;
		Document[] documents = QueryReader.toDocuments(queryFileName, true);
		try {
			for (Document doc : documents) {
				System.out.println(doc.docId);
				queryCnt += doc.mentions.length;
				doc.loadText(docDir);
				
				ResultEntryForTrainingDataGen resultEntry = genTextTrainingDataFromTacDoc(doc, candidatesRetriever,
						goldResults, cmpOnQueryId, midToEidMapper, widToMidMapper, maxNumCandidates,
						writer);
	
				inKbCnt += resultEntry.inKbCnt;
				singleHitCnt += resultEntry.singleHitCnt;
				hitCnt += resultEntry.hitCnt;
	
//				break;
			}
		
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(queryCnt + " queries");
		System.out.println(inKbCnt + " in KB queries");
		System.out.println(singleHitCnt + " single hit");
		System.out.println(hitCnt + " hit");
	}
	

	private static ResultEntryForTrainingDataGen genTextTrainingDataFromTacDoc(Document doc,
			CandidatesRetriever candidatesRetriever, LinkingResult[] goldResults, LinkingResult.ComparatorOnQueryId cmpOnQueryId, 
			MidToEidMapper midToEidMapper, WidMidMapper widToMidMapper, int maxNumCandidates,
			BufferedWriter dstFileWriter) throws IOException {
		ResultEntryForTrainingDataGen resultEntry = new ResultEntryForTrainingDataGen();

		EntityPopularityEntry tmpEntry = new EntityPopularityEntry();
		int curDocPos = 0;
		LinkedList<MentionEntry> mentionEntries = new LinkedList<MentionEntry>();

		CandidatesOfMention[] candidatesOfMentions = candidatesRetriever.getCandidatesInDocument(doc);

		LinkingResult tmpResult = new LinkingResult();
		int curWordCnt = 0;
		for (int i = 0; i < doc.mentions.length; ++i) {
			tmpResult.queryId = doc.mentions[i].queryId;
			
			int pos = Arrays.binarySearch(goldResults, tmpResult,
					cmpOnQueryId);
			if (pos < 0) {
				System.err
						.println("ERROR: query id not found in ground truth. "
								+ tmpResult.queryId);
				continue;
			}

			String goldKbId = goldResults[pos].kbid;
			if (goldKbId.startsWith(ELConsts.NIL)) {
				continue;
			}

			++resultEntry.inKbCnt;
			
			CandidateWithPopularity[] candidates = candidatesOfMentions[i].candidates;
			if (candidates == null || candidates.length == 0) {
				continue;
			}
			
			if (candidates.length == 1) {
				String eid = midToEidMapper.getEid(candidates[0].mid);
				if (eid != null && eid.equals(goldKbId)) {
					++resultEntry.singleHitCnt;
				}
				continue;
			}

			MentionEntry curMentionEntry = new MentionEntry();
			if (doc.mentions[i].beg > curDocPos) {
				TokenizeUtils.Words words = TokenizeUtils.toWords(
						doc.text.substring(curDocPos, doc.mentions[i].beg),
						true);
				if (curWordCnt != 0 && words.numWords > 0) {
					dstFileWriter.write(" ");
				}
				dstFileWriter.write(words.words);
//				System.out.print("$" + words.words + "$");
				curWordCnt += words.numWords;

				curMentionEntry.begPos = curWordCnt;

				words = TokenizeUtils
						.toWords(doc.text.substring(doc.mentions[i].beg,
								doc.mentions[i].end + 1), true);
				if (curWordCnt != 0 && words.numWords > 0)
					dstFileWriter.write(" ");
				dstFileWriter.write(words.words);
//				System.out.print("&" + words.words + "&");
				curWordCnt += words.numWords;

				curMentionEntry.endPos = curWordCnt - 1;

				curDocPos = doc.mentions[i].end + 1;
			}

			CandidatePopularityEntry[] candidatePopularityEntries = new CandidatePopularityEntry[candidates.length];
			int idx = 0;
			CandidatePopularityEntry hitEntry = null;
			for (CandidateWithPopularity candidate : candidates) {
				ByteArrayString mid = candidate.mid;
				float pse = candidate.npse;
				int wid = widToMidMapper.getWid(mid.toString().trim());
//				System.out.println(wid + "\t" + mid.toString().trim());
				
				CandidatePopularityEntry candidatePopularityEntry = new CandidatePopularityEntry();
				candidatePopularityEntries[idx++] = candidatePopularityEntry;

				String eid = midToEidMapper.getEid(mid.toString().trim());
//				System.out.println(wid + "\t" + eid);
				if (eid != null && eid.equals(goldKbId)) {
//					System.out.println(wid + "\t" + mid.toString());
					hitEntry = candidatePopularityEntry;
				}

				candidatePopularityEntry.wid = wid > -1 ? wid : 0;
				if (candidatePopularityEntry.wid == 0) {
					candidatePopularityEntry.popularity = 0;
				} else {
					tmpEntry.wid = wid;
					candidatePopularityEntry.popularity = candidate.npse;
				}
			}
			
			if (hitEntry == null)
				continue;

			Arrays.sort(candidatePopularityEntries);

//			System.out.println(candidatePopularityEntries.length);
			curMentionEntry.candidates = candidatePopularityEntries.length < maxNumCandidates ? new int[candidatePopularityEntries.length]
					: new int[maxNumCandidates];
			curMentionEntry.candidates[0] = hitEntry.wid;
			boolean isHit = false;
			int candidateIdx = 1;
			for (int j = 0; j < candidatePopularityEntries.length
					&& j < maxNumCandidates; ++j) {
//				System.out.print(candidatePopularityEntries[j].wid + "\t"
//						+ candidatePopularityEntries[j].popularity);
				if (candidatePopularityEntries[j] != hitEntry && candidateIdx < maxNumCandidates) {
					curMentionEntry.candidates[candidateIdx++] = candidatePopularityEntries[j].wid;
				} else {
					++resultEntry.hitCnt;
					isHit = true;
				}
//				System.out.println();
			}
//			System.out.println();
			
			if (isHit)
				mentionEntries.add(curMentionEntry);
		}

		TokenizeUtils.Words words = TokenizeUtils
				.toWords(doc.text.substring(curDocPos), true);
		if (curWordCnt != 0 && words.numWords > 0) {
			dstFileWriter.write(" ");
		}
		dstFileWriter.write(words.words + "\n");
		
		dstFileWriter.write(mentionEntries.size() + "\n");
		for (MentionEntry entry : mentionEntries) {
			dstFileWriter.write(entry.begPos + "\t" + entry.endPos + "\n");
			dstFileWriter.write(entry.candidates.length + "\n");
			for (int i = 0; i < entry.candidates.length; ++i) {
				dstFileWriter.write(entry.candidates[i] + "\n");
			}
		}
		
		return resultEntry;
	}
	
	public static void genCnnTrainingDataTac(String textDataFileName,
			String wordVecFileName, String dstFileName) {
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(textDataFileName);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			String line = null;
			int mentionCnt = 0;
			int candidatesCnt = 0;
			boolean errorFlg = false;
			while ((line = reader.readLine()) != null && !errorFlg) {
				int numMentions = Integer.valueOf(reader.readLine());
				
				if (numMentions > 0) {
//					System.out.println(numMentions);
					String[] words = line.split(" ");
					dos.writeInt(words.length);
					for (String word : words) {
						int idx = wordVectorSet.getWordIndex(word);
//						System.out.print(idx + " " + word + " ");
						dos.writeInt(idx);
					}
//					System.out.println();
					
					dos.writeInt(numMentions);
				}
				
				mentionCnt += numMentions;
				for (int i = 0; i < numMentions; ++i) {
					String spanLine = reader.readLine();
					String[] tmpVals = spanLine.split("\t");
					int begPos = Integer.valueOf(tmpVals[0]);
					int endPos = Integer.valueOf(tmpVals[1]);
					
					int numCandidates = Integer.valueOf(reader.readLine());
					if (numCandidates < 2) {
						System.out.println("numCandidates: " + numCandidates + ". smaller than 1");
						errorFlg = true;
						break;
					}
					
					dos.writeInt(begPos);
					dos.writeInt(endPos);
					
					candidatesCnt += numCandidates;
					dos.writeInt(numCandidates);
					
					int goldWid = Integer.valueOf(reader.readLine());
					int wid = Integer.valueOf(reader.readLine());
					dos.writeInt(wid);
					dos.writeInt(goldWid);
					for (int j = 2; j < numCandidates; ++j) {
						wid = Integer.valueOf(reader.readLine());
						dos.writeInt(wid);
					}
				}
			}
			System.out.println(mentionCnt);
			System.out.println((float)candidatesCnt / mentionCnt);
			
			reader.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static EntityPopularityEntry[] loadEntityPopularities(
			String fileName) {
		System.out.println("loading entity popularities...");
		LinkedList<EntityPopularityEntry> entityPopularityEntries = new LinkedList<EntityPopularityEntry>();

		DataInputStream dis = IOUtils.getBufferedDataInputStream(fileName);
		try {
			int preWid = -1;
			while (dis.available() > 0) {
				EntityPopularityEntry entry = new EntityPopularityEntry();
				entry.wid = dis.readInt();

				if (entry.wid < preWid) {
					System.out.println("WARNING: wid order not right.");
				}
				preWid = entry.wid;

				entry.popularity = dis.readFloat();
				entityPopularityEntries.add(entry);
			}

			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("done.");
		return entityPopularityEntries
				.toArray(new EntityPopularityEntry[entityPopularityEntries
						.size()]);
	}
}
