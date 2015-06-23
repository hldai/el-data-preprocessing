// author: DHL brnpoem@gmail.com

package dcd.el.tac;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dcd.el.io.IOUtils;
import dcd.el.tac.Query;

// TODO
public class QueryReader {
	public static final String QUERY_HEAD_PREFIX = "  <query id=";
	public static final String QUERY_END = "  </query>";
	public static final String NAME_PREFIX = "<name>";
	public static final String NAME_SUFFIX = "</name>";
	
	public void initWithQueryFile(String fileName) {
		reader = IOUtils.getUTF8BufReader(fileName);
		
		queryPattern = Pattern.compile("\\s*?<query\\sid=\\\"(.*?)\\\">\\s*"
				+ "<name>(.*?)</name>\\s*"
				+ "<docid>(.*?)</docid>\\s*"
				+ "<beg>(.*?)</beg>\\s*"
				+ "<end>(.*?)</end>\\s*"
				+ "</query>\\s*");
		
		try {
			// skip three lines not used
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(QUERY_HEAD_PREFIX))
					break;
			}
			queryStringBuilder.append(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Query nextQuery() {
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				queryStringBuilder.append(line);
				if (line.equals(QUERY_END))
					break;
			}
			
			if (line == null)
				return null;

			Matcher m = queryPattern.matcher(queryStringBuilder);
			if (m.find()) {
				Query q = new Query();
				q.queryId = m.group(1);
				q.name = m.group(2);
				q.docId = m.group(3);
				q.begPos = Integer.valueOf(m.group(4));
				q.endPos = Integer.valueOf(m.group(5));
				
				queryStringBuilder = new StringBuilder();
				return q;
			}
//			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void close() {
		try {
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BufferedReader reader = null;
	private Pattern queryPattern = null;
	private StringBuilder queryStringBuilder = new StringBuilder();
}
