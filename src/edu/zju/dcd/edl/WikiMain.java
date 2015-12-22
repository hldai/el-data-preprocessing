// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl;

import edu.zju.dcd.edl.config.IniFile;
import dcd.el.io.PairListFile;
import dcd.el.utils.TupleFileTools;
import dcd.el.wiki.WikiTools;

public class WikiMain {
	private static final String ORIGIN_DUMP = "d:/data/el/wiki/enwiki-20150403-pages-articles.xml.gz";
	private static final String PAGE_ONLY_WIKI_P = "d:/data/el/wiki/enwiki-20150403-pages-only-articles_p.xml.gz";
	private static final String PAGE_ONLY_WIKI = "d:/data/el/wiki/enwiki-20150403-pages-only-articles.xml";
	
	private static final String WIKI_NRED_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred.xml";
	private static final String REDIRECT_LIST_FILE = "d:/data/el/wiki/redirect_alias_list.txt";
	
	private static final String WEIRD_LIST_FOR_REDIRECT = "d:/data/el/wiki/enwiki-20150403-weird-for-red.txt";
	
	private static final String WIKI_STRIPED_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred-strip.txt";
	private static final String WEIRD_LIST_FOR_STRIP = "d:/data/el/wiki/enwiki-20150403-weird-for-strip.txt";
	private static final String WIKI_STRIPED_FILE_WITH_ID = "d:/data/el/wiki/enwiki-20150403-nred-strip-id.txt";
	private static final String WEIRD_LIST_FOR_STRIP_WITH_ID = "d:/data/el/wiki/enwiki-20150403-nred-strip-id-unmatch.txt";
	
	private static final String WIKI_STRIPED_S0_FILE = "d:/data/el/wiki/enwiki-20150403-pages-only-articles-nred-strip_s0.txt";
	
	private static final String WIKI_SPECCHAR_REPLACED_FILE = "d:/data/el/wiki/enwiki-20150403-a0-b0-c1-d0_specchar_replaced_with_id.txt";
	
	private static final String CLEANED_ANCHOR_ALIAS_LIST_FILE = "d:/data/el/wiki/anchor_alias_list_cleaned.txt";
	private static final String CLEANED_REDIRECT_ALIAS_LIST_FILE = "d:/data/el/wiki/redirect_alias_list_cleaned.txt";
	private static final String MERGED_ALIAS_LIST_FILE = "d:/data/el/wiki/merged_alias_list.txt";
	
	private static final String SPEC_CHAR_FILE0 = "d:/data/el/wiki/charslist0.txt";
	private static final String SPEC_CHAR_FILE1 = "d:/data/el/wiki/charslist1.txt";
	private static final String MERGED_CHAR_LIST_FILE = "d:/data/el/wiki/charslist.txt";
	private static final String MERGED_CHAR_LIST_FILE_SORTED = "d:/data/el/wiki/charslist_sorted.txt";
	
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
		WikiTools.test();
	}
	
	public static void cleanArticleAnchorFile(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_clean_article_anchor_file");
		String fileName = sect.getValue("file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.cleanArticleAnchorFile(fileName, dstFileName);
	}
	
	public static void genTitleToWidFile(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_gen_title_wid_file");
		String widTitleTextFileName = sect.getValue("wid_title_text_file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.genTitleToWidFile(widTitleTextFileName, dstFileName);
	}
	
	public static void genTitleWidFileWithRedirect(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_gen_title_wid_file_with_redirect");
		String titleWidFileName = sect.getValue("title_wid_file"),
				redirectFileName = sect.getValue("redirect_file"),
				dstTextFileName = sect.getValue("dst_text_file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.genTitleWidFileWithRedirect(titleWidFileName, redirectFileName, 
				dstTextFileName, dstFileName);
	}
	
	public static void genAnchorCntFile(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_gen_anchor_cnt_file");
		String articleAnchorFileName = sect.getValue("article_anchor_file"),
				titleWidFileName = sect.getValue("title_wid_file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.genAnchorCntFile(articleAnchorFileName, titleWidFileName, dstFileName);
	}
	
	public static void genTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_gen_training_data");
		String allWikiTextFileName = sect.getValue("file"),
				midToWidFileName = sect.getValue("mid_to_wid_file"),
				widTitleFileName = sect.getValue("wid_title_file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.genTrainingDataFromWiki(allWikiTextFileName, midToWidFileName, widTitleFileName, dstFileName);
	}
	
	private static void cleanTextData(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_clean_text_data");
		String fileName = sect.getValue("file"),
				dstFileName = sect.getValue("dst_file");
		WikiTools.cleanWikiTextData(fileName, dstFileName);
	}
	
	private static void extractKeywordsForEntities(IniFile config) {
		IniFile.Section sect = config.getSection("wiki_extract_keywords");
		String wikiArticleWordCntFileName = sect
				.getValue("wiki_article_wc_file"), idfFileName = sect
				.getValue("idf_file"), dstFileName = sect.getValue("dst_file");
		WikiTools.extractKeywordsForEntities(wikiArticleWordCntFileName,
				idfFileName, dstFileName);
	}
	
	public static void run(IniFile config) {
//		countLinesInFile();
//		test();
//		mergeCharList();
//		checkDumpSymbols();
//		stripWikiXMLWithID();
//		readStrippedWiki();
		String job = config.getValue("main", "job");
		if (job.equals("wiki_test"))
			test(config);
		else if (job.equals("wiki_search_wiki_page"))
			searchWikiXMLPage(config);
		else if (job.equals("wiki_clean_article_anchor_file"))
			cleanArticleAnchorFile(config);
		else if (job.equals("wiki_gen_title_wid_file"))
			genTitleToWidFile(config);
		else if (job.equals("wiki_gen_title_wid_file_with_redirect"))
			genTitleWidFileWithRedirect(config);
		else if (job.equals("wiki_gen_anchor_cnt_file"))
			genAnchorCntFile(config);
		else if (job.equals("wiki_gen_training_data"))
			genTrainingData(config);
		else if (job.equals("wiki_clean_text_data"))
			cleanTextData(config);
		else if (job.equals("wiki_extract_keywords"))
			extractKeywordsForEntities(config);
	}
}
