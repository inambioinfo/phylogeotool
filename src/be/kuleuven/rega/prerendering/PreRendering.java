package be.kuleuven.rega.prerendering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jebl.evolution.trees.SimpleRootedTree;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.kuleuven.rega.phylogeotool.core.Cluster;
import be.kuleuven.rega.phylogeotool.core.Edge;
import be.kuleuven.rega.phylogeotool.core.Node;
import be.kuleuven.rega.phylogeotool.core.Tree;
import be.kuleuven.rega.phylogeotool.data.csv.CsvUtils;
import be.kuleuven.rega.phylogeotool.io.read.ReadTree;
import be.kuleuven.rega.phylogeotool.settings.Settings;
import be.kuleuven.rega.phylogeotool.tools.ColorClusters;
import be.kuleuven.rega.phylogeotool.tree.distance.DistanceCalculateFromTree;
import be.kuleuven.rega.phylogeotool.tree.distance.DistanceInterface;
import be.kuleuven.rega.phylogeotool.tree.distance.DistanceMatrixDistance;

import com.opencsv.CSVReader;
import com.thoughtworks.xstream.XStream;

import figtree.application.GraphicFormat;

public class PreRendering {

	private XStream xStream = null;
	private static String basePath;
	public final static int CONTROL_PALETTE_WIDTH = 200;
	public static enum ID {
		LEAFID, NODEID
	}
	
	public PreRendering(String folderPhyloRenderLocation) {
		this.xStream = new XStream();
		this.xStream.alias("cluster", Cluster.class);
		this.xStream.alias("tree", Tree.class);
		this.xStream.alias("node", Node.class);
		this.xStream.alias("edge", Edge.class);
		this.xStream.omitField(Cluster.class, "tree");
		this.xStream.omitField(Cluster.class, "root");
		this.xStream.omitField(Cluster.class, "boundaries");
		setBasePath(folderPhyloRenderLocation);
	}
	
	private static void setBasePath(String basePath) {
		PreRendering.basePath = basePath;
	}
	
//	public void writeJeblTreeToXML(jebl.evolution.trees.Tree tree) {
//		String xml = xStream.toXML(tree);
//		FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter(new File(this.folderLocationTree + File.separator + "jebl.xml"));
//			fileWriter.write(xml);
//			fileWriter.close();
//		} catch (IOException e) {
//			System.err.println(PreRendering.class + " : Error with writing the tree jebl.xml to an xml file.");
//		}
//	}
	
//	public void writeTreeToXML(Tree tree) {
//		String xml = xStream.toXML(tree);
//		FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter(new File(this.folderLocationTree + File.separator + tree.getRootNode().getId() + ".xml"));
//			fileWriter.write(xml);
//			fileWriter.close();
//		} catch (IOException e) {
//			System.err.println(PreRendering.class + " : Error with writing the tree " + tree.getRootNode().getId() + " to an xml file.");
//		}
//	}
	
	public void writeClusterToXML(Cluster cluster) {
		String xml = xStream.toXML(cluster);
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(new File(Settings.getClusterPath(basePath) + File.separator + cluster.getRoot().getId() + ".xml"));
			fileWriter.write(xml);
			fileWriter.close();
		} catch (IOException e) {
			System.err.println(PreRendering.class + " : Error with writing the cluster " + cluster.getRoot().getId() + " to an xml file.");
		}
	}
	
	public Cluster getClusterFromXML(String clusterId) {
		Cluster cluster = null;
//			byte[] encoded = Files.readAllBytes(Paths.get(folderLocation + File.separator + clusterId + ".xml"));
//			newTree = (Tree)xStream.fromXML(new String(encoded));
		cluster = (Cluster)xStream.fromXML(new File(Settings.getClusterPath(basePath) + File.separator + clusterId + ".xml"));
//		Tree tree = getTreeFromXML("1");
		Tree tree = ReadTree.getTreeDrawTree();
		return new Cluster(tree, cluster.getRootId(), cluster.getParentalClusterRootId(), cluster.getBoundariesIds());
	}
	
	public Cluster getClusterFromXML(Tree tree, String clusterId) {
		Cluster cluster = null;
//			byte[] encoded = Files.readAllBytes(Paths.get(folderLocation + File.separator + clusterId + ".xml"));
//			newTree = (Tree)xStream.fromXML(new String(encoded));
		cluster = (Cluster)xStream.fromXML(new File(Settings.getClusterPath(basePath) + File.separator + clusterId + ".xml"));
		return new Cluster(tree, cluster.getRootId(), cluster.getParentalClusterRootId(), cluster.getBoundariesIds());
	}
	
	public void preRender(String treeLocation, String csvLocation, String distanceMatrixLocation, Path rBinary, Path rScripts) throws IOException {
		System.err.println("Reading JeblTree");
		jebl.evolution.trees.Tree jeblTree = ReadTree.readTree(new FileReader(treeLocation));
		System.err.println("JeblTree read");
		
		Tree tree = ReadTree.jeblToTreeDraw((SimpleRootedTree) jeblTree, new ArrayList<String>());
		System.err.println("JeblTree transformed to TreeDraw");
		int minimumClusterSize = 2;
		
		HashMap<String, Integer> translatedNodeNames = new HashMap<String, Integer>();
		DistanceInterface distanceInterface = null;
		boolean showNA = true;
		
		int index = 0;
		for (Node leaf : tree.getLeaves()) {
			translatedNodeNames.put(leaf.getLabel(), index++);
		}
		
		System.err.println("NodeList created");
		
		if(distanceMatrixLocation != null && !distanceMatrixLocation.equals("")) {
			System.err.println("Reading DistanceMatrix");
			distanceInterface = new DistanceMatrixDistance(translatedNodeNames, distanceMatrixLocation);
			System.err.println("DistanceMatrix Read");
		} else {
			distanceInterface = new DistanceCalculateFromTree();
		}
		
		LinkedList<Node> toDo = new LinkedList<Node>();
		toDo.add(tree.getRootNode());
		Node currentNode;
		
		System.err.println("Start calculations");
		
		/**
		 * We need to keep track of the connection between clusters. Which cluster is connected to which other cluster?
		 * To do this we create a hashmap that connects a root of a cluster (rootNode) to it's parent
		 */
		
		Map<Node, Cluster> clusterToParent = new HashMap<Node, Cluster>();
		
		while(toDo.peek() != null) {
			currentNode = toDo.pop();
//			currentNode = tree.getNodeById(1);
			// Do multi thread here
			Cluster cluster = BestClusterMultiThread.getBestCluster(rBinary, rScripts, Paths.get(basePath), minimumClusterSize, 50, 2, tree, currentNode, clusterToParent.get(currentNode), distanceInterface);
			if(cluster != null) {
				this.writeClusterToXML(cluster);
				System.err.println("Cluster " + cluster.getRootId() + " structure written to file");
				// Include NA, we can later on always decide not to show it. We can at least render it
				this.prepareCSV(cluster.getRoot().getId(), tree.getLeaves(cluster.getRoot()), null, csvLocation, true);
				System.err.println("XML details written to file");
//				NexusExporter.export(cluster, jeblTree, new FileWriter(new File(this.folderLocationTreeView + File.separator + cluster.getRoot().getId() + ".nexus")), minimumClusterSize, true);
				ColorClusters.prepareFullTreeView(jeblTree, null, cluster, GraphicFormat.PNG, new FileOutputStream(new File(Settings.getTreeviewPath(basePath) + File.separator + cluster.getRoot().getId() + ".png")), minimumClusterSize, true, false);
				System.err.println("Image written to file");
				
				List<Node> nodesList = cluster.getBoundaries();
				for(Node node:nodesList) {
					// Inner node
					if(node.getImmediateChildren().size() != 0) {
						toDo.add(tree.getNodeById(node.getId()));
						clusterToParent.put(node, cluster);
					// Leaf
					} else {
						// Include NA, we can later on always decide not to show it. We can at least render it
						this.prepareCSV(node.getId(), tree.getLeaves(node), null, csvLocation, true);
						this.writeClusterToXML(new Cluster(tree, node, cluster, new ArrayList<Node>()));
					}
				}
				// Case that the amount of nodes is too small to make a cluster
			} else {
				Cluster fakeCluster = new Cluster(tree, currentNode, clusterToParent.get(currentNode), tree.getLeaves(currentNode));
				this.writeClusterToXML(fakeCluster);
				// Include NA, we can later on always decide not to show it. We can at least render it
				this.prepareCSV(fakeCluster.getRoot().getId(), tree.getLeaves(fakeCluster.getRoot()), null, csvLocation, true);

				for(Node node:tree.getLeaves(currentNode)) {
					// Include NA, we can later on always decide not to show it. We can at least render it
					this.prepareCSV(node.getId(), tree.getLeaves(node), null, csvLocation, true);
					this.writeClusterToXML(new Cluster(tree, node, fakeCluster, new ArrayList<Node>()));
				}
			}
//			break;
//			toDo.addAll(tempTree.getLeaves());
		}
	}
	
	public static boolean checkFoldersEmpty(String folderPhyloRenderLocation) {
		File clusters = new File(folderPhyloRenderLocation + File.separator + "clusters");
		File xml = new File(folderPhyloRenderLocation + File.separator + "xml");
		File treeview = new File(folderPhyloRenderLocation + File.separator + "treeview");
		File r = new File(folderPhyloRenderLocation + File.separator + "r");
		
		DirectoryStream<Path> clustersStream = null;
		DirectoryStream<Path> xmlStream = null;
		DirectoryStream<Path> treeviewStream = null;
		DirectoryStream<Path> rStream = null;
		
		if(clusters.isDirectory() && xml.isDirectory() && treeview.isDirectory() && r.isDirectory()){
			try {
				clustersStream = Files.newDirectoryStream(clusters.toPath());
				xmlStream = Files.newDirectoryStream(xml.toPath());
				treeviewStream = Files.newDirectoryStream(treeview.toPath());
				rStream = Files.newDirectoryStream(r.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			boolean directoryEmpty = true;
			if(clustersStream.iterator().hasNext() || xmlStream.iterator().hasNext() || treeviewStream.iterator().hasNext() || rStream.iterator().hasNext()){
				directoryEmpty = false;
			} else {
				directoryEmpty = true;
			}
			try {
				clustersStream.close();
				xmlStream.close();
				treeviewStream.close();
				rStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return directoryEmpty;
		} else if(clusters.isFile()) {
			System.err.println(PreRendering.class + ": The path " + folderPhyloRenderLocation + File.separator + "clusters" + " to the folder seems to direct to a file.");
			return false;
		} else if(xml.isFile()) { 
			System.err.println(PreRendering.class + ": The path " + folderPhyloRenderLocation + File.separator + "xml" + " to the folder seems to direct to a file.");
			return false;
		} else if(treeview.isFile()) {
			System.err.println(PreRendering.class + ": The path " + folderPhyloRenderLocation + File.separator + "treeview" + " to the folder seems to direct to a file.");
			return false;
		} else if(r.isFile()) {
			System.err.println(PreRendering.class + ": The path " + folderPhyloRenderLocation + File.separator + "r" + " to the folder seems to direct to a file.");
			return false;
		} else {
			clusters.mkdirs();
			xml.mkdirs();
			treeview.mkdirs();
			r.mkdirs();
			return true;
		}
	}
	
	public void prepareCSV(int clusterId, List<Node> nodes, OutputStreamWriter outputStreamWriter, String csvLocation, boolean showNA) {
		CSVReader csvReader;
		
		List<String> ids = new ArrayList<String>();
		for(Node node:nodes) {
			ids.add(node.getLabel());
		}
		try {
			if(outputStreamWriter == null) {
				outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(Settings.getXmlPath(basePath) + File.separator + clusterId + ".xml")));
			}
			csvReader = new CSVReader(new FileReader(new File(csvLocation)), ';');
			String[] header = csvReader.readNext();
//			FileWriter fileWriter = new FileWriter(new File(Settings.getXmlPath(basePath) + File.separator + clusterId + ".xml"));
			outputStreamWriter.write("<xml>" + "\n");
			for(String key:header) {
				if(!key.equalsIgnoreCase("id")) {
					HashMap<String,Integer> tempHashMap = CsvUtils.csvToHashMapStringInteger(new File(csvLocation), ';', ids, key, showNA);
					for(String hashMapKey:tempHashMap.keySet()) {
						outputStreamWriter.write("\t<FIELD name=\"" + key + "\"" + " id=\"" + hashMapKey + "\">" + tempHashMap.get(hashMapKey) + "</FIELD>" + "\n");
					}
				}
			}
			outputStreamWriter.write("</xml>");
			outputStreamWriter.close();
		} catch (FileNotFoundException e) {
			System.err.println(PreRendering.class + ": " + "PrepareCSV, File could not be found.");
		} catch (IOException e) {
			System.err.println(PreRendering.class + ": " + "PrepareCSV, IOException in the CSVReader.");
		}
	}
	
//	public void prepareFullTreeView(int clusterId, Cluster cluster, jebl.evolution.trees.Tree jeblTree) {
//		
//		StringWriter stringWriter = null;
//		try {
////			FileWriter fileWriter = new FileWriter(new File(this.folderLocationTreeView + File.separator + clusterId + ".nexus"));
//			stringWriter = new StringWriter();
//			String export = NexusExporter.export(cluster, jeblTree, stringWriter);
////			fileWriter.write(stringWriter.toString());
////			fileWriter.close();
////			System.out.println(export);
//			
//			Map<String, Object> settings = new HashMap<String, Object>();
//	
//	        ExtendedTreeViewer treeViewer = new ExtendedTreeViewer();
//	        ControlPalette controlPalette = new BasicControlPalette(CONTROL_PALETTE_WIDTH, BasicControlPalette.DisplayMode.ONLY_ONE_OPEN);
//	        // This is important for the coloring.
//	        new FigTreePanel(null, treeViewer, controlPalette);
//	        // First of all, fully populate the settings map so that
//	        // all the settings have defaults
//	        controlPalette.getSettings(settings);
//	
//	        List<jebl.evolution.trees.Tree> trees = new ArrayList<jebl.evolution.trees.Tree>();
//	
//	        FigTreeNexusImporter importer = new FigTreeNexusImporter(new StringReader(export));
//	        trees.add(importer.importNextTree());
//	
//	            // Try to find a figtree block and if found, parse the settings
//	        while (true) {
//	        	try {
//	        		importer.findNextBlock();
//	                if (importer.getNextBlockName().equalsIgnoreCase("FIGTREE")) {
//	                    importer.parseFigTreeBlock(settings);
//	                }
//	            } catch (EOFException ex) {
//	               break;
//	            }
//	        }
//	
//	        treeViewer.setTrees(trees);
//	        controlPalette.setSettings(settings);
//	        
//	        int width = 650;
//	        int height = 650;
//	        treeViewer.getContentPane().setSize(width, height);
//	
//	        JComponent comp = treeViewer.getContentPane();
//	        ((TreePane)comp).setTreeLayout(new RadialTreeLayout());
//	        SimpleLabelPainter simpleLabelPainter = new SimpleLabelPainter(SimpleLabelPainter.PainterIntent.TIP);
//	        simpleLabelPainter.setVisible(false);
//	        ((TreePane)comp).setTipLabelPainter(simpleLabelPainter);
//	        
//	        ((TreePane)comp).setBranchStroke(new BasicStroke(2.0f));
//	        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//	        Graphics g = bi.createGraphics();
//	        comp.paint(g);
//	        g.dispose();
//	        
//	        File file = new File(this.folderLocationTreeView + File.separator + clusterId + ".png");
//			ImageIO.write(bi, "PNG", file);
//        } catch(IOException ioe) {
//        	throw new RuntimeException("Error writing graphic file: " + ioe);
//        } catch (ImportException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	static boolean isClosed;
	public HashMap<String, Integer> readCsv(int clusterId, String key, boolean readNA) {
		HashMap<String,Integer> hashMap = new HashMap<String, Integer>();
		FileInputStream fileInputStream = null;
		DirectoryStream<Path> dirStream = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dirStream = Files.newDirectoryStream(FileSystems.getDefault().getPath(Settings.getXmlPath(basePath)));
//			System.out.println(new File(folderLocationCsvs + File.separator + clusterId + ".xml"));
			if(Settings.getXmlPath(basePath) != null && !Settings.getXmlPath(basePath).equals("") && dirStream.iterator().hasNext()) {
				File file = new File(Settings.getXmlPath(basePath) + File.separator + clusterId + ".xml");
				fileInputStream = new FileInputStream(file);
				Document doc = dBuilder.parse(fileInputStream);
				XPathFactory xPathfactory = XPathFactory.newInstance();
				XPath xpath = xPathfactory.newXPath();
				NodeList nList = null;
				try {
					XPathExpression expr = xpath.compile("//FIELD[@name=\"" + key + "\"]");
					nList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
				// Could throw nullpointer if expression gave an exception
				int size = nList.getLength();
				for (int temp = 0; temp < size; temp++) {
					org.w3c.dom.Node nNode = nList.item(temp);
					if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						if(!readNA && !eElement.getAttribute("id").equals("NA") && !eElement.getAttribute("id").equals("")) {
							
//							hashMap.put(checkKey(key, eElement), Integer.parseInt(eElement.getTextContent()));
							if(hashMap.containsKey(eElement.getAttribute("id"))) {
								hashMap.put(eElement.getAttribute("id"), hashMap.get(eElement.getAttribute("id")) + Integer.parseInt(eElement.getTextContent()));
							} else {
								hashMap.put(eElement.getAttribute("id"), Integer.parseInt(eElement.getTextContent()));
							}
						} else if(readNA) {
//							hashMap.put(eElement.getAttribute("id"), Integer.parseInt(eElement.getTextContent()));
							if(hashMap.containsKey(eElement.getAttribute("id"))) {
								hashMap.put(eElement.getAttribute("id"), hashMap.get(eElement.getAttribute("id")) + Integer.parseInt(eElement.getTextContent()));
							} else {
								hashMap.put(eElement.getAttribute("id"), Integer.parseInt(eElement.getTextContent()));
							}
						}
//						System.out.println("ID : " + eElement.getAttribute("id"));
//						System.out.println("Value : " + eElement.getTextContent());
					}
				}
				try {
					fileInputStream.close();
				} catch(IOException ioException) {
					System.err.println("Exception: The fileInputStream in PreRendering.readCsv was already closed.");
				}
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(dirStream != null) {
				try {
					dirStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return hashMap;
	}
	
	public static void main(String[] args) {
//		PreRendering preRendering = new PreRendering("/Users/ewout/Documents/phylogeo/portugal/clusters", "/Users/ewout/Documents/phylogeo/portugal/xml", "/Users/ewout/Documents/phylogeo/portugal/treeview", "/Users/ewout/Documents/phylogeo/portugal/leafIds", "/Users/ewout/Documents/phylogeo/portugal/nodeIds");
//		PreRendering preRendering = new PreRendering("/Users/ewout/Documents/phylogeo/portugal/Test/clusters", "/Users/ewout/Documents/phylogeo/portugal/Test/xml", "/Users/ewout/Documents/phylogeo/portugal/Test/treeview", "/Users/ewout/Documents/phylogeo/portugal/Test/leafIds", "/Users/ewout/Documents/phylogeo/portugal/Test/nodeIds");
//		PreRendering preRendering = new PreRendering("/Users/ewout/Documents/phylogeo/EUResist/clusters", "/Users/ewout/Documents/phylogeo/EUResist/xml", "/Users/ewout/Documents/phylogeo/EUResist/treeview", "/Users/ewout/Documents/phylogeo/EUResist/leafIds", "/Users/ewout/Documents/phylogeo/EUResist/nodeIds");
//		PreRendering preRendering = new PreRendering("/Users/ewout/Documents/phylogeo/Test/clusters", "/Users/ewout/Documents/phylogeo/Test/xml", "/Users/ewout/Documents/phylogeo/Test/treeview", "/Users/ewout/Documents/phylogeo/Test/leafIds");
//		preRendering.preRender("/Users/ewout/Documents/phylogeo/EUResist_New/tree/besttree.midpoint.newick", "/Users/ewout/Documents/phylogeo/EUResist_New/EUResist.metadata.csv");
//		preRendering.preRender("/Users/ewout/Documents/phylogeo/EUResist_New/tree/besttree.midpoint.newick", "/Users/ewout/Documents/phylogeo/EUResist/EUResist.metadata.cleaned.csv");
//		preRendering.preRender("/Users/ewout/Documents/phylogeo/portugal/RAxML_bipartitions.final_tree", "/Users/ewout/Documents/phylogeo/portugal/final.csv");
//		preRendering.preRender("/Users/ewout/git/phylogeotool/lib/EwoutTrees/test.tree", "/Users/ewout/git/phylogeotool/lib/EwoutTrees/temp.csv");
//		preRendering.getLeafIdFromXML("1");
	
		String treeLocation = "";
		String csvLocation = "";
		String distanceMatrixLocation = "";
		String rBinaryLocation = "";
		String rScriptsLocation = "";
		int minimumClusterSize = 2;
		
		if(args.length > 5) {
			treeLocation = args[0];
			csvLocation = args[1];
			distanceMatrixLocation = args[2];
			basePath = args[3];
			rBinaryLocation = args[4];
			rScriptsLocation = args[5];
			
			ReadTree.setJeblTree(treeLocation);
			ReadTree.setTreeDrawTree(ReadTree.getJeblTree());
			
			checkFoldersEmpty(basePath);
		} else {
			System.err.println("You need to have a java -version > 7");
			System.err.println("java -jar PreRendering.jar phylo.tree csvFile distance.matrix basePath rBinaryLocation rScriptsLocation");
			System.exit(0);
		}
		
//		PreRendering preRendering = new PreRendering("/Users/ewout/Documents/phylogeo/Configs/Portugal/tree","/Users/ewout/Documents/phylogeo/Configs/Portugal/clusters", "/Users/ewout/Documents/phylogeo/Configs/Portugal/xml", "/Users/ewout/Documents/phylogeo/Configs/Portugal/treeview", "/Users/ewout/Documents/phylogeo/Configs/Portugal/leafIds", "/Users/ewout/Documents/phylogeo/Configs/Portugal/nodeIds");
		PreRendering preRendering = new PreRendering(basePath);
		
		try {
//			preRendering.preRender("/Users/ewout/Documents/TDRDetector/fullPortugal/trees/fullTree.Midpoint.tree", "/Users/ewout/Documents/TDRDetector/fullPortugal/allSequences_cleaned_ids.out2.csv", "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.portugal.txt");
			preRendering.preRender(treeLocation, csvLocation, distanceMatrixLocation, Paths.get(rBinaryLocation), Paths.get(rScriptsLocation));
//			preRendering.preRender("/Users/ewout/Documents/phylogeo/TestCases/Portugal/besttree.500.midpoint.solved.newick", "", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
//		preRendering.preRender("/Users/ewout/Documents/phylogeo/EUResist/data/temp/phylo.tree", "/Users/ewout/Documents/phylogeo/EUResist_New/EUResist_one_seq_per_pat.csv", "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.EUResist.shortened.txt");
//		preRendering.preRender("/Users/ewout/git/phylogeotool/lib/Test/Portugal/besttree.500.midpoint.solved.newick", "", "/Users/ewout/git/phylogeotool/lib/Test/Portugal/distance.500.txt");
	}
}
