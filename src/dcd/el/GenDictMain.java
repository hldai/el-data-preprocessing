// author: DHL brnpoem@gmail.com

package dcd.el;

import java.util.LinkedList;

import dcd.config.IniFile;
import dcd.el.dict.AliasDictWithIndex;
import dcd.el.dict.DictGen;
import dcd.el.dict.IndexedAliasDictWithPse;
import dcd.el.dict.SimpleWikiDict;
import dcd.el.objects.ByteArrayString;
import dcd.el.tac.DictStat;
import dcd.el.tools.MiscTools;
import dcd.el.utils.TupleFileTools;

public class GenDictMain {
	public static final String FREEBASE_FULL_DUMP_FILE_NAME = "d:/data/el/freebase-rdf-2015-01-25-00-00.gz";

	public static final String FULL_FREEBASE_NAME_FILE = "d:/data/el/freebase_name_full.txt";
	public static final String FULL_FREEBASE_ALIAS_FILE = "d:/data/el/freebase_alias_full.txt";

	public static final String FULL_FREEBASE_NAME_EN_FILE = "d:/data/el/full_freebase_name_en.txt";
	public static final String FULL_FREEBASE_ALIAS_EN_FILE = "d:/data/el/full_freebase_alias_en.txt";
	public static final String FULL_FREEBASE_NAME_EN_LC_FILE = "d:/data/el/full_freebase_name_en_lc.txt";
	public static final String FULL_FREEBASE_ALIAS_EN_LC_FILE = "d:/data/el/full_freebase_alias_en_lc.txt";

	public static final String ORDERED_NAME_FILE = "d:/data/el/ordered_name_list.txt";

	public static final String SORT_FILE = "d:/data/el/full_freebase_alias_en.txt";
	public static final String SORT_RESULT_FILE = "d:/data/el/full_freebase_alias_en_sorted.txt";

	public static final String MERGED_NAME_ALIAS_EN_LC_FILE = "d:/data/el/full_freebase_name_alias_merged_en_lc.txt";
	public static final String MERGED_NAME_ALIAS_EN_LC_ORDER_BY_MID_FILE = "d:/data/el/full_freebase_name_alias_merged_en_lc_order_by_mid.txt";
	public static final String MERGED_NAME_ALIAS_EN_LC_ORDER_BY_MID_NAME_DUP_FILE = "d:/data/el/full_freebase_name_alias_merged_en_lc_order_by_name_mid_dup.txt";
	public static final String MERGED_NAME_ALIAS_EN_LC_ORDER_BY_MID_NAME_FILE = "d:/data/el/full_freebase_name_alias_merged_en_lc_order_by_name_mid.txt";

	public static final String WIKI_ANCHOR_ALIAS_LIST_CLEANED_FILE = "d:/data/el/wiki/anchor_alias_list_cleaned.txt";

	public static final String CLEANED_REDIRECT_ALIAS_LIST_FILE = "d:/data/el/wiki/redirect_alias_list_cleaned.txt";

	public static final int NUM_ALIASES = 23585781;
	public static final int NUM_DICT_INDICES = 1000000;

	public static void genFreebaseFullDumpName() {
		MiscTools.genFreebaseFullDumpName(FREEBASE_FULL_DUMP_FILE_NAME,
				FULL_FREEBASE_NAME_FILE);
	}

	public static void genFreebaseFullDumpAlias() {
		MiscTools.genFreebaseFullDumpAlias(FREEBASE_FULL_DUMP_FILE_NAME,
				FULL_FREEBASE_ALIAS_FILE);
	}

	public static void genFullFreebaseEnName() {
		// MiscTools.genFullFreebaseEnName(FULL_FREEBASE_NAME_FILE,
		// FULL_FREEBASE_NAME_EN_FILE, false);
		MiscTools.genFullFreebaseEnName(FULL_FREEBASE_NAME_FILE,
				FULL_FREEBASE_NAME_EN_LC_FILE, true);
	}

	public static void genFullFreebaseEnAlias() {
		// MiscTools.genFullFreebaseEnName(FULL_FREEBASE_ALIAS_FILE,
		// FULL_FREEBASE_ALIAS_EN_FILE, false);
		MiscTools.genFullFreebaseEnName(FULL_FREEBASE_ALIAS_FILE,
				FULL_FREEBASE_ALIAS_EN_LC_FILE, true);
	}

	public static void checkMaxNameLen() {
		int maxLen = MiscTools.maxNameLen(FULL_FREEBASE_NAME_EN_FILE);
		System.out.println("Max length is " + maxLen + " in "
				+ FULL_FREEBASE_NAME_EN_FILE);

		maxLen = MiscTools.maxNameLen(FULL_FREEBASE_ALIAS_EN_FILE);
		System.out.println("Max length is " + maxLen + " in "
				+ FULL_FREEBASE_ALIAS_EN_FILE);
	}

	public static void processFreebaseEnNames() {
		MiscTools.processFreebaseEnNames(FULL_FREEBASE_NAME_EN_FILE,
				ORDERED_NAME_FILE);
	}

	public static void mergeNameAlias() {
		// String[] fileNames = { "d:/data/el/en_name_100_sorted.txt",
		// "d:/data/el/en_name_100.txt" };
		String[] fileNames = { FULL_FREEBASE_ALIAS_EN_LC_FILE,
				FULL_FREEBASE_NAME_EN_LC_FILE };
		MiscTools.mergePairFiles(fileNames, MERGED_NAME_ALIAS_EN_LC_FILE);
	}

	public static void checkMaxMidLen() {
		DictGen.getMaxMidLen(MERGED_NAME_ALIAS_EN_LC_ORDER_BY_MID_NAME_FILE);
	}

	public static void testDict(IniFile config) {
		IniFile.Section sect = config.getSection("dict_test_dict");

		String dictAliasFileName = sect.getValue("dict_alias_file"), dictAliasIndexFileName = sect
				.getValue("dict_alias_index_file"), dictMidFileName = sect
				.getValue("dict_mid_file");

		boolean dictWithPse = true;
		long ct = 0;
		if (dictWithPse) {
			IndexedAliasDictWithPse dict = new IndexedAliasDictWithPse(
					dictAliasFileName, dictAliasIndexFileName, dictMidFileName);
			ct = System.currentTimeMillis();
			IndexedAliasDictWithPse.MidPseList candidates = dict
					.getMidPses("NEWTON");

			if (candidates != null) {
				int cnt = 0;
				for (ByteArrayString mid : candidates.mids) {
					System.out.println(++cnt + " " + mid.toString().trim());
				}
			}
		} else {
			AliasDictWithIndex dict = new AliasDictWithIndex(dictAliasFileName,
					dictAliasIndexFileName, dictMidFileName);

			ct = System.currentTimeMillis();

			LinkedList<ByteArrayString> mids = dict.getMids("NEWTON");
			if (mids != null) {
				int cnt = 0;
				for (ByteArrayString mid : mids) {
					System.out.println(++cnt + " " + mid.toString().trim());
				}
			}
			dict.close();
		}

		System.out.println("dict search time: "
				+ (System.currentTimeMillis() - ct) / 1000.0 + " seconds.");
	}
	
	public static void testSimpleWikiDict(IniFile config) {
		IniFile.Section sect = config.getSection("dict_test_simple_wiki_dict");
		String aliasFileName = sect.getValue("alias_file"),
				widCandidatesFileName = sect.getValue("wid_candidates_file");
		
		SimpleWikiDict dict = new SimpleWikiDict(aliasFileName, widCandidatesFileName);
		int[] wids = dict.getCandidates("michael jordan");
		if (wids == null)
			return ;
		for (int i = 0; i < wids.length; ++i) {
			System.out.println(wids[i]);
		}
	}

	// alias index file is generated by python program
	// TODO filter some aliases of the dictionary, e.g. too long aliases
	public static void genDict(IniFile config) {
		IniFile.Section sect = config.getSection("dict_gen_dict");
		if (sect == null)
			return;

		String srcFileName = sect.getValue("src_file"), dstAliasFile = sect
				.getValue("dst_alias_file"), dstMidFile = sect
				.getValue("dst_mid_file"), dstAliasIndexFile = sect
				.getValue("dst_alias_index_file");
		int numIndices = sect.getIntValue("num_indices");

		DictGen.genDict(srcFileName, dstAliasFile, dstMidFile, numIndices,
				dstAliasIndexFile);
	}

	public static void removeDupliate(IniFile config) {
		IniFile.Section sect = config.getSection("dict_remove_duplicates");
		if (sect == null)
			return;

		String srcFileName = sect.getValue("src_file"), dstFileName = sect
				.getValue("dst_file");

		// DictGen.removeDuplicates(srcFileName, dstFileName);
		TupleFileTools.removeDuplicates(srcFileName, dstFileName);
	}

	public static void mergeWikiFbMidAliasCntFiles(IniFile config) {
		IniFile.Section sect = config
				.getSection("dict_merge_mid_alias_cnt_files");
		String wikiMidAliasCntFileName = sect
				.getValue("wiki_mid_alias_cnt_file"), freebaseMidAliasFileName = sect
				.getValue("fb_mid_alias_file"), dstMidEachAliasCntFileName = sect
				.getValue("dst_mid_each_alias_cnt_file"), dstMidAliasCntFileName = sect
				.getValue("dst_mid_alias_cnt_file");
		DictGen.mergeMidAliasCntFiles(wikiMidAliasCntFileName,
				freebaseMidAliasFileName, dstMidEachAliasCntFileName,
				dstMidAliasCntFileName);
	}

	public static void genDictPse(IniFile config) {
		IniFile.Section sect = config.getSection("dict_gen_dict_pse");
		String midEachAliasCntFileName = sect.getValue("each_alias_cnt_file"), midAliasCntFileName = sect
				.getValue("alias_cnt_file"), dstAliasFile = sect
				.getValue("dst_alias_file"), dstMidFile = sect
				.getValue("dst_mid_file"), dstAliasIndexFile = sect
				.getValue("dst_alias_index_file");
		int numIndices = sect.getIntValue("num_indices");
		DictGen.genDictWithPse(midEachAliasCntFileName, midAliasCntFileName,
				dstAliasFile, dstMidFile, numIndices, dstAliasIndexFile);
	}

	public static void genSimpleWikiDict(IniFile config) {
		IniFile.Section sect = config.getSection("dict_gen_simple_wiki_dict");
		String midEachAliasCntFileName = sect.getValue("each_alias_cnt_file"), midToWidFileName = sect
				.getValue("mid_to_wid_file"), dstAliasListFileName = sect
				.getValue("dst_alias_file"), dstCandidatesFileName = sect
				.getValue("dst_candidates_file");
		DictGen.genSimpleWikiDict(midEachAliasCntFileName, midToWidFileName,
				dstAliasListFileName, dstCandidatesFileName);
	}

	public static void test() {
		System.out.println("DHL");
		DictGen.genAliasIndices("d:/data/el/tmp.txt", 13, 4,
				"d:/data/el/tmp0.txt");
	}

	public static void run(IniFile config) {
		// genFreebaseFullDumpName();
		// genFreebaseFullDumpAlias();
		// genFullFreebaseEnName();
		// genFullFreebaseEnAlias();
		// checkFileOrder();
		// checkMaxMidLen();
		// testAliasIndex();

		String job = config.getValue("main", "job");
		if (job.equals("dict_remove_duplicates")) {
			removeDupliate(config);
		} else if (job.equals("dict_gen_dict")) {
			genDict(config);
		} else if (job.equals("dict_test_dict")) {
			testDict(config);
		} else if (job.equals("dict_test")) {
			test();
		} else if (job.equals("dict_stat")) {
			DictStat.run(config);
		} else if (job.equals("dict_merge_mid_alias_cnt_files"))
			mergeWikiFbMidAliasCntFiles(config);
		else if (job.equals("dict_gen_dict_pse"))
			genDictPse(config);
		else if (job.equals("dict_gen_simple_wiki_dict"))
			genSimpleWikiDict(config);
		else if (job.equals("dict_test_simple_wiki_dict"))
			testSimpleWikiDict(config);
	}
}
