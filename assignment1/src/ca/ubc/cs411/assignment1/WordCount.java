package ca.ubc.cs411.assignment1;

import java.io.File;

public class WordCount {

	private static long sum;
	
	public static void main(String[] args) {
		sum = 0;
		for (int i = 0; i < args.length; i++)
			printInfo(args[i]);
		System.out.println(String.format("Sum: %8d", sum));
	}

	static void printInfo(String filename) {
		try {
			// open the file
			File file = new File(filename);
			if (!file.exists()) {
				System.err.println("error: file does not exist");
				return;
			}
			if (!file.isFile()) {
				System.err.println("error: file is not a normal file");
				return;
			}
			// print formatted length + filename
			long length = file.length();
			sum += length;
			printLength(length);
			System.out.println(" " + file.getName());
			
		} catch (NullPointerException e) {
			System.err.println("error: pathname argument is null.");
			return;
		} catch (SecurityException e) {
			System.err.println("error: read access denied");
			return;
		}
	}
	
	static void printLength(long length) {
		System.out.print(String.format("%8d", length));
	}
}
