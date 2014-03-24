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

	static int splitValue = 5;//463715;
	static int cutoff = 3;//10000;
	static double a = 1;//463715
	static double c = 0.5;//463715
	static int xRows = 2;//16;
	static int yRows;
 	
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
					if (i < cutoff) {
						output.collect(new Text("train"+ key + " " + i), values.next());
					} else {
						values.next();
					}
				} else {
					if ((i - splitValue) < cutoff) {
						output.collect(new Text("test" + key + " " + (i - splitValue)), values.next());
					} else {
						values.next();
					}
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
				return;
			}	
			//Calculate weight difference
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();		
			row = tokenizer.nextToken();
			if (matrix.equals("trainx")) {
				output.collect(new Text(row), new Text(line));	
			}
			if (matrix.equals("testx")) {
				for (int i = 0; i < cutoff; i++) {
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

	public static class TransposeWeightXMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (!matrix.contains("weightTimesX")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose trainy
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weightTimesX")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeWeightXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
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
				output.collect(new Text("transposedWeightTimesX " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class TransposeTrainXMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (matrix.equals("trainx")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeTrainXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
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
				output.collect(new Text("transposedTrainX " + key.toString()), new Text(finalValue));
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
				int column = 0;	
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(row), new Text(matrix + " " + column + " " + tokenizer.nextToken()));
					column++;
				}
			}			
 	 	}
	}
 	
	public static class WeightTimesXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				//String finalValue = values.next().toString();
				double weight = 0.0;
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					String matrix = tokens.nextToken();
					if (matrix.equals("weight")) {
						tokens.nextToken();
						weight = Double.parseDouble(tokens.nextToken());
					} else {
						Integer position = Integer.parseInt(tokens.nextToken());
						String val = tokens.nextToken();
						items.put(position, val);
					}
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + (Double.parseDouble(items.get(i)) * weight);
				}
				output.collect(new Text("weightTimesX " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class TransposeWeightTimesXMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (!matrix.contains("weightTimesX")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose trainy
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weightTimesX")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeWeightTimesXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
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
				output.collect(new Text("transposedWeighTimesX " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class TransposeXTimesTransposeWeightTimesXMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (!matrix.contains("transposedWeighTimesX")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose Save line to each coordinates
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("transposedWeighTimesX")) {	
		 		long column = 0;
				while (column < xRows) {
					output.collect(new Text(column + " " + row), new Text(line));
					column++;			
				}
			}
			if (matrix.equals("transposedTrainX")) {	
		 		long column = 0;
				while (column < xRows) {
					output.collect(new Text(row + " " + column), new Text(line));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeXTimesTransposeWeightTimesXReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
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
				double sum = 0.0;
				while (tokenizer1.hasMoreTokens()) {
					sum += (Double.parseDouble(tokenizer1.nextToken()) * Double.parseDouble(tokenizer2.nextToken()));
				}
				output.collect(key, new Text(Double.toString(sum)));
			}
 		}
	}

	public static class RecombineMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 	
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
			if (matrix.contains("t") && !matrix.equals("transposedWeighTimesX")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Grab the partial matrix and group by column
	 		tokenizer = new StringTokenizer(line);
			row = tokenizer.nextToken();
			String column = tokenizer.nextToken();
			if (!row.contains("t")) {	
		 		output.collect(new Text(row), new Text(column + " " + tokenizer.nextToken()));
			}
			
 	 	}
	}
 	
	public static class RecombineReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("multiplication " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class WeightTimesYTransposeMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 	
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
			if (!key.toString().equals("multiplication")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Find the coordinates for each part of the new matrix
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weight") || matrix.equals("trainy")) {
				int column = 0;	
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(row), new Text(matrix + " " + column + " " + tokenizer.nextToken()));
					column++;
				}
			}			
 	 	}
	}
 	
	public static class WeightTimesYTransposeReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t") || key.toString().contains("i")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				//String finalValue = values.next().toString();
				double weight = 0.0;
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					String matrix = tokens.nextToken();
					if (matrix.equals("weight")) {
						tokens.nextToken();
						weight = Double.parseDouble(tokens.nextToken());
					} else {
						Integer position = Integer.parseInt(tokens.nextToken());
						String val = tokens.nextToken();
						items.put(position, val);
					}
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + (Double.parseDouble(items.get(i)) * weight);
				}
				output.collect(new Text("weightTimesTransposedY " + key.toString()), new Text(finalValue));

			}
 		}
	}

	public static class TransposeWeightTimesTransposeYMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (!matrix.contains("weightTimesTransposedY")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose trainy
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("weightTimesTransposedY")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeWeightTimesTransposeYReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t") || key.toString().contains("i")) {
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
				output.collect(new Text("transposedWeightTimesTransposedY " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class TransposeXTimesTransposeWeightTimesYMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (!matrix.contains("transposedWeightTimesTransposedY")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose Save line to each coordinates
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("transposedWeightTimesTransposedY")) {
		 		long column = 0;
				while (column < xRows) {
					output.collect(new Text(column + " " + row), new Text(line));
					column++;			
				}
			}
			if (matrix.equals("transposedTrainX")) {	
		 		long column = 0;
				while (column < 1) {
					output.collect(new Text(row + " " + column), new Text(line));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeXTimesTransposeWeightTimesYReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t") || key.toString().contains("i")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
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
				double sum = 0.0;
				while (tokenizer1.hasMoreTokens()) {
					sum += (Double.parseDouble(tokenizer1.nextToken()) * Double.parseDouble(tokenizer2.nextToken()));
				}
				output.collect(key, new Text(Double.toString(sum)));
			}
 		}
	}

	public static class Recombine2Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 	
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
			if (matrix.equals("inverse") || matrix.equals("testx")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Grab the partial matrix and group by column
	 		tokenizer = new StringTokenizer(line);
			row = tokenizer.nextToken();
			String column = tokenizer.nextToken();
			if (!row.contains("t") && !row.contains("i")) {	
		 		output.collect(new Text(row), new Text(column + " " + tokenizer.nextToken()));
			}
			
 	 	}
	}
 	
	public static class Recombine2Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t") || key.toString().contains("i")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("newMultiplication " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class TransposeNewMultiplicationMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (matrix.equals("inverse") || matrix.contains("testx")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose trainy
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("newMultiplication")) {	
		 		long column = 0;
				while (tokenizer.hasMoreTokens()) {
					output.collect(new Text(new Long(column).toString()), new Text(row + " " + tokenizer.nextToken()));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class TransposeNewMultiplicationReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("inverse") || key.toString().contains("testx")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				//String finalValue = values.next().toString();
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				while (values.hasNext()) {
					String test = values.next().toString();
					StringTokenizer tokens = new StringTokenizer(test);
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
					//finalValue += (" " + );
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("transposedNewMultiplication " + key.toString()), new Text(finalValue));
			}
 		}
	}

	public static class InverseTimesNewMultiplicationMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
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
			if (matrix.contains("testx")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Transpose Save line to each coordinates
	 		tokenizer = new StringTokenizer(line);
			matrix = tokenizer.nextToken();
			row = tokenizer.nextToken();
			if (matrix.equals("transposedNewMultiplication")) {
		 		long column = 0;
				while (column < xRows) {
					output.collect(new Text(column + " " + row), new Text(line));
					column++;			
				}
			}
			if (matrix.equals("inverse")) {	
		 		long column = 0;
				while (column < 1) {
					output.collect(new Text(row + " " + column), new Text(line));
					column++;			
				}
			}
			
 	 	}
	}
 	
	public static class InverseTimesNewMultiplicationReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("testx")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
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
				double sum = 0.0;
				while (tokenizer1.hasMoreTokens()) {
					sum += (Double.parseDouble(tokenizer1.nextToken()) * Double.parseDouble(tokenizer2.nextToken()));
				}
				output.collect(key, new Text(Double.toString(sum)));
			}
 		}
	}

	public static class Recombine3Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
 	
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
			if (matrix.equals("testx")) {
				output.collect(new Text(matrix + " " + row), new Text(valueString));
			}
			//Grab the partial matrix and group by column
	 		tokenizer = new StringTokenizer(line);
			row = tokenizer.nextToken();
			String column = tokenizer.nextToken();
			if (!row.contains("t")) {
		 		output.collect(new Text(row), new Text(column + " " + tokenizer.nextToken()));
			}
			
 	 	}
	}
 	
	public static class Recombine3Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
 		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			if (key.toString().contains("t")) {
				while(values.hasNext()) {
					output.collect(key, values.next());
				}
			} else {
				HashMap<Integer, String> items = new HashMap<Integer, String>();
				
				while (values.hasNext()) {
					StringTokenizer tokens = new StringTokenizer(values.next().toString());
					Integer position = Integer.parseInt(tokens.nextToken());
					String val = tokens.nextToken();
					items.put(position, val);
				}
				String finalValue = "";
				for (int i = 0; i < items.size(); i++) {
					finalValue = finalValue + " " + items.get(i);
				}
				output.collect(new Text("betas " + key.toString()), new Text(finalValue));
			}
 		}
	}
 	
	public static void main(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(new Configuration());
		fs.setVerifyChecksum(false);	

		fs.delete(new Path(args[1]), true);
		fs.delete(new Path("temp1"), true);
		fs.delete(new Path("temp2"), true);

		//Organize the data
		/*JobConf conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Split Data");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.SplitMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.SplitReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
	 	FileInputFormat.setInputPaths(conf, new Path(args[0]));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
		JobClient.runJob(conf);*/
	 	

		//Calculate Weights
		JobConf conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Calculate Weights");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.WeightsMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.WeightsReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path(args[0]));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//transpose testy Weights
		/*conf = new JobConf(MapReduceLinearRegression.class);
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
	 	
		JobClient.runJob(conf);*/

		//Weights time x
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainy");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.WeightTimesXMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.WeightTimesXReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Transpose trainx
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainx");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeTrainXMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeTrainXReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);


		//Transpose weightTimesX
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainx");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeWeightTimesXMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeWeightTimesXReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Multiply
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose trainx");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeXTimesTransposeWeightTimesXMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeXTimesTransposeWeightTimesXReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//Recombine
		//Multiply
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Recombine");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.RecombineMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.RecombineReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);
		//Calculate inverse

		Process p;
		try {
			p = Runtime.getRuntime().exec("java -Xmx2048m -cp ./commons-math3-3.2/commons-math3-3.2.jar:./linear_regression/ Inverse " + xRows);
			p.waitFor();	
		} catch (Exception e) {
			e.printStackTrace();
		}

		//Weight times transposed trainy
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression weight times transposed y");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.WeightTimesYTransposeMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.WeightTimesYTransposeReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//Transpose weightTimesYTranspose
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose transpose weight 2");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeWeightTimesTransposeYMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeWeightTimesTransposeYReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Multiply
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose multiply 2");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeXTimesTransposeWeightTimesYMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeXTimesTransposeWeightTimesYReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//Recombine
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose recombine 2");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.Recombine2Map.class);
	 	conf.setReducerClass(MapReduceLinearRegression.Recombine2Reduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Transpose 
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose transpose new multiplication");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.TransposeNewMultiplicationMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.TransposeNewMultiplicationReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);

		//Multiply inverse and above
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Transpose inverse new multiplication");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.InverseTimesNewMultiplicationMap.class);
	 	conf.setReducerClass(MapReduceLinearRegression.InverseTimesNewMultiplicationReduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp1"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp2"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp1"));
	 	
		JobClient.runJob(conf);

		//Recombine
		conf = new JobConf(MapReduceLinearRegression.class);
	 	conf.setJobName("Linear Regression Recombine 3");
	 	conf.setOutputKeyClass(Text.class);
	 	conf.setOutputValueClass(Text.class);	
 
	 	conf.setMapperClass(MapReduceLinearRegression.Recombine3Map.class);
	 	conf.setReducerClass(MapReduceLinearRegression.Recombine3Reduce.class);
	 	
	 	conf.setInputFormat(TextInputFormat.class);
	 	conf.setOutputFormat(TextOutputFormat.class);
	 	
		fs.delete(new Path("temp2"), true);
	 	FileInputFormat.setInputPaths(conf, new Path("temp1"));
	 	FileOutputFormat.setOutputPath(conf, new Path("temp2"));
	 	
		JobClient.runJob(conf);


	 }
 }
 	
