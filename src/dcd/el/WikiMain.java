// author: DHL brnpoem@gmail.com

package dcd.el;

import dcd.config.IniFile;
import dcd.el.io.IOUtils;
import dcd.el.io.PairListFile;
import dcd.el.utils.TupleFileTools;
import dcd.el.wiki.WikiTools;

public class WikiMain {
	public static final String ORIGIN_DUMP = "d:/data/el/wiki/enwiki-20150403-pages-articles.xml.gz";
	public static final String PAGE_ONLY_WIKI_P = "d:/data/el/wiki/enwiki-20150403-pages-only-articles_p.xml.gz";
	public static final String PAGE_ONLY_WIKI = "d:/data/el/wiki/enwiki-20150403-pages-only-articles.xml";
	
	public static final String WIKI_NRED_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred.xml";
	public static final String REDIRECT_LIST_FILE = "d:/data/el/wiki/redirect_alias_list.txt";
	
	public static final String WEIRD_LIST_FOR_REDIRECT = "d:/data/el/wiki/enwiki-20150403-weird-for-red.txt";
	
	public static final String WIKI_STRIPED_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred-strip.txt";
	public static final String WEIRD_LIST_FOR_STRIP = "d:/data/el/wiki/enwiki-20150403-weird-for-strip.txt";
	public static final String WIKI_STRIPED_FILE_WITH_ID = "d:/data/el/wiki/enwiki-20150403-nred-strip-id.txt";
	public static final String WEIRD_LIST_FOR_STRIP_WITH_ID = "d:/data/el/wiki/enwiki-20150403-nred-strip-id-unmatch.txt";
	
	public static final String WIKI_STRIPED_S0_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred-strip_s0.txt";
	
	public static final String WIKI_SPECCHAR_REPLACED_FILE = "d:/data/el/wiki/enwiki-20150403-a0-b0-c1-d0_specchar_replaced_with_id.txt";
	public static final String GEN_ANCHOR_LIST_TEST_FILE = "d:/data/el/wiki/enwiki-20150403-a0-b0-c1-d0_specchar_replaced_with_id_200pages.txt";
	
	public static final String ANCHOR_LIST_FILE = "d:/data/el/wiki/enwiki-20150403-a0-b0-c1-d1_specchar_replaced_with_id_anchors.txt";
	public static final String ANCHOR_ALIAS_LIST_FILE = "d:/data/el/wiki/anchor_alias_list.txt";
	
	public static final String CLEANED_ANCHOR_ALIAS_LIST_FILE = "d:/data/el/wiki/anchor_alias_list_cleaned.txt";
	public static final String CLEANED_REDIRECT_ALIAS_LIST_FILE = "d:/data/el/wiki/redirect_alias_list_cleaned.txt";
	public static final String MERGED_ALIAS_LIST_FILE = "d:/data/el/wiki/merged_alias_list.txt";
	
	public static final String SPEC_CHAR_FILE0 = "d:/data/el/wiki/charslist0.txt";
	public static final String SPEC_CHAR_FILE1 = "d:/data/el/wiki/charslist1.txt";
	public static final String MERGED_CHAR_LIST_FILE = "d:/data/el/wiki/charslist.txt";
	public static final String MERGED_CHAR_LIST_FILE_SORTED = "d:/data/el/wiki/charslist_sorted.txt";
	
	public static void stripWikiXMLWithID() {
		WikiTools.stripWikiXML(WIKI_NRED_FILE, WIKI_STRIPED_FILE_WITH_ID, WEIRD_LIST_FOR_STRIP_WITH_ID, true);
	}
	
	public static void checkDumpSymbols() {
		WikiTools.checkDumpSymbols(PAGE_ONLY_WIKI);
	}
	
	public static void extractXMLPages() {
		WikiTools.extractXMLPages(ORIGIN_DUMP, PAGE_ONLY_WIKI);
	}
	
	public static void filterRedirect() {
		WikiTools.filterRedirect(PAGE_ONLY_WIKI, WIKI_NRED_FILE,
				REDIRECT_LIST_FILE, WEIRD_LIST_FOR_REDIRECT);
	}
	
//	public static void readStrippedWiki() {
//		WikiTools.readStrippedWiki(WIKI_STRIPED_S0_FILE);
//	}
	
	public static void genAnchorList() {
		String outputFileName = IOUtils.addSuffixToFileName(WIKI_SPECCHAR_REPLACED_FILE, "_anchors");
		WikiTools.genAnchorList(WIKI_SPECCHAR_REPLACED_FILE, outputFileName);
//		String outputFileName = CommonUtils.addSuffixToFileName(GEN_ANCHOR_LIST_TEST_FILE, "_anchors");
//		WikiTools.genAnchorList(GEN_ANCHOR_LIST_TEST_FILE, outputFileName);
	}
	
	public static void genAnchorAliasList() {
		WikiTools.genAnchorAliasList(ANCHOR_LIST_FILE, ANCHOR_ALIAS_LIST_FILE);
	}
	
	public static void mergeAliasFiles() {
		String[] fileNames = new String[2];
		fileNames[0] = CLEANED_ANCHOR_ALIAS_LIST_FILE;
		fileNames[1] = CLEANED_REDIRECT_ALIAS_LIST_FILE;
		
		PairListFile.mergePairListFiles(fileNames, MERGED_ALIAS_LIST_FILE);
	}
	
	public static void mergeCharList() {
//		WikiTools.mergeCharListFiles(SPEC_CHAR_FILE0, SPEC_CHAR_FILE1, MERGED_CHAR_LIST_FILE);
//		PairFileSort.pairFileSort(MERGED_CHAR_LIST_FILE, 0, MERGED_CHAR_LIST_FILE_SORTED);
		TupleFileTools.sort(MERGED_CHAR_LIST_FILE, MERGED_CHAR_LIST_FILE_SORTED, new TupleFileTools.SingleFieldComparator(0));
	}
	
	public static void searchWikiXMLPage(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_search_wiki_page");
		if (sect == null)
			return;
		
		String fileName = sect.getValue("src_file"),
				dstFileName = sect.getValue("dst_file");
		String wid = sect.getValue("wid");
		
		WikiTools.searchWikiXMLPageById(fileName, wid, dstFileName);
	}
	
	public static void test(IniFile iniFile) {
		System.out.println(iniFile.getValue("wikitest", "arg0"));
	}
	
	public static void run(IniFile iniFile) {
//		countLinesInFile();
//		test();
//		mergeCharList();
//		checkDumpSymbols();
//		stripWikiXMLWithID();
//		readStrippedWiki();
//		genAnchorList();
//		genAnchorAliasList();
		String job = iniFile.getValue("main", "job");
		if (job.equals("wikitest")) {
			test(iniFile);
		} else if (job.equals("wiki_search_wiki_page")) {
			searchWikiXMLPage(iniFile);
		}
		
//		mergeAliasFiles();
	}
}
