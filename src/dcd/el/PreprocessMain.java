// author: DHL brnpoem@gmail.com

package dcd.el;

import java.util.HashMap;

import dcd.config.IniFile;
import dcd.el.tools.MiscTools;

public class PreprocessMain {
	public static final String GZIP_DATA_PATH = "d:/data/el/LDC2015E42/data/webpages-m-00000.nt.gz";
	public static final String FREEBASE_PATH = "d:/data/el/LDC2015E42/data";

	public static final String SEARCH_FILE = "d:/data/el/freebase-rdf-2015-01-25-00-00.gz";
	// public static final String SEARCH_FILE =
	// "d:/data/el/LDC2015E42/data/webpages-m-00010.nt.gz";
	public static final String SEARCH_SMID = "m.0_hcwg_";
	public static final String SEARCH_RESULT_FILE = "d:/data/el/search_results.txt";
	public static final String SEARCH_FILE_NAME_FILETER = "webpages";
	public static final String PATH_SEARCH_RESULT_FILE = "d:/data/el/path_search_results.txt";

	public static final String MID_STAT_FILENAME = "d:/data/el/mid_stat.txt";
	public static final String MID_LIST_FILENAME = "d:/data/el/mids_from_label.txt";

	public static final String GZIP_TEST_PATH = "d:/data/el/results.txt.gz";

	public static final String FREEBASE_WIKIPEDIA_MAP_PRE = "d:/data/el/mid_to_wikipedia_pre.txt";
	public static final String FREEBASE_WIKIPEDIA_MAP = "d:/data/el/mid_to_wikipedia.txt";
	public static final String MAPPED_NON_ID_WIKI_URL_FILE = "d:/data/el/mid_to_wikipedia_non_id.txt";

	public static final String FREEBASE_FULL_DUMP_FILE_NAME = "d:/data/el/freebase-rdf-2015-01-25-00-00.gz";
	
	public static final String FREEBASE_FULL_WEBPAGE_LIST = "d:/data/el/freebase_webpage_list_full.txt";
	public static final String FULL_FREEBASE_WIKIPEDIA_MAP = "d:/data/el/mid_to_wiki_full.txt";
	public static final String FULL_MAPPED_NON_ID_WIKI_URL_FILE = "d:/data/el/mid_to_wikipedia_non_id_full.txt";
	public static final String FULL_MAPPED_ID_WIKI_URL_FILE = "d:/data/el/mid_to_wikipedia_id_full.txt";

	public static final String COMP_WIKI_ID_LIST_FILE = "e:/el_data/enwiki-20150403-id-list.txt";


	public static final String WIKI_ID_POP_FILE = "e:/el_data/enwiki-20150403-id-len-list.txt";
	public static final String FREEBASE_MID_POP_FILE = "d:/data/el/mid_popularity_from_wiki_len.txt";


	public static void searchFreebasePath() {
		MiscTools.retrieveFromFreebasePath(FREEBASE_PATH, SEARCH_SMID,
				SEARCH_FILE_NAME_FILETER, PATH_SEARCH_RESULT_FILE);
	}

	public static void searchRdf() {
		MiscTools.retrieveFromRdf(SEARCH_FILE, SEARCH_SMID, SEARCH_RESULT_FILE);
	}

	public static void genMidList() {
		MiscTools.genFreebaseMidList(FREEBASE_PATH, "label", MID_LIST_FILENAME);
	}

	public static void genFreebaseFullDumpWebpage() {
		MiscTools.genFreebaseFullDumpWebpageList(FREEBASE_FULL_DUMP_FILE_NAME,
				FREEBASE_FULL_WEBPAGE_LIST);
	}


	public static void washFreebaseFullDumpWebpageList() {
		MiscTools.washFreebaseWebpageListFile(FREEBASE_FULL_WEBPAGE_LIST,
				FULL_FREEBASE_WIKIPEDIA_MAP);
	}

	public static void splitMappedWikiUrlList() {
		MiscTools.splitMappedWikiUrlList(FULL_FREEBASE_WIKIPEDIA_MAP,
				FULL_MAPPED_ID_WIKI_URL_FILE, FULL_MAPPED_NON_ID_WIKI_URL_FILE);
	}

	public static void findUnMatchedIds() {
		MiscTools.genMissMatchWikiList(FULL_MAPPED_ID_WIKI_URL_FILE,
				COMP_WIKI_ID_LIST_FILE);
	}

	public static void genFreebaseTopicPopularity() {
		MiscTools.genFreebasePopFromWiki(FULL_MAPPED_ID_WIKI_URL_FILE,
				WIKI_ID_POP_FILE, FREEBASE_MID_POP_FILE);
	}
	
	// step 0
	public static void mapFreebaseToWiki0() {
		MiscTools.mapFreebaseToWiki(FREEBASE_PATH, FREEBASE_WIKIPEDIA_MAP_PRE);
	}

	// step 1
	public static void mapFreebaseToWiki1() {
		MiscTools.washFreebaseWebpageListFile(FREEBASE_WIKIPEDIA_MAP_PRE,
				FREEBASE_WIKIPEDIA_MAP);
	}
	
	public static void test() {
		HashMap<String, Integer> m = new HashMap<String, Integer>();
//		m.put("DHL", 4);
		m.put("DHL", m.get("DHL") + 1);
		System.out.println(m.get("DHL"));
	}

	public static void rdfOrderCheck() {
		boolean rslt = MiscTools.freebaseOrderCheck(FREEBASE_PATH, "webpages");
		if (rslt) {
			System.out.println("yes");
		} else {
			System.out.println("no");
		}
	}
		
	public static void runWithIni() {
		IniFile iniFile = new IniFile("d:/data/el/config/java_tools.ini");
		String job = iniFile.getValue("main", "job");
		
		System.out.println("Job: " + job);
		
		if (job.startsWith("wiki"))
			WikiMain.run(iniFile);
		else if (job.startsWith("dict"))
			GenDictMain.run(iniFile);
		else if (job.startsWith("misc"))
			MiscMain.run(iniFile);
		else if (job.startsWith("feat"))
			FeatureMain.run(iniFile);
		else if (job.startsWith("freebase"))
			FreebaseMain.run(iniFile);
		else if (job.equals("test"))
			test();
	}


	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();

		// test();
		// genFreebaseFullDumpWebpageList();
		// washFreebaseFullDumpWebpageList();
		// splitMappedWikiUrlList();
		// findUnMatchedIds();

		// checkMaxNameLen();
		// nameFileToDb();
		// genFreebaseTopicPopularity();
		// processFreebaseEnNames();
//		checkWikiPopFileOrder();

		runWithIni();


		long endTime = System.currentTimeMillis();
		System.out.println((endTime - startTime) / 1000.0 + " seconds used.");
		// searchFreebasePath();
		// searchRdf();
		// genMidList();
		// rdfOrderCheck();
		// test();

		// mapFreebaseToWiki2();
	}
}
