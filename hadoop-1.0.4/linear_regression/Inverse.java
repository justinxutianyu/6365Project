import org.apache.commons.math3.linear.*;

import java.io.*;
import java.util.*;
import java.lang.Math;

public class Inverse {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {		
		FileInputStream file = new FileInputStream("temp1/part-00000");
		BufferedReader reader = new BufferedReader(new InputStreamReader(file));

		int xRows = Integer.parseInt(args[0]);

		RealMatrix multiplication = MatrixUtils.createRealMatrix(xRows,xRows);
		
		//Parse training set
		for(int i=0 ; i<xRows; i++){
			String line = reader.readLine();
			StringTokenizer tokenizer = new StringTokenizer(line);
			
			double[] x = new double[xRows];
			if (tokenizer.nextToken().equals("multiplication")) {
				int row = Integer.parseInt(tokenizer.nextToken());
				for(int j=0; j<xRows; j++){
					x[j] = Double.parseDouble(tokenizer.nextToken());
				}
				multiplication.setRow(row, x);
			}
		}
		reader.close();

		RealMatrix inv_xt = new LUDecomposition(multiplication).getSolver().getInverse();

        	FileWriter fstream = new FileWriter("temp1/part-00000", true);
        	BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0 ; i<xRows; i++){
			String newLine = "inverse " + i;
			for (int j = 0; j < xRows; j++) {
				newLine = newLine + " " + inv_xt.getEntry(i,j);
			}
			newLine = newLine + "\n";
			out.write(newLine);
		}
		//fstream.close();
		out.close();
		
	}

}		


