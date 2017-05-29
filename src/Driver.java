
import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;

import ttp.Optimisation.MyOperators;
import ttp.Optimisation.Optimisation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import ttp.TTPInstance;
import ttp.TTPSolution;
import ttp.Benchmark.TTPDynamicItems;
import ttp.Utils.Configuration;
import ttp.Utils.DataPool;
import ttp.Utils.DeepCopy;
import ttp.Utils.Utils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author puzhiyao
 * This class is adopted from original Driver created by @author wagner
 */
public class Driver {
	/* The current sequence of parameters is
	 * args[0]  folder with TTP files
	 * args[1]  pattern to identify the TTP problems that should be solved
	 * args[2]  optimization approach chosen [disabled in dynamic items, all approaches will be used]
	 * args[3]  stopping criterion: number of evaluations without improvement
	 * args[4]  stopping criterion: time in milliseconds (e.g., 60000 equals 1 minute)
	 * args[5]	Dynamic Item Set Mode: 1 = Generate New benchmark, 2 = Read from pre-generated benchmark
	 * args[6]	target version of benchmark (Choose 1 or 2) since each data sets has two dynamic item benchmark
	 * args[7]  number of generations
	 * args[8]  number of runs
	 * 
	 * All parameters are set in Config file
	 */
	public static void main(String[] args) throws IOException {
		// run configuration
		Configuration config = new Configuration();
		String[] parameters = config.getConfig();

		// fetching previous set parameters from config file
		if(parameters[1].equals("0")) {
			// run all 9 instances
			for(int i = 0; i < 9; ++i) {
				args = new String[]{parameters[0], config.getInstanceName(i),
						parameters[2], parameters[3], parameters[4],parameters[5],parameters[6],parameters[7],parameters[8]};
				// run dynamic items
				if(parameters[6].equals("0")) {
					// run both two benchmarks
					args[6] = "1";
					DynamicItems(args);
					args[6] = "2";
					DynamicItems(args);
				}
				else if(parameters[6].equals("1")) {
					// run benchmark version 1
					DynamicItems(args);
				}
				else if(parameters[6].equals("2")) {
					// run benchmark version 2
					DynamicItems(args);
				}
			}
		}
		else {
			// run 1 instance
			int instanceIndex = Integer.valueOf(parameters[1]) - 1;
			args = new String[]{parameters[0],config.getInstanceName(instanceIndex),
					parameters[2], parameters[3], parameters[4],parameters[5],parameters[6],parameters[7],parameters[8]};
			// run dynamic items
			if(parameters[6].equals("0")) {
				// run both two benchmarks
				args[6] = "1";
				DynamicItems(args);
				args[6] = "2";
				DynamicItems(args);
			}
			else if(parameters[6].equals("1")) {
				// run benchmark version 1
				DynamicItems(args);
			}
			else if(parameters[6].equals("2")) {
				// run benchmark version 2
				DynamicItems(args);
			}
		}
	}

	/**
	 * DynamicItems can process several files sequentially with
	 * dynamic item sets
	 * @param args
	 * @throws IOException 
	 */
	public static void DynamicItems(String[] args) throws IOException {
		// read file name from arguments
		File[] files = ttp.Utils.Utils.getFileList(args);

		// setup algorithm and parameters from arguments
		int algorithm = Integer.parseInt(args[2]);
		int durationWithoutImprovement = Integer.parseInt(args[3]);
		int maxRuntime = Integer.parseInt(args[4]);

		// setup which version of generated dynamic item set
		// will be used.(Choose 1 or 2)
		int itemSetVersion = Integer.parseInt(args[6]);

		// setup which mode of dynamic item will be used
		// 1 for generating new dynamic item sets
		// 2 for reading old dynamic item sets
		int DynaItemMode = Integer.parseInt(args[5]);

		// setup number of generations
		// compute 100,000 generations with multiple algorithms
		int totalGeneration = Integer.parseInt(args[7]);
		// setup number of runs
		int totRuns = Integer.parseInt(args[8]);;

		// setup record of best solution
		DataPool pool = new DataPool();

		// start iterate input files
		for (File f:files) {
			// output current instance name
			System.out.println(f.getName());
			// read the TTP instance
			TTPInstance instance = new TTPInstance(f);

			// setup TTP dynamic items from instance
			TTPDynamicItems DynaItems = new TTPDynamicItems(instance);

			// Repeat runs on each instances for each each algorithm
			for (int currentTimeOfRun = 1; currentTimeOfRun <= totRuns; ++currentTimeOfRun) {
				// print out current run number
				System.out.println("Run "+currentTimeOfRun+": ");
				// setup for best solution quality container
				double bestOB_Reference_RLS = Double.NEGATIVE_INFINITY;
				long bestComputationTime_Reference_RLS = 0;

				double bestOB_Reference_11EA = Double.NEGATIVE_INFINITY;
				long bestComputationTime_Reference_11EA = 0;

				double bestOB_ComplexHeuristic_1 = Double.NEGATIVE_INFINITY;
				long bestComputationTime_ComplexHeuristic_1 = 0;

				double bestOB_ComplexHeuristic_2 = Double.NEGATIVE_INFINITY;
				long bestComputationTime_ComplexHeuristic_2 = 0;

				// Identify Dynamic Item Mode
				if(DynaItemMode == 1) {
					// generate new dynamic items based on current generation
					DynaItems.generateDynamicItem(instance, totalGeneration, itemSetVersion);
				}

				// load previous generated benchmark into memory
				DynaItems.readDynamicItem(instance, itemSetVersion);

				// start generation iteration
				for (int currentGeneration = 1; currentGeneration <= totalGeneration; currentGeneration=currentGeneration + 1) {        	
					if(currentGeneration == 51) {
						currentGeneration = 50;
					}

					// update current generation dynamic item status
					DynaItems.updateCurrentItems(instance, currentGeneration);

					// setup start computing time
					long startTime = System.currentTimeMillis();
					// String resultTitle_Reference = instance.file.getName() + ".NameOfTheAlgorithm." + currentGeneration;

					// generate a Linkern tour (or read it if it already exists)
					int[] tour = Optimisation.linkernTour(instance);

					//System.out.print(f.getName()+": ");

					// Run reference RLS approach
					// add 200 ms to compensate time limit
					TTPSolution solution_Reference_RLS = Optimisation.hillClimber(instance, tour, 1, 
							durationWithoutImprovement, maxRuntime+200);

					// check if new solution picked unavailable items
					boolean flag_RLS = DynaItems.validateCurrentItems(solution_Reference_RLS, instance, tour);
					// re-evaluate updated solution if unavailable items were picked before
					if(flag_RLS == true) {
						// re-evaluation
						instance.evaluate(solution_Reference_RLS);
					}

					// check if solution is valid
					// weight remain must larger than zero
					if(solution_Reference_RLS.wend > 0) {
						// check best solution quality
						if(solution_Reference_RLS.ob > bestOB_Reference_RLS) {
							bestOB_Reference_RLS = solution_Reference_RLS.ob;
							bestComputationTime_Reference_RLS = solution_Reference_RLS.computationTime;
						}
					}

					// Run reference (1+1) EA approach
					// add 200 ms to compensate time limit
					TTPSolution solution_Reference_11EA = Optimisation.hillClimber(instance, tour, 2, 
							durationWithoutImprovement, maxRuntime+200);

					// check if new solution picked unavailable items
					boolean flag_11EA = DynaItems.validateCurrentItems(solution_Reference_11EA, instance, tour);
					// re-evaluate updated solution if unavailable items were picked before
					if(flag_11EA == true) {
						// re-evaluation
						instance.evaluate(solution_Reference_11EA);
					}

					// check if solution is valid
					// weight remain must larger than zero
					if(solution_Reference_11EA.wend > 0) {
						// check best solution quality
						if(solution_Reference_11EA.ob > bestOB_Reference_11EA) {
							bestOB_Reference_11EA = solution_Reference_11EA.ob;
							bestComputationTime_Reference_11EA = solution_Reference_11EA.computationTime;
						}
					}

					// Run Exercise 1 designed ComplexHeuristic_1 algorithm
					TTPSolution solution_ComplexHeuristic_1 = MyOperators.ComplexHeuristic_1(instance, tour,
							durationWithoutImprovement, maxRuntime);

					// check if new solution picked unavailable items
					boolean flag_CH1 = DynaItems.validateCurrentItems(solution_ComplexHeuristic_1, instance, tour);
					// re-evaluate updated solution if unavailable items were picked before
					if(flag_CH1 == true) {
						// re-evaluation
						instance.evaluate(solution_ComplexHeuristic_1);
					}

					// check if solution is valid
					// weight remain must larger than zero
					if(solution_ComplexHeuristic_1.wend > 0) {
						// check best solution quality
						if(solution_ComplexHeuristic_1.ob > bestOB_ComplexHeuristic_1) {
							bestOB_ComplexHeuristic_1 = solution_ComplexHeuristic_1.ob;
							bestComputationTime_ComplexHeuristic_1 = solution_ComplexHeuristic_1.computationTime;
						}
						//System.out.println("solution_ComplexHeuristic_1: "+solution_ComplexHeuristic_1.ob);
					}

					// Run Exercise 2 designed ComplexHeuristic_2 algorithm
					TTPSolution solution_ComplexHeuristic_2 = MyOperators.ComplexHeuristic_2(instance, tour,
							durationWithoutImprovement, maxRuntime);

					// check if new solution picked unavailable items
					boolean flag_CH2 = DynaItems.validateCurrentItems(solution_ComplexHeuristic_2, instance, tour);
					// re-evaluate updated solution if unavailable items were picked before
					if(flag_CH2 == true) {
						// re-evaluation
						instance.evaluate(solution_ComplexHeuristic_2);
					}

					// check if solution is valid
					// weight remain must larger than zero
					if(solution_ComplexHeuristic_2.wend > 0) {
						// check best solution quality
						if(solution_ComplexHeuristic_2.ob > bestOB_ComplexHeuristic_2) {
							bestOB_ComplexHeuristic_2 = solution_ComplexHeuristic_2.ob;
							bestComputationTime_ComplexHeuristic_2 = solution_ComplexHeuristic_2.computationTime;
						}
						//System.out.println("solution_ComplexHeuristic_2: "+solution_ComplexHeuristic_2.ob);
					}

					// print complete percentage
					double completeRate = ((double)currentGeneration / (double) totalGeneration) * 100;
					// Round a completeRate to 3 significant figures
					BigDecimal bd = new BigDecimal(completeRate);
					bd = bd.round(new MathContext(3));
					double roundRatio= bd.doubleValue();
					// setup end time
					long tmpEndTime = System.currentTimeMillis();
					long estimateTotalTime = ((tmpEndTime - startTime) * (totalGeneration - currentGeneration)) / (long)1000;
					System.out.println("Complete Rate: "+currentGeneration+"/"+totalGeneration+" ("+roundRatio+" %)"+" Time Remain: "+estimateTotalTime+" s");
				}

				// complete a series of generation
				// store best result from this generation
				double[] tmpOB = {bestOB_Reference_RLS,bestOB_Reference_11EA,bestOB_ComplexHeuristic_1,bestOB_ComplexHeuristic_2};
				long[] tmpCT = {bestComputationTime_Reference_RLS,bestComputationTime_Reference_11EA,bestComputationTime_ComplexHeuristic_1,bestComputationTime_ComplexHeuristic_2};

				// store solution quality of current run
				pool.addOB(tmpOB);
				pool.addCT(tmpCT);
				saveSolution(f,itemSetVersion,pool);
			}
		}
	}

	/**
	 * This method will save best solution every run
	 * @param f
	 * @param benchmark
	 * @throws IOException
	 */
	public static void saveSolution(File f, int benchmark, DataPool p) throws IOException {
		String tmpFileName = f.toString();
		String outputFileName = "results/"+tmpFileName.substring(10, tmpFileName.length()-4)+"-benchmark"+benchmark+".csv";

		// check output dir exist or not
		File outputFile = new File(outputFileName);
		// check if output file exist or not 
		if(!outputFile.exists()) {
			// check if output file folder exist or not
			if(!outputFile.getParentFile().exists()) {
				// create folder
				outputFile.getParentFile().mkdirs();
			}
			// create file
			outputFile.createNewFile();
		}

		// writing results into file
		FileWriter fw = new FileWriter(outputFileName, true);
		try(  PrintWriter out = new PrintWriter(fw)  ){
			out.println("Run,OB-Ref-RLS,OB-Ref-11EA,OB-ComplexHeuristic_1,OB-ComplexHeuristic_2,Time-Ref-RLS,Time-Ref-11EA,Time-ComplexHeuristic_1,Time-ComplexHeuristic_2");
			for(int i = 0; i < p.bestOB.size(); ++i) {
				int tmpIndex = i + 1;
				out.println(tmpIndex+","+p.bestOB.get(i)[0]+","+p.bestOB.get(i)[1]+","+p.bestOB.get(i)[2]+","+p.bestOB.get(i)[3]+","+p.bestCT.get(i)[0]+","+p.bestCT.get(i)[1]+","+p.bestCT.get(i)[2]+","+p.bestCT.get(i)[3]);
			}
		}
	}
}
