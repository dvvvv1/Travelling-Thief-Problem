package ttp.Optimisation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import ttp.TTPInstance;
import ttp.TTPSolution;
import ttp.Utils.DeepCopy;

/**
 * This class is designed for finding an acceptable PackingPlan solution for the given TTP problem instance and TSP tour 
 * @author Puzhi Yao
 *
 */
public class MyOperators {
	/**
	 * This function is designed for converting the index of PackingPlan into the index of in Items
	 * @param PPIndex Index of PackingPlan
	 * @param instance TTP instance
	 * @param tour TSP tour
	 * @return itemIndex index of items
	 */
	public static int PackingPlan2Item(int PPIndex, TTPInstance instance, int[] tour){
		int ItemsPerCity = instance.numberOfItems / (instance.numberOfNodes-1);
		// Formula:	
		// int indexOfPackingPlan = (i-1)*ItemsPerCity+itemNumber;
		int i = PPIndex / ItemsPerCity + 1;
		int itemNumber = PPIndex - (i-1) * ItemsPerCity;
		int currentCity = tour[i]-1;
		// calculate item index based on
		// previous calculated parameters
		int itemIndex = currentCity+itemNumber*(instance.numberOfNodes-1);//* (this.numberOfNodes-1); 
		return itemIndex;

	}

	/**
	 * This function is designed for converting the index of Items into the index of in PackingPlan
	 * @param ItemIndex Index of items
	 * @param instance TTP instance
	 * @param tour TSP tour
	 * @return itemIndex index of PackingPlan
	 */
	public static int Item2PackingPlan(int ItemIndex, TTPInstance instance, int[] tour){
		int ItemsPerCity = instance.numberOfItems / (instance.numberOfNodes-1);
		// Formula:     
		// int itemIndex = currentCity+itemNumber*(instance.numberOfNodes-1);//* (this.numberOfNodes-1); 
		int itemNumber = ItemIndex / (instance.numberOfNodes-1);
		// calculate current city index
		int currentCity = ItemIndex % (instance.numberOfNodes-1);
		int currentCityIndex = -1;
		// loop to find the current city index in tour
		for(int city =0 ; city < tour.length; ++city){
			if(currentCity + 1 == tour[city]) {
				currentCityIndex = city;
				break;
			}
		}
		int i = currentCityIndex;
		// calculate item index in packingPlan
		int indexOfPackingPlan = (i-1)*ItemsPerCity+itemNumber;
		// output packingplan item index
		int PPIndex = indexOfPackingPlan;
		return PPIndex;

	}

	/**
	 * This function is implemented for our algorithm to find a acceptable solution for TTP
	 * @param instance TTP instance
	 * @param tour TSP tour
	 * @param durationWithoutImprovement the number of times as termination conditions allowed to improve a packingPlan
	 * @param maxRuntime the maximum runtime as termination conditions allowed to improve a packingPlan
	 * @return
	 */
	public static TTPSolution ComplexHeuristic_1(TTPInstance instance, int[] tour,  int durationWithoutImprovement, int maxRuntime){
		// setup timer
		ttp.Utils.Utils.startTiming();
		long startTime_G = System.currentTimeMillis();

		// Recalculate the profit of each item according to the weight and increasing travel time 
		double[] penalised_profits = PenaltyFunction(instance, tour);

		// Calculate and Sort the Index of valuePerUnit for each item and store in list[]
		// valuePerUnit = profit / weight
		double[] valuePerUnit = new double[penalised_profits.length];
		Integer[] list = new Integer[penalised_profits.length];
		for(int i =0; i< list.length; ++i){
			// store index info in list
			list[i] = i;
			// store penalty values in array
			valuePerUnit[i] = penalised_profits[i] / instance.items[i][2];
		}
		
		// modify array comparator
		// let the comparator able to sort array based on another
		// array's element
		class MyComparator implements Comparator<Integer> {
			@Override
			public int compare(Integer i2, Integer i1) {
				return new Double(valuePerUnit[i1.intValue()]).compareTo(new Double(valuePerUnit[i2.intValue()]));
			}
		}
		// sort all elements based on penalty values
		Arrays.sort(list, new MyComparator());
		// setup current total weight container
		int totalW = 0;

		// Initialize a Solution
		int[] packingPlan = new int[instance.numberOfItems];
		// reset all packingplan
		Arrays.fill(packingPlan, 0);
		
		// create new solution
		TTPSolution temp = new TTPSolution(tour, packingPlan);
		// evaluate current plan
		instance.evaluate(temp);
		// distribute time to 2 approaches
		int halfTime = (int) (maxRuntime * 0.5);
		
		// Greedy Pick items according to the valuePerUnit of the items
		for(int pos = 0; pos <list.length; ++pos){
			// check execution time limit once per 50 runs
			if (pos % 25 == 0 && System.currentTimeMillis() - startTime_G > halfTime) {
				break;
			}
			// update new plan
			temp.packingPlan[Item2PackingPlan(list[pos], instance, tour)] = 1;
			packingPlan[Item2PackingPlan(list[pos], instance, tour)] = 1;
			
			// checkout new plan validity
			if((totalW + instance.items[list[pos]][2]) <= instance.capacityOfKnapsack && penalised_profits[list[pos]] >0 ){
				totalW += instance.items[list[pos]][2];
			}			
			else{
				// roll back if new plan is not valid
				temp.packingPlan[Item2PackingPlan(list[pos], instance, tour)] = 0;
				packingPlan[Item2PackingPlan(list[pos], instance, tour)] = 0;
			}

		}
		// evaluate new solution
		instance.evaluate(temp);

		// Improve the result from Greedy using LocalSearch
		TTPSolution	result =  LocalSearch(instance, tour, durationWithoutImprovement, halfTime, temp.packingPlan);
		
		//Evaluate the final result and record the computation time
		instance.evaluate(result);
		long duration = ttp.Utils.Utils.stopTiming();
		result.computationTime = duration;

		return result;
	}

	/**
	 * This function is designed for recalculating the profit of each item according to the weight and increasing travel time
	 * For each item in each city, first calculate the remain distance from the current city to the end
	 * Then calculate the modified velocity if taking a item and the increasing travel time
	 * Finally modify the profit of each item by decreasing the cost from increasing time * RentRatio
	 * @param instance TTP instance
	 * @param tour TSP tour
	 * @return double[] penalised_profits calculated profits
	 */
	public static double[] PenaltyFunction(TTPInstance instance, int[] tour){
		//int indexOfPackingPlan = (i-1)*itemsPerCity+itemNumber;
		
		// Initialise a solution for calculating the total distance
		int[] zeros =  new int[instance.numberOfItems];
		// setup zero array
		Arrays.fill(zeros, 0);
		// create new solution
		TTPSolution temp = new TTPSolution(tour, zeros);
		// evaluate new solution
		instance.evaluate(temp);
		// record current ftraw in solution
		long rawdistance = temp.ftraw;
		
		// setup penalty value arrays
		double[] penalised_profits = new double[instance.numberOfItems];
		// calculate the number of items in each city
		int ItemsPerCity = instance.numberOfItems / (instance.numberOfNodes-1);
		long remainDistance = rawdistance;
		
		// Calculate the profits of items by starting from the second city (no items in the first city)
		for(int CityIndex = 1; CityIndex < instance.numberOfNodes; ++CityIndex){
			// setup city index info
			int currentCityIndex = CityIndex;
			int PrevCityIndex = currentCityIndex - 1;
			int currentCity = tour[currentCityIndex] -1;
			// Calculate the distance of the remain tour for the current city 
			long prevDistance = (long)Math.ceil(instance.distances(tour[PrevCityIndex],tour[currentCityIndex]));
			remainDistance = remainDistance - prevDistance;

			// Calculate the penalised profit of each item in the current city
			for(int itemNumberInCity = 0; itemNumberInCity < ItemsPerCity; ++ itemNumberInCity){
				
				// Calculate the itemIndex in the items of instance for the current item
				int itemIndex = currentCity+itemNumberInCity*(instance.numberOfNodes-1);//* (this.numberOfNodes-1); 

				// Weight of the current item
				double wc = 0.0d + instance.items[itemIndex][2];
				// Calculate the modified velocity if taking this item
				double V_Diff = instance.maxSpeed - wc *(instance.maxSpeed-instance.minSpeed)/instance.capacityOfKnapsack;
				// Calculate the increasing travel time  if taking this item
				double time_Diff = remainDistance / V_Diff - remainDistance / instance.maxSpeed;
				// Calculate the increasing Rent if taking this item according the time
				double Penalty_profit = time_Diff * instance.rentingRatio;
				// Modify the proft according to the increasing travel cost if if taking this item
				double processed_profit = instance.items[itemIndex][1] - Penalty_profit;
				penalised_profits[itemIndex] = processed_profit ;
			}

		}

		return penalised_profits;

	}

	/**
	 * This function is designed for improve the packingPlan from Greedy Algorithm using  Local search algorithm
	 * The Local Search algorithm is RLS adopted from the given Package()
	 * @param instance TTP instance
	 * @param tour TSP tour
	 * @param durationToImprovement termination condition if no improvement reaching this number
	 * @param maxRuntime termination condition if total computation time reaching this number
	 * @param packingPlan the PackiingPlan to be improved
	 * @return bestOne the improved solution
	 */
	public static TTPSolution LocalSearch(TTPInstance instance, int[] tour, int durationToImprovement, int maxRuntime, int[] packingPlan){
		// parameter setup
		int i = 0;
		int counter = 0;
		// setup time clock
		long LS_Start_Time = System.currentTimeMillis();
		
		// setup new packinplan
		int[] newPackingPlan = (int[])DeepCopy.copy(packingPlan);
		TTPSolution bestOne = new TTPSolution(tour, packingPlan);
		// Evaluate current new plan
		instance.evaluate(bestOne);
		while(counter<durationToImprovement) {
			// do the time check just every 50 iterations, as it is time consuming
			if((i % 10) == 0 &&  (System.currentTimeMillis()-LS_Start_Time) > maxRuntime) {
				break;
			}
			// load current plan into container
			newPackingPlan = (int[])DeepCopy.copy(packingPlan);
			
			// start next position flip
			int position = (int)(Math.random()*newPackingPlan.length);
			if (newPackingPlan[position] == 1) {
				newPackingPlan[position] = 0;
			} else {
				newPackingPlan[position] = 1;
			}
			// create new solution
			TTPSolution NextbestOne = new TTPSolution(tour, newPackingPlan);
			// evaluate new solution
			instance.evaluate(NextbestOne);
			// compare new objective value and old one
			if (NextbestOne.ob >= bestOne.ob && NextbestOne.wend >=0 ) {
				// if new solution is valid
				// then copy new solution to current solution
				packingPlan = newPackingPlan;
				bestOne = NextbestOne;
				counter = 0;
			}
			else {
				// if not valid
				// counter increment
				counter++;
			}
			i++;
		}
		return bestOne;

	}
	
	/**
	 * This function is adopted from Local search algorithm
	 * The key point of this method is that the number of mutating position
	 * depends on the size of item number. (0.01% of the total number of items)
	 * The Local Search algorithm is RLS adopted from the given Package()
	 * @param instance
	 * @param tour
	 * @param durationToImprovement
	 * @param maxRuntime
	 * @param packingPlan
	 * @return
	 */
	public static TTPSolution ModifiedLocalSearch(TTPInstance instance, int[] tour, int durationToImprovement, int maxRuntime, int[] packingPlan){
		// parameter setup
		int index = 0;
		int counter = 0;
		// setup time clock
		long LS_Start_Time = System.currentTimeMillis();
		
		// setup current solution
		int[] newPackingPlan = (int[])DeepCopy.copy(packingPlan);
		TTPSolution currentSolution = new TTPSolution(tour, packingPlan); 
		
		// evaluate current solution
		instance.evaluate(currentSolution);
		
		while(true) {
			// do the time check just every 10 iterations, as it is time consuming
			if((index % 10) == 0 &&  (System.currentTimeMillis()-LS_Start_Time) > maxRuntime) {
				break;
			}
			// setup checking length
			double Length = 1;
			int checkLength = (int) Math.ceil(Length);
			// setup restore space
			int[][] tmpStatus = new int[checkLength][2];
			// start flip loop
			for(int i = 0; i < checkLength; ++i) {
				// random pick one bit to flip
				int position = (int)(Math.random()*newPackingPlan.length);
				// store current status of picked position
				tmpStatus[i][0] = position;
				tmpStatus[i][1] = newPackingPlan[position];
				
				// flip the status
				if (newPackingPlan[position] == 1) {
					newPackingPlan[position] = 0;
				} else {
					newPackingPlan[position] = 1;
				}
			}
			
			// check out the new solution objective value
			TTPSolution NextSolution = new TTPSolution(tour, newPackingPlan);
			instance.evaluate(NextSolution);
			if (NextSolution.ob > currentSolution.ob && NextSolution.wend >=0 ) {
				// update new solution
				currentSolution = NextSolution;
			}
			else{
				// roll back to old plan
				// and continue pick next random position
				for(int i = 0; i < checkLength; ++i) {
					newPackingPlan[tmpStatus[i][0]] = tmpStatus[i][1];
				}
			}
			// increment total number of iteration counter
			index++;
		}
		return currentSolution;
	}
	
	/**
	 * This function improves the implementation of GreedyRLS method and
	 * uses new LS to slightly mutate the results.
	 * @param instance
	 * @param tour
	 * @param durationWithoutImprovement
	 * @param maxRuntime
	 * @return
	 */
	public static TTPSolution ComplexHeuristic_2(TTPInstance instance, int[] tour,  int durationWithoutImprovement, int maxRuntime) {
		ttp.Utils.Utils.startTiming();
		long startTime_G = System.currentTimeMillis();

		// Recalculate the profit of each item according to the weight and increasing travel time 
		double[] penalised_profits = PenaltyFunction(instance, tour);

		// Calculate and Sort the Index of valuePerUnit for each item and store in list[]
		double[][] valuePerUnit = new double[penalised_profits.length][2];
		for(int i = 0; i < penalised_profits.length; ++i) {
			// valuePerUnit[i][0] = profit / weight
			valuePerUnit[i][0] = penalised_profits[i] / instance.items[i][2];
			// valuePerUnit[i][1] = item index;
			valuePerUnit[i][1] = i;
		}
	
		// sort the valuePerUnit by its profit/weight value
		Arrays.sort(valuePerUnit, new Comparator<double[]>() {
            @Override
            public int compare(final double[] entry1, final double[] entry2) {
                final Double value1 = entry1[0];
                final Double value2 = entry2[0];
                return value2.compareTo(value1);
            }
        });
		// setup current total weight
		int totalW = 0;
		
		// Intialise a Solution
		int[] packingPlan = new int[instance.numberOfItems];
		
		TTPSolution tempSolution = new TTPSolution(tour, packingPlan);
		instance.evaluate(tempSolution);
		// distribute time to 3 approaches
		int phase1_Time = (int) (maxRuntime * 0.2);
		int phase2_Time = (int) (maxRuntime * 0.4);
		int phase3_Time = (int) (maxRuntime * 0.4);
	
		// Greedy Pick items according to the valuePerUnit of the items
		for(int pos = 0; pos < valuePerUnit.length; ++pos){
			// check execution time limit once per 50 runs
			if (pos % 50 == 0 && System.currentTimeMillis() - startTime_G > phase1_Time) {
				break;
			}
			
			if((totalW + instance.items[(int) valuePerUnit[pos][1]][2]) <= instance.capacityOfKnapsack && valuePerUnit[pos][0] >0 ){
				totalW += instance.items[(int) valuePerUnit[pos][1]][2];
				tempSolution.packingPlan[Item2PackingPlan((int) valuePerUnit[pos][1], instance, tour)] = 1;
			}			
		}

		// Improve the result from Greedy using LocalSearch
		TTPSolution	result_RLS =  ModifiedLocalSearch(instance, tour, durationWithoutImprovement, phase2_Time, tempSolution.packingPlan);	
		
		// Improve the result from RLS
		TTPSolution result_Final = ModifiedEA(instance, tour, durationWithoutImprovement, phase3_Time, result_RLS.packingPlan);	
		
		//Evaluate the final result and record the computation time
		instance.evaluate(result_Final);
		long duration = ttp.Utils.Utils.stopTiming();
		result_Final.computationTime = duration;

		return result_Final;
	}

	/**
	 * This function is adopted from (1+1) EA
	 * The (1+1)EA is EA adopted from the given Package()
	 * @param instance
	 * @param tour
	 * @param durationToImprovement
	 * @param maxRuntime
	 * @param packingPlan
	 * @return
	 */
	public static TTPSolution ModifiedEA(TTPInstance instance, int[] tour, int durationToImprovement, int maxRuntime, int[] packingPlan){
		// parameter setup
		int index = 0;
		int counter = 0;
		// setup time clock
		long LS_Start_Time = System.currentTimeMillis();
		
		// setup current solution
		TTPSolution currentSolution = new TTPSolution(tour, packingPlan); 
		
		// evaluate current solution
		instance.evaluate(currentSolution);
		
		while(true) {
			// do the time check just every 50 iterations, as it is time consuming
			if((index % 10) == 0 &&  (System.currentTimeMillis()-LS_Start_Time) > maxRuntime) {
				break;
			}
			// setup new packing-plan
			int[] newPackingPlan = (int[])DeepCopy.copy(packingPlan);

			// flip the status
            for (int j=0; j<packingPlan.length; j++) {
            	// flip with probability 1/n
                if (Math.random()<1d/packingPlan.length)
                	// check if item available or not
                	if(instance.items[PackingPlan2Item(j,instance,tour)][1] <= 0) {
                		continue;
                	}
                	// flip status based on current status
                    if (newPackingPlan[j] == 0) {
                        newPackingPlan[j] = 1;
                    } else {
                        newPackingPlan[j] = 0;
                    }
            }
			
			// check out the new solution objective value
			TTPSolution NextSolution = new TTPSolution(tour, newPackingPlan);
			instance.evaluate(NextSolution);
			if (NextSolution.ob >= currentSolution.ob && NextSolution.wend >=0 ) {
				// update new solution
				packingPlan = newPackingPlan;
				currentSolution = NextSolution;
			}
			else{
				// roll back to old plan
				// increment improvement false counter
			}
			// increment total number of iteration counter
			index++;
		}
		return currentSolution;
	}
}
