#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static ${package}.utils.Utils.*;

import ${package}.utils.CloseableIterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

public class Main {

	public static final String APP_NAME = "${artifactId}";

	public static void main(String[] args) throws Exception {
		String master;

		if (args.length > 0) {
			master = args[0];
		} else {
			master = "local[*]";
		}

		long start = System.currentTimeMillis();
		SparkConf conf = new SparkConf().setAppName(APP_NAME).setMaster(master);

		// Tries to determine necessary jar
		String source = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		if (source.endsWith(".jar")) {
			conf.setJars(new String [] {source});
		}

		try (JavaSparkContext sc = new JavaSparkContext(conf)) {

			// Do your analysis here starting with sc
		}

		long s = (System.currentTimeMillis() - start) / 1000;
		String dur = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
		System.out.println("Analysis completed in " + dur);
	}
}
