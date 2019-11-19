package test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

public class Test {
	public static void main(String[] args) throws IOException {
		System.out.println("0");
		SwingWorker<Void, Void> thread = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				System.out.println("1");
				return null;
			}
			@Override
			protected void done() {
				System.out.println("Done");
			}	
		};
		thread.execute();
		try {
			thread.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void recursiveHelper(File source, int[] counter) {
		for (File f : source.listFiles()) {
			String regex1 = ".*_.*_[1-2]\\.VOB";
			String regex2 = ".*_TS\\.VOB";
			if (f.isDirectory()) {
				recursiveHelper(f, counter);
			} else if (f.getName().matches(regex1) || f.getName().matches(regex2)) {
				counter[1]++;
			} else {
				System.out.println(f.getName());
			}
			counter[0]++;
		}
	}
}
