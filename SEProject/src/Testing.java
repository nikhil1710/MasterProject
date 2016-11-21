import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class Testing{

	/*
	 * Initialize various HashMaps for all the XML tags
	 */
	//{ Key, {Key, Value}} = {MsgId, {Send/Receive, LifeLineId}}
	static LinkedHashMap<String, LinkedHashMap<String, String>> fragmentMap =
			new LinkedHashMap<String, LinkedHashMap<String, String>>();
	//{ Key, Value}} = {MsgId, MsgName}
	static LinkedHashMap<String, String> messageMap = new LinkedHashMap<String, String>();
	//{ Key, Value}} = {ClassId, ClassName}
	static LinkedHashMap<String, String> lifeLineMap = new LinkedHashMap<String, String>();
	static LinkedList<LinkedHashMap<String, String>> scenarios = new LinkedList<LinkedHashMap<String, String>>();

	public static void generateFragments(NodeList fragmentList){
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
					if(nnm.item(j).getNodeName() == "interactionOperator"){
						NodeList operandList = nNode.getChildNodes();
						for(int k = 0; k < operandList.getLength(); k++){
							if(operandList.item(k).getNodeName() == "operand"){
								String guard = "";
								NodeList operandFragments = operandList.item(k).getChildNodes();
								for(int x = 0; x < operandFragments.getLength(); x++){
									if(operandFragments.item(x).getNodeName() == "guard"){
										Node operandGuard = operandFragments.item(x);
										if(operandGuard.getNodeType() == Node.ELEMENT_NODE){
											Element guardElement = (Element) operandGuard;
											NamedNodeMap guardMap = guardElement.getAttributes();
											for(int y = 0; y < guardMap.getLength(); y++){
												if(guardMap.item(y).getNodeName() == "name"){
													guard = guardMap.getNamedItem("name").getTextContent();
												}
											}
										}
									}
								}
								for(int x = 0; x < operandFragments.getLength(); x++){
									if(operandFragments.item(x).getNodeName() == "fragment"){
										Node operandFragment = operandFragments.item(x);
										if(operandFragment.getNodeType() == Node.ELEMENT_NODE){
											Element ofElement = (Element) operandFragment;
											NamedNodeMap ofnnm = ofElement.getAttributes();
											for(int y = 0; y < ofnnm.getLength(); y++){
												if(ofnnm.item(y).getNodeName() == "message"){
													String opFragMessageId = ofnnm.getNamedItem("message").getTextContent() + "#" +guard;
													String opFragName = "";
													if(ofnnm.getNamedItem("name").getTextContent().contains("Send"))
														opFragName = "Send";
													else
														opFragName = "Recv";
													String opFragClassId = ofnnm.getNamedItem("covered").getTextContent();
													if(!fragmentMap.containsKey(opFragMessageId)){
														//create inner HashMap, with key = send/receive and value = classId
														LinkedHashMap<String, String> operatorInnerMap = new LinkedHashMap<String, String>();
														operatorInnerMap.put(opFragName, opFragClassId);
														fragmentMap.put(opFragMessageId, operatorInnerMap);
													}
													else{
														LinkedHashMap<String, String> operatorContainedHashMap =
																fragmentMap.get(opFragMessageId);
														operatorContainedHashMap.put(opFragName, opFragClassId);
														fragmentMap.put(opFragMessageId, operatorContainedHashMap);
													}
												}
											}
										}
										i++;
									}
								}
							}
						}
					}
					if(nnm.item(j).getNodeName() == "message"){
						String messageId = nnm.getNamedItem("message").getTextContent();
						String name = "";
						if(nnm.getNamedItem("name").getTextContent().contains("Send"))
							name = "Send";
						else
							name = "Recv";
						String classId = nnm.getNamedItem("covered").getTextContent();
						if(!fragmentMap.containsKey(messageId)){
							//create inner HashMap, with key = send/receive and value = classId
							LinkedHashMap<String, String> innerMap = new LinkedHashMap<String, String>();
							innerMap.put(name, classId);
							fragmentMap.put(messageId, innerMap);
						}
						else{
							LinkedHashMap<String, String> containedHashMap =
									fragmentMap.get(messageId);
							containedHashMap.put(name, classId);
							fragmentMap.put(messageId, containedHashMap);
						}
					}
				}
			}
		}
	}

	public static void generateMessageMap(NodeList messageList){
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
	}

	public static void generateLifelineMap(NodeList lifelineList){
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
	}

	public static void generateScenarios(){
		/*
		 * Construct Scenarios
		 */

		ArrayList<String> visited = new ArrayList<String>();

		boolean finish = false;
		Map.Entry<String,LinkedHashMap<String, String>> entry=fragmentMap.entrySet().iterator().next();
		
		String messageId = entry.getKey();
		LinkedHashMap<String, String> value = entry.getValue();
				
		visited.add(messageId);
		while(!finish){
			String send_class = value.get("Send");
			String recv_class = value.get("Recv");
			LinkedHashMap<String, String> innerHashMap = new LinkedHashMap<String, String>();
			innerHashMap.put(messageId, send_class + "$" + recv_class);
			for(int i = 1; i < fragmentMap.size(); i++){
				String current = (String) fragmentMap.keySet().toArray()[i];
				
				String curr_send = fragmentMap.get(current).get("Send");
				String curr_recv = fragmentMap.get(current).get("Recv");
				
				if(!visited.contains(current)){
					if(recv_class.equals(curr_send)){
						innerHashMap.put(current, curr_send + "$" + curr_recv);
						visited.add(current);
						send_class = curr_send;
						recv_class = curr_recv;
					}
					else{
						break;
					}
				}
			}
			scenarios.add(innerHashMap);
			if(visited.size() == fragmentMap.size())
				finish = true;
		}
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		// Read File
		File inputFile = new File("./src/UMLInput/model2.uml");

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
		 * Generate Fragment, Message, LifeLine maps
		 */
		generateFragments(fragmentList);
		generateMessageMap(messageList);
		generateLifelineMap(lifelineList);

		
		System.out.println(fragmentMap);
		System.out.println(messageMap);
		System.out.println(lifeLineMap);

		//generateScenarios();
		//System.out.println(scenarios);
	}
}