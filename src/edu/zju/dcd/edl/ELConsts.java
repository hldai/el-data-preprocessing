// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl;

public class ELConsts {
	public static int MID_BYTE_LEN = 8;
	public static int EID14_BYTE_LEN = 8;
	public static int QUERY_ID_BYTE_LEN = 14;

	public static int MID_WITH_PSE_BYTE_LEN = MID_BYTE_LEN + Float.BYTES * 2;
	
	public static String TMP_FILE_PATH = "e:/el/tmp_files";
//	public static String TMP_FILE_PATH = "/home/dhl/data/tmp_files";
	
	public static String NUM_LINES_FILE_SUFFIX = "_nl";
	public static String NIL = "NIL";
	
	public static int MAX_ALIAS_LEN = 97;

	public static final String QUERY_PATTERN = "\\s*?<query\\sid=\\\"(.*?)\\\">\\s*"
					+ "<name>(.*?)</name>\\s*" + "<docid>(.*?)</docid>\\s*"
					+ "<beg>(.*?)</beg>\\s*" + "<end>(.*?)</end>\\s*"
					+ "</query>\\s*";

	public static final String QUERY_PATTERN_WITHOUT_POS = "\\s*?<query\\sid=\\\"(.*?)\\\">\\s*"
					+ "<name>(.*?)</name>\\s*"
					+ "<docid>(.*?)</docid>\\s*</query>\\s*";
}
