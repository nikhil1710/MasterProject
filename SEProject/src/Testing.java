import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class Testing{

	@SuppressWarnings("deprecation")
	public static String readXMLFile() throws IOException
	{
		/*
		 * readXMLFile - reads the UML file and returns the content
		 * as a string.
		 *
		 * */
		File file = new File("./src/UMLInput/model.uml");
		String content = "";
		if (file.isFile() && file.getName().endsWith(".uml")) {
			content = FileUtils.readFileToString(file);
		}
		return content;
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		// Read File
		File inputFile = new File("./src/UMLInput/model.uml");

		// Create Document Object from XML file
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();

		// Extract the list of elements based on their tag-names
		NodeList lifelineList = doc.getElementsByTagName("lifeline");
		NodeList fragmentList = doc.getElementsByTagName("fragment");
		NodeList messageList = doc.getElementsByTagName("message");

		/*
		 * Initialize various HashMaps for all the XML tags
		 */
		//{ Key, {Key, Value}} = {MsgId, {Send/Receive, LifeLineId}}
		HashMap<String, HashMap<String, String>> fragmentMap =
				new HashMap<String, HashMap<String, String>>();
		//{ Key, Value}} = {MsgId, MsgName}
		HashMap<String, String> messageMap = new HashMap<String, String>();
		//{ Key, Value}} = {ClassId, ClassName}
		HashMap<String, String> lifeLineMap = new HashMap<String, String>();

		/*
		 * Loop over Fragment List to get all relevant information
		 * into fragmentMap
		 */
		for(int i = 0; i < fragmentList.getLength(); i++){
			Node nNode = fragmentList.item(i);
			if(nNode.getNodeType() == Node.ELEMENT_NODE){
				Element eElement = (Element) nNode;
				NamedNodeMap nnm = eElement.getAttributes();
				for(int j = 0; j < nnm.getLength(); j++){
					if(nnm.item(j).getNodeName() == "message"){
						String messageId = nnm.getNamedItem("message").getTextContent();
						String name = nnm.getNamedItem("name").getTextContent();
						String classId = nnm.getNamedItem("covered").getTextContent();
						if(!fragmentMap.containsKey(messageId)){
							//create inner HashMap, with key = send/receive and value = classId
							HashMap<String, String> innerMap = new HashMap<String, String>();
							innerMap.put(name, classId);
							fragmentMap.put(messageId, innerMap);
						}
						else{
							HashMap<String, String> containedHashMap =
									fragmentMap.get(messageId);
							containedHashMap.put(name, classId);
						}
					}
				}
			}
		}

		/*
		 * Loop over Message List to get all relevant information
		 * into messagetMap
		 */
		for(int i = 0; i < messageList.getLength(); i++){
			Node nNode = messageList.item(i);
			if(nNode.getNodeType() == Node.ELEMENT_NODE){
				Element eElement = (Element) nNode;
				NamedNodeMap nnm = eElement.getAttributes();
				for(int j = 0; j < nnm.getLength(); j++){
					messageMap.put(nnm.getNamedItem("xmi:id").getTextContent(),
							nnm.getNamedItem("name").getTextContent());
				}
			}
		}

		/*
		 * Loop over Life line(class) List to get all relevant information
		 * into lifeLineMap
		 */
		for(int i = 0; i < lifelineList.getLength(); i++){
			Node nNode = lifelineList.item(i);
			if(nNode.getNodeType() == Node.ELEMENT_NODE){
				Element eElement = (Element) nNode;
				NamedNodeMap nnm = eElement.getAttributes();
				for(int j = 0; j < nnm.getLength(); j++){
					lifeLineMap.put(nnm.getNamedItem("xmi:id").getTextContent(),
							nnm.getNamedItem("name").getTextContent());
				}
			}
		}
		System.out.println(fragmentMap);
		System.out.println(messageMap);
		System.out.println(lifeLineMap);
	}
}