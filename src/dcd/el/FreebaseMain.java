package dcd.el;

import dcd.config.IniFile;
import dcd.el.tools.FreebaseTools;

public class FreebaseMain {
	private static void filterPersonLastName(IniFile config) {
		IniFile.Section sect = config.getSection("freebase_filter_person_last_name");
		String lastNameListFileName = sect.getValue("last_name_list_file"),
				dstFileName = sect.getValue("dst_file");
		FreebaseTools.filterPersonLastNameList(lastNameListFileName, dstFileName);
	}
	
	private static void searchDump(IniFile config) {
		IniFile.Section sect = config.getSection("freebase_search");
		String dumpFileName = sect.getValue("dump_file"),
				targetStr = sect.getValue("target_str"),
				dstFileName = sect.getValue("dst_file");
		FreebaseTools.searchFile(dumpFileName, targetStr, dstFileName);
	}
	
	private static void genPersonList(IniFile config) {
		IniFile.Section sect = config.getSection("freebase_gen_person_list");
		String dumpFileName = sect.getValue("dump_file"),
				dstFileName = sect.getValue("dst_file");
		FreebaseTools.genPersonMidList(dumpFileName, dstFileName);
	}
	
	private static void genPersonLastNameList(IniFile config) {
		IniFile.Section sect = config.getSection("freebase_gen_person_last_name");
		String nameListFileName = sect.getValue("name_list_file"),
				personListFileName = sect.getValue("person_list_file"),
				dstFileName = sect.getValue("dst_file");
		FreebaseTools.genPersonLastNameList(nameListFileName, personListFileName, dstFileName);
	}
	
	private static void genNotableFor(IniFile config) {
		IniFile.Section sect = config.getSection("freebase_gen_notable_for");
		String dumpFileName = sect.getValue("dump_file"),
				dstFileName = sect.getValue("dst_file");
		FreebaseTools.genNotableForAttributes(dumpFileName, dstFileName);
	}
	
	public static void run(IniFile config) {
		String job = config.getValue("main", "job");
		if (job.equals("freebase_search"))
			searchDump(config);
		else if (job.equals("freebase_gen_person_list"))
			genPersonList(config);
		else if (job.equals("freebase_gen_person_last_name"))
			genPersonLastNameList(config);
		else if (job.equals("freebase_filter_person_last_name"))
			filterPersonLastName(config);
		else if (job.equals("freebase_gen_notable_for"))
			genNotableFor(config);
	}
}
