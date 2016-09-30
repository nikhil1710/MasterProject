import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

class Testing{
	public static int PRETTY_PRINT_INDENT_FACTOR = 4;
	
	@SuppressWarnings("deprecation")
	public static String readFile() throws IOException 
	{
		File folder = new File("C:/Users/Nikhil Reddy/Desktop");
		File[] listOfFiles = folder.listFiles();

		String content = "";
		for (int i = 0; i < listOfFiles.length; i++) {
		  File file = listOfFiles[i];
		  if (file.isFile() && file.getName().endsWith(".xmi")) {
			  content = FileUtils.readFileToString(file);
		  }
		}
		return content;
	}

	public static void main(String[] args) throws IOException {
		try {
			String xml_string = Testing.readFile();
			JSONObject xmlJSONObj = XML.toJSONObject(xml_string);
			String jsonPrettyPrintString = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
			try (PrintWriter writer = new PrintWriter("C:/Users/Nikhil Reddy/Desktop/result.json", "UTF-8")) {
				writer.write(jsonPrettyPrintString);
				System.out.println("Successfully Copied JSON Object to File...");
			}
		} catch (JSONException je) {
			System.out.println("Exception....");
		}
	}
}