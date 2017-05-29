package ttp.Utils;

import java.util.Vector;
/**
 * @author puzhiyao
 * 
 */
public class DataPool {
	/*
	 * Vector container for storing best objective
	 * value in current run
	 */
	public static Vector<double[]> bestOB;
	/*
	 * Vector container for storing computation time of 
	 * best objective value in current run
	 */
	public static Vector<long[]> bestCT;
	
	/**
	 * Constructor
	 * create new container
	 */
	public DataPool(){
		bestOB = new Vector<double[]>();
		bestCT = new Vector<long[]>();
	}
	/**
	 * Add current run best objective value into pool
	 * @param ob
	 */
	public void addOB(double[] ob) {
		bestOB.add(ob);
	}
	/**
	 * Add current run computation time into pool
	 * @param ct
	 */
	public void addCT(long[] ct) {
		bestCT.add(ct);
	}

}
