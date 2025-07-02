package ttp.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author pz.yao
 * 
 */
public class Configuration {
	/*
	 * store current config argument
	 */
	public static String[] args = new String[9];
	/*
	 * store all target instance file names
	 */
	public static String[] instancesName = {
			"a280_n279_bounded-strongly-corr_01.ttp",
			"a280_n1395_uncorr-similar-weights_05.ttp",
			"a280_n2790_uncorr_10.ttp",
			"fnl4461_n4460_bounded-strongly-corr_01.ttp",
			"fnl4461_n22300_uncorr-similar-weights_05.ttp",
			"fnl4461_n44600_uncorr_10.ttp",
			"pla33810_n33809_bounded-strongly-corr_01.ttp",
			"pla33810_n169045_uncorr-similar-weights_05.ttp",
			"pla33810_n338090_uncorr_10.ttp",
	};

	/**
	 * Constructor
	 * read configuration info from config file
	 */
	public Configuration() {
		// setup new configuration file name
		String configFileName = "Config";

		// open configuration file
		File file = new File(configFileName);
		BufferedReader reader = null;
		// setup parameters' container
		String TTP_Instance_Folder = null;
		String Input_Instance = null;
		String Approach = null;
		String Number_of_Evaluation = null;
		String Time_Limit = null;
		String Dynamic_Item_Mode = null;
		String Dynamic_Item_Benchmark = null;
		String Generations = null;
		String runs = null;

		// start read configuration info from configuration file
		try {
			// setup new reader and text container
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			// start reading configuration file
			while ((text = reader.readLine()) != null) {
				// read line text
				// jump out if this line contains no key info
				if(text.equals("") || text.charAt(0) == '%' || text.equals("\t") || text.equals(" ") || text.charAt(0) == ' ') {
					// check to see this line 
					// is instruction comment
					continue;
				}
				else {
					if(text.contains("TTP_Instance_Folder")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							TTP_Instance_Folder = m.group(1);
						}					
					}
					else if(text.contains("Input_Instance")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Input_Instance = m.group(1);
						}	
					}
					else if(text.contains("Approach")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Approach = m.group(1);
						}	
					}
					else if(text.contains("Number_of_Evaluation")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Number_of_Evaluation = m.group(1);
						}
					}
					else if(text.contains("Time_Limit")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Time_Limit = m.group(1);
						}
					}
					else if(text.contains("Dynamic_Item_Mode")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Dynamic_Item_Mode = m.group(1);
						}
					}
					else if(text.contains("Dynamic_Item_Benchmark")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Dynamic_Item_Benchmark = m.group(1);
						}
					}
					else if(text.contains("Generations")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							Generations = m.group(1);
						}
					}
					else if(text.contains("runs")) {
						// setup scan pattern
						String regex = "\"([^\"]*)\"";
						// set pattern to read between double quotes
						Pattern pat = Pattern.compile(regex);
						Matcher m = pat.matcher(text);
						if(m.find()) {
							// find instance folder name between quotes
							runs = m.group(1);
						}
					}
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

		// setup arguments
		args[0] = TTP_Instance_Folder;
		args[1] = Input_Instance;
		args[2] = Approach;
		args[3] = Number_of_Evaluation;
		args[4] = Time_Limit;
		args[5] = Dynamic_Item_Mode;
		args[6] = Dynamic_Item_Benchmark;
		args[7] = Generations;
		args[8] = runs;
	}

	/**
	 * Get current config arguments
	 * @return
	 */
	public String[] getConfig() {
		return this.args;
	}
	/**
	 * Get target instance file name
	 * @param index
	 * @return
	 */
	public String getInstanceName(int index) {
		return this.instancesName[index];
	}
}
