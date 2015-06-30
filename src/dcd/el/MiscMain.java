// author: DHL brnpoem@gmail.com

package dcd.el;

import java.util.Comparator;

import dcd.config.IniFile;
import dcd.el.io.IOUtils;
import dcd.el.io.PairListFile;
import dcd.el.tools.MiscTools;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TextToBinary;
import dcd.el.utils.TupleFileTools;

public class MiscMain {

	public static void searchFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_search_file");
		if (sect == null)
			return;

		String fileName = sect.getValue("file");
		String str = sect.getValue("str");
		MiscTools.searchFile(fileName, str);
	}

	public static void searchItemFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_search_item");
		if (sect == null)
			return;

		String fileName = sect.getValue("file");
		String str = sect.getValue("str");
		MiscTools.searchItem(fileName, str);
	}

	public static void swapTupleFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_swap");
		if (sect == null)
			return;

		String fileName = sect.getValue("file"), dstFileName = sect
				.getValue("dst_file");
		int idx0 = sect.getIntValue("idx0"), idx1 = sect.getIntValue("idx1");
		TupleFileTools.swap(fileName, idx0, idx1, dstFileName);
//		PairListFile.swap(fileName, dstFileName);
	}

	public static void countLinesInFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_count_lines");
		if (sect == null)
			return;

		String fileName = sect.getValue("file");
		IOUtils.countLinesInFile(fileName);
	}

	public static void toLowerCase(IniFile config) {
		IniFile.Section sect = config.getSection("misc_to_lower_case");
		if (sect == null)
			return;

		String fileName = sect.getValue("file"), dstFileName = sect
				.getValue("dst_file");

		PairListFile.toLowerCase(fileName, dstFileName);
	}

	public static void checkFileOrder(IniFile config) {
		IniFile.Section sect = config.getSection("misc_check_order");
		if (sect == null)
			return;

		String fileName = sect.getValue("file");
		int[] indices = sect.getIntArrayValue("indices");

		Comparator<String> comparator = new TupleFileTools.MultiStringFieldComparator(indices);
		TupleFileTools.checkTupleFileOrder(fileName, comparator);
	}

	public static void checkPairFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_check_pair_file");
		if (sect == null)
			return;

		String fileName = sect.getValue("file");
		PairListFile.check(fileName);
	}

	public static void stringDoubleFileToBinary(IniFile config) {
		IniFile.Section sect = config.getSection("misc_str_double_to_bin");
		String srcFileName = sect.getValue("src_file"), dstFileName = sect
				.getValue("dst_file");

		TextToBinary.stringDoubleToBinary(srcFileName, dstFileName);
	}

	public static void midEidFileToBinary(IniFile config) {
		IniFile.Section sect = config.getSection("misc_mid_eid_to_bin");
		String fileName = sect.getValue("file"), dstFileName = sect
				.getValue("dst_file");

		TextToBinary.stringStringToBinary(fileName, ELConsts.MID_BYTE_LEN,
				ELConsts.EID14_BYTE_LEN, dstFileName);
	}

	public static void tupleFileJoin(IniFile config) {
		IniFile.Section sect = config.getSection("misc_tuple_file_join");

		String fileName0 = sect.getValue("file0"), fileName1 = sect
				.getValue("file1"), dstFileName = sect.getValue("dst_file");
		int idxCmp0 = sect.getIntValue("idx_cmp0"), idxCmp1 = sect
				.getIntValue("idx_cmp1");
		// boolean filterUnMatched = sect.getIntValue("filter_unmatched") != 0;

		TupleFileTools.join(fileName0, fileName1, idxCmp0, idxCmp1,
				dstFileName, null);
	}

	public static void joinIntFieldTupleFile(IniFile config) {
		IniFile.Section sect = config.getSection("misc_join_int_field_tuple_file");

		String fileName0 = sect.getValue("file0"), fileName1 = sect
				.getValue("file1"), dstFileName = sect.getValue("dst_file");
		int idxCmp0 = sect.getIntValue("idx_cmp0"), idxCmp1 = sect
				.getIntValue("idx_cmp1");

		TupleFileTools.join(fileName0, fileName1, idxCmp0, idxCmp1,
				dstFileName, new CommonUtils.StringToIntComparator());
	}

	public static void tupleFileSort(IniFile config) {
		IniFile.Section sect = config.getSection("misc_tuple_file_sort");

		String srcFileName = sect.getValue("src_file");
		String dstFileName = sect.getValue("dst_file");

		int[] sortIndices = sect.getIntArrayValue("sort_indices");
		if (sortIndices.length == 1) {
			TupleFileTools.sort(srcFileName, dstFileName,
					new TupleFileTools.SingleFieldComparator(sortIndices[0]));
		} else {
			TupleFileTools.sort(srcFileName, dstFileName,
					new TupleFileTools.MultiStringFieldComparator(sortIndices));
		}
	}

	public static void tupleFileMerge(IniFile config) {
		IniFile.Section sect = config.getSection("misc_tuple_file_merge");

		String[] srcFileNames = sect.getArrayValue("src_files");
		String dstFileName = sect.getValue("dst_file");
		TupleFileTools.merge(srcFileNames, dstFileName);
	}

	public static void sortIntFieldTupleFile(IniFile config) {
		IniFile.Section sect = config
				.getSection("misc_sort_int_field_tuple_file");
		String fileName = sect.getValue("file"), dstFileName = sect
				.getValue("dst_file");
		int sortIdx = sect.getIntValue("sort_idx");
		TupleFileTools.sort(fileName, dstFileName,
				new TupleFileTools.SingleFieldComparator(sortIdx,
						new CommonUtils.StringToIntComparator()));
	}
	
	private static void widToMid(IniFile config) {
		IniFile.Section sect = config
				.getSection("misc_wid_to_mid");
		String fileName = sect.getValue("file"),
				dstFileName = sect.getValue("dst_file"),
				widToMidFileName = sect.getValue("mid_wid_file");
		MiscTools.widToMidInTupleFile(fileName, widToMidFileName, dstFileName);
	}

	public static void test() {

	}

	public static void run(IniFile config) {
		String job = config.getValue("main", "job");
		if (job.equals("misc_swap"))
			swapTupleFile(config);
		else if (job.equals("misc_search_file"))
			searchFile(config);
		else if (job.equals("misc_search_item"))
			searchItemFile(config);
		else if (job.equals("misc_count_lines"))
			countLinesInFile(config);
		else if (job.equals("misc_to_lower_case"))
			toLowerCase(config);
		else if (job.equals("misc_check_order"))
			checkFileOrder(config);
		else if (job.equals("misc_check_pair_file"))
			checkPairFile(config);
		else if (job.equals("misc_test"))
			test();
		else if (job.equals("misc_str_double_to_bin"))
			stringDoubleFileToBinary(config);
		else if (job.equals("misc_mid_eid_to_bin"))
			midEidFileToBinary(config);
		else if (job.equals("misc_tuple_file_join"))
			tupleFileJoin(config);
		else if (job.equals("misc_tuple_file_sort"))
			tupleFileSort(config);
		else if (job.equals("misc_tuple_file_merge"))
			tupleFileMerge(config);
		else if (job.equals("misc_sort_int_field_tuple_file"))
			sortIntFieldTupleFile(config);
		else if (job.equals("misc_join_int_field_tuple_file"))
			joinIntFieldTupleFile(config);
		else if (job.equals("misc_wid_to_mid"))
			widToMid(config);
	}
}
