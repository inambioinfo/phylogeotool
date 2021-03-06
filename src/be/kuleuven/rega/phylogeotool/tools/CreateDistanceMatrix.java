package be.kuleuven.rega.phylogeotool.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jebl.evolution.trees.SimpleRootedTree;
import be.kuleuven.rega.phylogeotool.core.Node;
import be.kuleuven.rega.phylogeotool.core.Tree;
import be.kuleuven.rega.phylogeotool.io.read.ReadTree;
import be.kuleuven.rega.phylogeotool.tree.distance.DistanceCalculateFromTree;
import be.kuleuven.rega.phylogeotool.tree.distance.DistanceInterface;

public class CreateDistanceMatrix {

	private HashMap<String, Integer> translatedNodeNames = new HashMap<String, Integer>();
	private Tree tree = null;
	private List<Node> leafs = new ArrayList<Node>();
	
	public static void main(String[] args) {
		String treeLocation = "";
		String distanceMatrixLocation = "";
		
		if (args.length >= 2) {
			treeLocation = args[0];
			distanceMatrixLocation = args[1];
			ReadTree.setJeblTree(treeLocation);
			ReadTree.setTreeDrawTree(ReadTree.getJeblTree());
		} else {
			System.err.println("TreeLocation: " + treeLocation);
			System.err.println("Distance Matrix Location: " + distanceMatrixLocation);
			System.err.println("java -jar DistanceMatrix.jar phylo.tree distance.matrix");
			System.exit(0);
		}
		
//		String treeLocation = "/Users/ewout/Documents/TDRDetector/fullPortugal/trees/RAxML_bipartitions.fullPortugal.final.tree";
//		String treeLocation = "/Users/ewout/Documents/TDRDetector/fullPortugal/trees/fullTree.Midpoint.tree";
//		String treeLocation = "/Users/ewout/Documents/phylogeo/EUResist/data/temp/phylo.solved.midpoint.newick";
//		String treeLocation = "/Users/ewout/git/phylogeotool/lib/Test/tree.phylo";
//		String treeLocation = "/Users/ewout/Documents/phylogeo/TestCases/Portugal/besttree.500.midpoint.solved.newick";
//		String distanceMatrixLocation = "/Users/ewout/git/phylogeotool/lib/Test/Portugal/distance.EUResist.txt";
//		String distanceMatrixLocation = "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.test.other.txt";
//		String distanceMatrixLocation = "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.500.txt";
//		String distanceMatrixLocation = "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.EUResist.txt";
//		String distanceMatrixLocation = "/Users/ewout/Documents/phylogeo/TestCases/Portugal/distance.portugal.txt";
		
		CreateDistanceMatrix createDistanceMatrix = new CreateDistanceMatrix();
		try {
			createDistanceMatrix.init(treeLocation);
			createDistanceMatrix.createDistanceMatrix(distanceMatrixLocation, new DistanceCalculateFromTree());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void init(String treeLocation) throws FileNotFoundException {
		jebl.evolution.trees.Tree jeblTree = ReadTree.readTree(new FileReader(treeLocation));
		tree = ReadTree.jeblToTreeDraw((SimpleRootedTree) jeblTree, new ArrayList<String>());
		
		if(jeblTree.getTaxa().size() != tree.getLeaves().size()) {
			System.err.println("Phylogenetic tree not accepted.");
			System.err.println("Please check tree structure for polytomies");
			System.exit(0);
		}
		
		int index = 0;
		for (Node leaf : tree.getLeaves()) {
			leafs.add(leaf);
			translatedNodeNames.put(leaf.getLabel(), index++);
		}
	}
	
	public void createDistanceMatrix(String distanceMatrixLocation, DistanceInterface distanceInterface) throws IOException {
		FileWriter fw = new FileWriter(new File(distanceMatrixLocation));
		DecimalFormat df = new DecimalFormat("#.####");
		for(int i = 0; i < translatedNodeNames.keySet().size(); i++) {
			for(int j = 0; j < translatedNodeNames.keySet().size(); j++) {
				if(j >= i + 1) {
					fw.write(df.format(distanceInterface.getDistance(tree, leafs.get(i), leafs.get(j))) + ";");
				} else {
					fw.write(";");
				}
			}
			System.err.println("i: " + i);
			fw.write("\n");
		}
		fw.close();
	}
	
}
