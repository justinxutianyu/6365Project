package mapReduce.linearRegression;

import java.io.IOException;
import java.util.*;
import java.lang.Math;
 	
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
 	
public class MapReduceLinearRegression {

	static int splitValue = 3;//463715
	static double a = 1;//463715
	static double c = 0.5;//463715
 	
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = value.toString();
	 		StringTokenizer tokenizer = new StringTokenizer(line, ",");
	 		String predictedValueString = tokenizer.nextToken();
			double predictedValue = Double.parseDouble(predictedValueString);
			tokenizer.nextToken();
			String centeroid = null;
			double sum = 0.0;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				sum += Float.parseFloat(token);			
			}
			double difference = predictedValue - sum;
			double regressionValue = Math.pow(difference, 2.0);
			
			output.collect(new Text("Sqaured Difference"), new DoubleWritable(regressionValue));
 	 	}
	}
 	
	public static class Reduce extends MapReduceBase implements Reducer<Text, DoubleWritable, Text, DoubleWritable> {
 		public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			double finalValue = 0.0;
			while (values.hasNext()) {
				finalValue += values.next().get();
			}
			output.collect(new Text("Final Value:"), new DoubleWritable(finalValue));
 		}
	}

	public static class SplitMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
	 		StringTokenizer tokenizer = new StringTokenizer(line, ",");
			String yValue = tokenizer.nextToken();
			//if (key.compareTo(new LongWritable(splitValue)) <= 0) {
				output.collect(new Text("y"), new Text(yValue));
			//} else {
				//output.collect(new Text("testy " + key.toString()), new Text(yValue));
			//}
			String xValue = tokenizer.nextToken();
			while (tokenizer.hasMoreTokens()) {
				xValue = xValue + " " + tokenizer.nextToken();
			}
			//if (key.compareTo(new LongWritable(splitValue)) <= 0) {
				output.collect(new Text("x"), new Text(xValue));
			//} else {
			//	output.collect(new Text("testx " + key.toString()), new Text(xValue));
			//}
			
 	 	}
	}
 	
	public static class SplitReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			//String finalValue = values.next().toString();
			int i = 0;
			while (values.hasNext()) {
				if (i < splitValue) {
					output.collect(new Text("train"+ key + " " + i), values.next());
				} else {
					output.collect(new Text("test" + key + " " + (i - splitValue)), values.next());
				}
				i++;
				//finalValue += ("," + values.next().toString());
			}
			//String matrix = new StringTokenizer(key.toString()).nextToken();

 		}
	}

	public static class WeightsDifferenceMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			//Save the matrix line
	 		StringTokenizer tokenizer = new StringTokenizer(line);
			String matrix = tokenizer.nextToken();
			String row = tokenizer.nextToken();
			String valueString = "";
			while (tokenizer.hasMoreTokens()) {
				valueString = valueString + " " + tokenizer.nextToken();
			}
			output.collect(new Text(matrix + " " + row), new Text(valueString));
			//Calculate weight difference
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();		
			row = tokenizer.nextToken();
			if (matrix.equals("trainx")) {
				output.collect(new Text(row), new Text(line));				
			}
			if (matrix.equals("testx")) {
				for (int i = 0; i < splitValue; i++) {
					output.collect(new Text(Integer.toString(i)), new Text(line));				
				}
			}
 	 	}
	}
 	
	public static class WeightsDifferenceReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				output.collect(key, values.next());
			} else {
				String line1 = values.next().toString();
				String line2 = values.next().toString();
	 			StringTokenizer tokenizer1 = new StringTokenizer(line1);
				StringTokenizer tokenizer2 = new StringTokenizer(line2);
				String matrix1 = tokenizer1.nextToken();
				String matrix2 = tokenizer2.nextToken();
				String row1 = tokenizer1.nextToken();
				String row2 = tokenizer2.nextToken();
				ArrayList<Double> list = new ArrayList<Double>();
				while (tokenizer1.hasMoreTokens()) {
					if (matrix1.contains("train")) {
						list.add(Double.parseDouble(tokenizer2.nextToken()) - Double.parseDouble(tokenizer1.nextToken()));
					} else {
						list.add(Double.parseDouble(tokenizer1.nextToken()) - Double.parseDouble(tokenizer2.nextToken()));
					}
				}
				String row = (matrix1.contains("train") ? row1 : row2);
				String matrixLine = "";
				for (int i = 0; i < list.size(); i++) {
					matrixLine = matrixLine + " " + list.get(i);
				}
				output.collect(new Text("weight " + row), new Text(matrixLine));
			}			

 		}
	}

	public static class TransposeWeightMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			//Save the matrix line
	 		StringTokenizer tokenizer = new StringTokenizer(line);
			String matrix = tokenizer.nextToken();
			String row = tokenizer.nextToken();
			String valueString = "";
			while (tokenizer.hasMoreTokens()) {
				valueString = valueString + " " + tokenizer.nextToken();
			}
			output.collect(new Text(matrix + " " + row), new Text(valueString));
			//Transpose trainx
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weight")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeWeightReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				//String finalValue = values.next().toString();
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
					//finalValue += (" " + );
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("transposedWeight " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class WeightsMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			//Save the matrix line
	 		StringTokenizer tokenizer = new StringTokenizer(line);
			String matrix = tokenizer.nextToken();
			String row = tokenizer.nextToken();
			String valueString = "";
			if (matrix.contains("train") || matrix.contains("test")) {
				while (tokenizer.hasMoreTokens()) {
					valueString = valueString + " " + tokenizer.nextToken();
				}
			output.collect(new Text(matrix + " " + row), new Text(valueString));
			} else {

			}	
			//Calculate weight difference
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();		
			row = tokenizer.nextToken();
			if (matrix.equals("trainx")) {
				output.collect(new Text(row), new Text(line));				
			}
			if (matrix.equals("testx")) {
				for (int i = 0; i < splitValue; i++) {
					output.collect(new Text(Integer.toString(i)), new Text(line));				
				}
			}
 	 	}
	}
 	
	public static class WeightsReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				output.collect(key, values.next());
			} else {
				String line1 = values.next().toString();
				String line2 = values.next().toString();
	 			StringTokenizer tokenizer1 = new StringTokenizer(line1);
				StringTokenizer tokenizer2 = new StringTokenizer(line2);
				String matrix1 = tokenizer1.nextToken();
				String matrix2 = tokenizer2.nextToken();
				String row1 = tokenizer1.nextToken();
				String row2 = tokenizer2.nextToken();
				ArrayList<Double> list = new ArrayList<Double>();
				while (tokenizer1.hasMoreTokens()) {
					if (matrix1.contains("train")) {
						list.add(Double.parseDouble(tokenizer2.nextToken()) - Double.parseDouble(tokenizer1.nextToken()));
					} else {
						list.add(Double.parseDouble(tokenizer1.nextToken()) - Double.parseDouble(tokenizer2.nextToken()));
					}
				}
				String row = (matrix1.contains("train") ? row1 : row2);
				double sum = 0;
				for (int i = 0; i < list.size(); i++) {
					sum = sum + Math.pow(list.get(i), 2);
				}
				double gauessianKernel = a * (sum/((-2.0 * Math.pow(c, 2))));
				output.collect(new Text("weight " + row), new Text(Double.toString(gauessianKernel)));
			}			

 		}
	}

	public static class TransposeTrainYMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 		private final static IntWritable one = new IntWritable(1);
 	        private Text word = new Text();
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			//Save the matrix line
	 		StringTokenizer tokenizer = new StringTokenizer(line);
			String matrix = tokenizer.nextToken();
			String row = tokenizer.nextToken();
			String valueString = "";
			while (tokenizer.hasMoreTokens()) {
				valueString = valueString + " " + tokenizer.nextToken();
			}
			output.collect(new Text(matrix + " " + row), new Text(valueString));
			//Transpose trainy
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("trainy")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeTrainYReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				//String finalValue = values.next().toString();
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
					//finalValue += (" " + );
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("transposedTrainY " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class WeightTimesXMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 	
 	        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			//Save the matrix line
	 		StringTokenizer tokenizer = new StringTokenizer(line);
			String matrix = tokenizer.nextToken();
			String row = tokenizer.nextToken();
			String valueString = "";
			while (tokenizer.hasMoreTokens()) {
				valueString = valueString + " " + tokenizer.nextToken();
			}
			output.collect(new Text(matrix + " " + row), new Text(valueString));
			//Find the coordinates for each part of the new matrix
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weight") || matrix.equals("trainx")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(row + " " + new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}			
 	 	}
	}
 	
	public static class WeightTimesXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			//if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			/*} else {
				//String finalValue = values.next().toString();
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
					//finalValue += (" " + );
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("transposedTrainY " + key.toString()), new Text(finalValue));
			}*/
 		}
	}
 	
	public static void main(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(new Configuration());
		fs.delete(new Path(args[1]), true);

		fs.delete(new Path(args[1]), true);
		fs.delete(new Path("temp1"), true);
		fs.delete(new Path("temp2"), true);

		//Organize the data
		JobConf conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Split Data");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.SplitMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.SplitReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
	 	FileInputFormat.setInputPaths(conf, new Path(args[0]));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Calculate Weights
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Calculate Weights");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.WeightsMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.WeightsReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//transpose testy Weights
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainy");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeTrainYMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeTrainYReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Weights time x
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainy");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.WeightTimesXMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.WeightTimesXReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

	 }
 }
 	
