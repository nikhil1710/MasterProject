import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JWhileLoop;

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
	static LinkedList<LinkedHashMap<String, String>> final_scenarios = new LinkedList<LinkedHashMap<String, String>>();
	static LinkedHashMap<String, LinkedList<String>> loopHashMap = new LinkedHashMap<String, LinkedList<String>>();

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
						String operator = nnm.getNamedItem("interactionOperator").getTextContent();
						NodeList operandList = nNode.getChildNodes();
						for(int k = 0; k < operandList.getLength(); k++){
							if(operandList.item(k).getNodeName() == "operand"){
								String guard = "";
								NodeList operandFragments = operandList.item(k).getChildNodes();
								for(int x = 0; x < operandFragments.getLength(); x++){
									if(operandFragments.item(x).getNodeName() == "guard"){
										Node operandGuard = operandFragments.item(x);
										NodeList guardList = operandFragments.item(x).getChildNodes();
										if(operandGuard.getNodeType() == Node.ELEMENT_NODE){
											Element guardElement = (Element) operandGuard;
											NamedNodeMap guardMap = guardElement.getAttributes();
											for(int y = 0; y < guardMap.getLength(); y++){
												if(guardMap.item(y).getNodeName() == "name"){
													guard = guardMap.getNamedItem("name").getTextContent();
												}
											}
											if(!guard.equals("")){
												if(operator.equals("alt")){
													for(int u = 0; u < guardList.getLength(); u++){
														if(guardList.item(u).getNodeName() == "ownedComment"){
															NodeList bodyList = guardList.item(u).getChildNodes();
															for(int v = 0; v < bodyList.getLength(); v++){
																if(bodyList.item(v).getFirstChild() != null){
																	if(bodyList.item(v).getFirstChild().getTextContent().equals("Break")){
																		guard += "#" + "break";
																	}
																}
															}
														}
													}
												}
												else if(operator.equals("loop")){
													guard += "#" + "0";
													String maxint = "1";
													for(int u = 0; u < guardList.getLength(); u++){
														if(guardList.item(u).getNodeName() == "maxint"){
															Node maxInt = guardList.item(u);
															if(maxInt.getNodeType() == Node.ELEMENT_NODE){
																Element maxIntEle = (Element) maxInt;
																NamedNodeMap maxIntMap = maxIntEle.getAttributes();
																for(int y = 0; y < maxIntMap.getLength(); y++){
																	if(maxIntMap.item(y).getNodeName() == "value"){
																		maxint = maxIntMap.getNamedItem("value").getTextContent();
																	}
																}
															}
														}
													}
													guard += "#" + maxint;
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

	private static void addMsgToScenarios(String messageId, String send, String recv){
		LinkedHashMap<String, String> innerHashMap = null;
		if(scenarios.size() == 0){
			innerHashMap = new LinkedHashMap<String, String>();
			innerHashMap.put(messageId, send + "#" + recv);
			scenarios.add(innerHashMap);
			//System.out.println("In if.. " + scenarios.size());
		}
		else{
			//System.out.println("In else " + scenarios.size());
			for(int i = 0; i < scenarios.size(); i++){
				innerHashMap = scenarios.get(i);
				String previous_send = null;
				for(String message : innerHashMap.keySet()){
					previous_send = innerHashMap.get(message).split("#")[1];
				}
				if(previous_send.equals(send)){
					innerHashMap.put(messageId, send + "#" + recv);
				}
			}
		}
	}

	private static void addAltMsgToScenarios(LinkedHashMap<String, LinkedList<String>> altMap){
		//LinkedList<LinkedHashMap<String, String>> temp = new LinkedList<LinkedHashMap<String, String>>(scenarios);
		LinkedList<LinkedHashMap<String, String>> temp = new LinkedList<LinkedHashMap<String, String>>();
		temp=DeepClone.deepClone(scenarios);
		scenarios.clear();
		LinkedList<LinkedHashMap<String, String>> altTemp = new LinkedList<LinkedHashMap<String, String>>();
		for(String altKey : altMap.keySet()){
			altTemp = DeepClone.deepClone(temp);
			LinkedList<String> altMessages = altMap.get(altKey);
			boolean isBreak = (altKey.split("#").length == 2)? true : false;
			for(int j = 0; j < altMessages.size(); j++){
				String messageSplit[] = altMessages.get(j).split("#");
				String messageId = messageSplit[0];
				String send = messageSplit[1];
				String recv = messageSplit[2];

				for(int k = 0; k < altTemp.size(); k++){
					LinkedHashMap<String, String> innerHashMap = null;
					if(altTemp.size() == 0){
						innerHashMap = new LinkedHashMap<String, String>();
						innerHashMap.put(messageId, send + "#" + recv);
						altTemp.add(innerHashMap);
					}
					else{
						for(int i = 0; i < altTemp.size(); i++){
							innerHashMap = altTemp.get(i);
							String previous_send = null;
							for(String message : innerHashMap.keySet()){
								previous_send = innerHashMap.get(message).split("#")[1];
							}
							if(previous_send.equals(send)){
								innerHashMap.put(messageId, send + "#" + recv);
							}
						}
					}
				}
			}

			if(isBreak){
				for(int s = 0; s < altTemp.size(); s++){
					LinkedHashMap<String, String> innerHashMap = altTemp.get(s);
					final_scenarios.add(innerHashMap);
				}
			}
			else{
				for(int s = 0; s < altTemp.size(); s++){
					LinkedHashMap<String, String> innerHashMap = altTemp.get(s);
					scenarios.add(innerHashMap);
				}
			}
			altTemp.clear();
		}
	}

	public static void generateScenarios(){
		/*
		 * Construct Scenarios
		 */

		for(int i = 0; i < fragmentMap.size(); i++){
			boolean end=false;
			String message = (String) fragmentMap.keySet().toArray()[i];
			String[] message_list = message.split("#");
			if(message_list.length == 1){
				/*
				 * Regular messages (not in alt box or loops)
				 */
				String messageId = message_list[0];
				String send_class = fragmentMap.get(messageId).get("Send");
				String recv_class = fragmentMap.get(messageId).get("Recv");
				addMsgToScenarios(messageId, send_class, recv_class);
			}
			else if(message_list.length == 2 || message_list.length == 3){
				/*
				 * Messages which are present in alt box
				 */
				LinkedHashMap<String, LinkedList<String>> altMap =
						new LinkedHashMap<String, LinkedList<String>>();
				String messageAlt = (String) fragmentMap.keySet().toArray()[i];
				String[] messageAlt_list = message.split("#");
				while(messageAlt_list.length == 2 || messageAlt_list.length == 3){
					String messageId = messageAlt_list[0];
					String altStmt = messageAlt_list[1];
					if(messageAlt_list.length == 3)
						altStmt += "#" + messageAlt_list[2];
					String send_class = fragmentMap.get(messageAlt).get("Send");
					String recv_class = fragmentMap.get(messageAlt).get("Recv");

					if(altMap.containsKey(altStmt)){
						LinkedList<String> alt_messages = altMap.get(altStmt);
						alt_messages.add(messageId + "#" + send_class + "#" + recv_class);
						altMap.put(altStmt, alt_messages);
					}
					else{
						LinkedList<String> alt_messages = new LinkedList<String>();
						alt_messages.add(messageId + "#" + send_class + "#" + recv_class);
						altMap.put(altStmt, alt_messages);
					}
					i++;
					if(i>=fragmentMap.size()){
						end=true;
						break;
					}
					else{
						messageAlt = (String) fragmentMap.keySet().toArray()[i];
						messageAlt_list = messageAlt.split("#");
					}
				}
				addAltMsgToScenarios(altMap);
				if(end)
					break;
				i--;
			}
			else{
				/*
				 * Messages which are present in loops
				 */
				String messageId = message_list[0];
				String send_class = fragmentMap.get(message).get("Send");
				String recv_class = fragmentMap.get(message).get("Recv");
				addMsgToScenarios(messageId, send_class, recv_class);

				// Insert loop messages into loop HashMap
				String key = message_list[1] + "#" + message_list[2] + "#" + message_list[3];
				LinkedList<String> values = null;
				if(loopHashMap.containsKey(key)){
					values = loopHashMap.get(key);
					values.add(messageId);
				}
				else{
					values = new LinkedList<String>();
					values.add(messageId);
				}
				loopHashMap.put(key, values);
			}
		}
		for(int s = 0; s < scenarios.size(); s++){
			LinkedHashMap<String, String> innerHashMap = scenarios.get(s);
			final_scenarios.add(innerHashMap);
		}
	}

	private static void generateTestSuite() throws JClassAlreadyExistsException, IOException {
		String seq="";

		LinkedHashMap<String,String> testMap=new LinkedHashMap<String, String>();

		for(int i=0;i<final_scenarios.size();i++){
			HashMap<String, String> sequences = final_scenarios.get(i);
			//System.out.println(sequences);
			seq+="Scenario"+i+":";
			Iterator<Entry<String, String>> it=sequences.entrySet().iterator();

			while(it.hasNext()){
				Map.Entry<String, String> keyVal=(Map.Entry<String, String>) it.next();
				String messageFunction = (String) keyVal.getKey();
				String source_destination = (String) keyVal.getValue();

				LinkedList<String> messages=null;
				String loop=null;

				for(String loop_name: loopHashMap.keySet()){
					messages=loopHashMap.get(loop_name);

					if(messages.contains(messageFunction)){
						loop=loop_name;
						break;
					}
				}

				if(loop==null){
					String message=messageMap.get(messageFunction);
					String[] s_d=source_destination.split("#");

					String source= lifeLineMap.get(s_d[0]);
					String destination=lifeLineMap.get(s_d[1]);
					seq+=message+"@"+source+"@"+destination+",";
				}
				else{
					String[] loopContents=loop.split("#");
					for(String msg : messages){
						String message=messageMap.get(msg);
						source_destination = sequences.get(msg);
						String[] s_d=source_destination.split("#");

						String source= lifeLineMap.get(s_d[0]);
						String destination=lifeLineMap.get(s_d[1]);
						seq+=message+"@"+source+"@"+destination+"@"+loopContents[0]+"@"+ loopContents[1]+"@"+loopContents[2]
								+"@"+messages.size()+",";
					}
					for(int j=0;j<messages.size() - 1;j++){
						it.next();
					}
				}
			}
			//System.out.println(seq);
			seq+="#";
		}

		JCodeModel cm = new JCodeModel();

		String suites[]=seq.split("#");

		String classNames[] = null;
		for(int i=0;i<suites.length;i++){
			classNames=suites[i].split(":");
			for(int j=0;j<classNames.length;j++){
				//System.out.println(classNames[j]);
				testMap.put(classNames[0],classNames[1]);
			}

		}

		JDefinedClass[] dc=new JDefinedClass[testMap.size()];

		for(int s=0;s<testMap.size();s++){

			String cName=(String) testMap.keySet().toArray()[s];
			String[] methods=testMap.get(cName).split(",");

			dc[s]=cm._class("test."+cName);

			JMethod unitTestMethod=dc[s].method(1, void.class,"testMethods");
			unitTestMethod.annotate(cm.ref("Test"));

			for(int k=0;k<methods.length;k++){
				if(methods[k].split("@").length == 3){
					unitTestMethod.body().directStatement("//sends "+methods[k].split("@")[0]+" from class "+methods[k].split("@")[1]+" "+" to class "+methods[k].split("@")[2]+"");
					unitTestMethod.body().invoke(methods[k].split("@")[0]);
				}
				else{
					String mSplit[]= methods[k].split("@");
					unitTestMethod.body().directStatement("/* Next " + mSplit[mSplit.length - 1] +
							" messages loops for " +mSplit[mSplit.length - 2] + " times */");


					unitTestMethod.body().directStatement("int index=0;");
					JExpression test = JExpr.ref("index").lt(JExpr.lit(Integer.valueOf(mSplit[mSplit.length - 2])));	
					JWhileLoop whileLoop=unitTestMethod.body()._while(test);
					JBlock whileBody = whileLoop.body();

					for(int u = k; u <= Integer.parseInt(mSplit[mSplit.length - 1]); u++){


						whileBody.directStatement("//sends "+methods[k].split("@")[0]+""
								+ "from class "+methods[k].split("@")[1]+" "+"to class "+methods[k].split("@")[2]+"");
						whileBody.invoke(methods[k].split("@")[0]);



						k++;

					}
					whileBody.directStatement("index++;");

					k--;
				}
			}

		}
		File file = new File("./target/classes");
		file.mkdirs();
		cm.build(file);
	}

	private static void generateGraph() {
		// TODO Auto-generated method stub
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		Graph graph[] = new MultiGraph[final_scenarios.size()];
		for(int i=0;i<final_scenarios.size();i++){
			int number=i+1;
			graph[i]=new MultiGraph("final_scenarios"+number);
			graph[i].setStrict(false);

			HashMap<String, String> sequences = final_scenarios.get(i);
			//System.out.println(sequences);

			graph[i].addNode("Start");
			int count=1;
			HashMap<Integer,String> loopSet=new HashMap<Integer,String>();
			for (Map.Entry<String, String> val : sequences.entrySet()) {
				String messageFunction = val.getKey();
				String source_destination = val.getValue();

				for(String loop: loopHashMap.keySet()){
					LinkedList<String> loopList=loopHashMap.get(loop);
					if(loopList.contains(messageFunction)){
						loopSet.put(count,loop.split("#")[2]);
					}
				}

				String message=messageMap.get(messageFunction);
				String[] s_d=source_destination.split("#");
				//System.out.println(s_d[0]);

				String source= "Node"+" "+lifeLineMap.get(s_d[0]);
				String destination="Node"+" "+lifeLineMap.get(s_d[1]);

				//System.out.println("Message:"+message+"Source:"+source+"Destination:"+destination);
				graph[i].addNode(source);
				graph[i].addNode(destination);
				graph[i].addEdge(count+":"+message,source,destination);
				count++;

			}
			org.graphstream.graph.Node start = graph[i].getNode(1);
			graph[i].addEdge("0:Start", "Start",start.toString());


			graph[i].addAttribute("ui.stylesheet", 
					"node {stroke-mode: plain; fill-color: white;shape: rounded-box;size-mode: "
							+ "fit;text-size:15; padding: 4px, 4px; fill-mode:dyn-plain;}"
							+ "edge {text-size:15;fill-mode:dyn-plain;}");

			int edgeCount=0;
			for(Edge e:graph[i].getEachEdge()) {
				edgeCount++;
				
				if(loopSet.containsKey(edgeCount)){
					e.addAttribute("ui.color", Color.RED);
					
					e.addAttribute("ui.label", e.getId()+": LOOP("+loopSet.get(edgeCount)+")");
				}
				else{
					e.addAttribute("ui.label", e.getId());
					e.addAttribute("ui.stylesheet","edge {fill-color:red;}");
				}


			}
			for(org.graphstream.graph.Node n:graph[i]) {

				//System.out.println(n.getId());
				n.addAttribute("ui.style", "fill-color:rgba(255,0,0,128);");
				//	n.addAttribute("ui.style", "rounded-box");

				n.addAttribute("ui.label", n.getId());
			}

			//graph[i].display();		

			Viewer viewer = graph[i].display();	
			View view = viewer.getDefaultView();
			//	view.getCamera().setViewPercent(1;


		}
	}

	public static boolean deleteDirectory(File directory) {
		if(directory.exists()){
			File[] files = directory.listFiles();
			if(null!=files){
				for(int i=0; i<files.length; i++) {
					if(files[i].isDirectory()) {
						deleteDirectory(files[i]);
					}
					else {
						files[i].delete();
					}
				}
			}
		}
		return(directory.delete());
	}
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, JClassAlreadyExistsException {
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
		 * Generate Fragment, Message, LifeLine maps
		 */
		generateFragments(fragmentList);
		generateMessageMap(messageList);
		generateLifelineMap(lifelineList);


		//System.out.println(fragmentMap);
		//System.out.println(messageMap);
		//System.out.println(lifeLineMap);
		
		generateScenarios();
		
		System.out.println(loopHashMap);
		//System.out.println(final_scenarios);
		File file = new File("target");
		deleteDirectory(file);
		generateTestSuite();

		//Creates visual graph
		generateGraph();
	}
}