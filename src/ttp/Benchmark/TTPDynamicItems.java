package ttp.Benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ttp.TTPInstance;
import ttp.TTPSolution;
import ttp.Optimisation.MyOperators;

/**
 * This class is designed for generating or reading Dynamic Items
 * @author pz.yao
 */
public class TTPDynamicItems {
	/*
	 * Integer matrix container for
	 * storing items' info
	 */
	public int[][] initItems;
	/*
	 * Probability of status change
	 */
	public double prob;
	/*
	 * Length of row of matrix container
	 */
	public int row;
	/*
	 * Length of col of matrix container
	 */
	public int col;
	/*
	 * loaded dynamic items
	 */
	public ArrayList<ArrayList<Integer>> itemSet;

	/**
	 * Constructor
	 * read TTPInstance and generate dynamic item benchmarks
	 * @param instance
	 */
	public TTPDynamicItems(TTPInstance instance) {
		// record row and col size of original item sets
		row = instance.items.length;
		col = instance.items[0].length;
		// record total number of items
		int m = row;
		// setup mutation probability
		prob = 5 / (double)m;

		// store original items
		initItems = new int[row][col];
		for(int i = 0; i < row; ++i) {
			for(int j = 0; j < col; ++j) {
				// copy items' info
				initItems[i][j] = instance.items[i][j];
			}
		}

	}

	/**
	 * This method takes current item sets and generation number
	 * to generate new item sets.
	 * @param instance
	 * @param generation
	 * @throws IOException 
	 */
	public void generateDynamicItem(TTPInstance instance,int totalGeneration,int itemSetVersion) throws IOException {
		// setup which target version folder of 
		// dynamic benchmark
		String version = "";
		String benchmark1 = "benchmark1";
		String benchmark2 = "benchmark2";

		// setup benchmark version
		if(itemSetVersion == 1) {
			version = benchmark1;
		}
		else if (itemSetVersion == 2) {
			version = benchmark2;
		}
		else {
			// print out error message
			System.out.println("Invalid item set version number, Please choose from 1 or 2.");
			System.exit(0); 
		}

		// dynamic mutate items' availability
		// and save the new item set to benchmark file
		String inputDataName = String.valueOf(instance.file);
		inputDataName = inputDataName.substring(10, inputDataName.length()-4);
		String outputFileName = "benchmarks/dynamicItems/"+inputDataName+"/"+version+ "-items.txt";
		// create file and folders if not exist
		File file = new File(outputFileName);
		file.getParentFile().mkdirs();

		// start write applied operation into file
		try(  FileWriter writer = new FileWriter(file);  ){
			// loop generation
			for(int generation = 1; generation <= totalGeneration; ++generation) {

				// check generation flag to see 
				// if it reaches every 50 generation
				int generationFlag = generation % 50;
				if(generationFlag != 0) {
					// no action will perform
					// between each 50 generation
					continue;
				}
				// write current generation into file
				String currentItemStatus = String.valueOf(generation) + ":";

				// loop all items in current generation
				for(int i = 0; i < row; ++i) {
					double currentDice = Math.random();
					// current dice value larger than probability
					// then no status change on this element
					if(currentDice > prob) {
						// jump to next element
						continue;
					}
					else {
						// change the item's status
						if(instance.items[i][1] > 0) {
							instance.items[i][1] = 0;
							String tmp = String.valueOf(i);
							// check if it is the first applied operation
							if(currentItemStatus.charAt(currentItemStatus.length()-1) == ':') {
								// save item's status in benchmark
								currentItemStatus = currentItemStatus + tmp;
							}
							else {
								// save item's status in benchmark
								currentItemStatus = currentItemStatus + "," + tmp;
							}
						}
						else {
							instance.items[i][1] = initItems[i][1];
						}
					}
				}
				// start from new line
				currentItemStatus = currentItemStatus + "\n";
				writer.write(currentItemStatus);
			}
			writer.close();
		}
	}

	/**
	 * This method takes input file name and item set version
	 * number to read pre-generated dynamic item sets.
	 * @param instance
	 * @param generation
	 * @param itemSetVersion
	 */
	public void readDynamicItem(TTPInstance instance, int itemSetVersion) {
		// setup which version of pre-generated dynamic benchmark
		// will be read in this generation.
		String version = "";
		String benchmark1 = "benchmark1";
		String benchmark2 = "benchmark2";

		// setup benchmark version
		if(itemSetVersion == 1) {
			version = benchmark1;
		}
		else if (itemSetVersion == 2) {
			version = benchmark2;
		}
		else {
			// print out error message
			// terminate program
			System.out.println("Invalid item set version number, Please choose from 1 or 2.");
			System.exit(0); 
		}

		// data set initialization
		itemSet = new ArrayList<ArrayList<Integer>>();

		// setup new item file name
		String inputDataName = String.valueOf(instance.file);
		inputDataName = inputDataName.substring(10, inputDataName.length()-4);
		String readFileName = "benchmarks/dynamicItems/"+inputDataName+"/"+version + "-items.txt";

		// open item file
		File file = new File(readFileName);
		BufferedReader reader = null;

		// start read item's info from item file
		try {
			// setup new reader and text container
			reader = new BufferedReader(new FileReader(file));
			String text = null;

			while ((text = reader.readLine()) != null) {
				// read status changed info of dynamic item
				String[] tmpSplit = text.split(":");
				
				if(tmpSplit.length == 2) {
					String tmpItems = tmpSplit[1];
					String[] tmpStatus = tmpItems.split(",");
					
					// create new array
					ArrayList<Integer> tmp = new ArrayList<Integer>();
					// store all applied changes to arrayList
					for(int i = 0; i < tmpStatus.length; ++i) {
						tmp.add(Integer.valueOf(tmpStatus[i]));
					}
					// store to list
					itemSet.add(tmp);
				}
				else {
					// create new array
					ArrayList<Integer> tmp = new ArrayList<Integer>();
					// store to list
					itemSet.add(tmp);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					// reading finish
					// close reader
					reader.close();
				}
			} catch (IOException e) {
			}
		}	
	}
	
	/**
	 * This method will update item status based on current
	 * generation.
	 * @param instance
	 * @param currentGeneration
	 */
	public void updateCurrentItems(TTPInstance instance, int currentGeneration) {
		// check generation flag to see 
		// if it reaches every 50 generation
		int generationFlag = currentGeneration % 50;
		if(generationFlag != 0) {
			// no action will perform
			// between each 50 generation
			return;
		}

		// reset instance to original item set
		for(int i = 0; i < row; ++i) {
			if(instance.items[i][1] == 0) {
				// roll back all profit value to original value
				instance.items[i][1] = initItems[i][1];
			}
		}
		
		// setup reading index
		int index = currentGeneration / 50;
		// apply operation on items
		for(int i = 0; i < itemSet.get(index).size(); ++i) {
			// update item's status
			instance.items[itemSet.get(index).get(i)][1] = 0;
		}
	}
	
	/**
	 * This method will check the new solution item status in order to void
	 * picking unavailable items.
	 * @param solu
	 * @param currentGeneration
	 * @param instance
	 * @param tour
	 * @return
	 */
	public boolean validateCurrentItems(TTPSolution solu, TTPInstance instance, int[] tour) {
		// setup flag for checking the new solution packingPlan
		// if no unavailable item is chosen, then flag false
		// if there are unavailable item chosen, then flag true
		boolean flag = false;
		
		// start checking status of unavailable item in new packingPlan
		for(int i = 0; i < solu.packingPlan.length; ++i) {
			// get target item packingPlan index
			int packingPlan_Index = i;
			// covert packingPlan index to item index
			int item_Index = MyOperators.PackingPlan2Item(packingPlan_Index, instance, tour);
			
			// check item's status in dynamic item lists
			if(instance.items[item_Index][1] <= 0) {
				// change flag status
				flag = true;
				// ensure new solution packingPlan does not pick unavailable
				// and set packingPlan of that item to 0
				solu.packingPlan[i] = 0;
			}
		}
		return flag;
	}
}
