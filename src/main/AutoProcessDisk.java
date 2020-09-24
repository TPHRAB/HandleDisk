package main;
// Timmy Zhao

// 08/19/19

// Github---extensions

// AutoProcessDisk class can 1. move videos in a disk to desktop 2. rename videos from disks to disks' name

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;

import progressbar.ProgressBarFrame;

import static extensions.file.fileUtils.mergeSortOnFilesNames;

public class AutoProcessDisk {

	public static final String DIRECTORY_SEPERATOR =
			System.getProperty("os.name").toLowerCase().contains("win") ? "\\" : "/";
	public static final String PROGRAM =
			System.getProperty("os.name").toLowerCase().contains("win") ? "./ffmpeg.exe" : "./ffmpeg";

	public static void moveVideosFromDisk(File in, File out, JFrame rootFrame) throws Exception {
		// get destination
		int count = 0;
		for (File f : out.listFiles()) {
			if (f.isDirectory()) {
				count++;
			}
		}
		File dir = new File(out.getAbsolutePath() + DIRECTORY_SEPERATOR + (count + 1));
		dir.mkdir();

		// get videos's directory and copy to destination
		File[] list = in.listFiles();
		list = extensions.file.fileUtils.mergeSortOnFilesNames(list);
		List<File> videosSource = new ArrayList<>();
		List<File> videosDestination = new ArrayList<>();
		for (File f : list) {
			if (f.isHidden() || !isFileQualified(f, videosSource)) {
				continue;
			}
			File result = new File(dir.getAbsolutePath() + DIRECTORY_SEPERATOR + f.getName());
			videosSource.add(f);
			videosDestination.add(result);
		}
		convertVideos(videosSource, videosDestination, rootFrame);
	}

	public static void convertVideos(List<File> source,
			List<File> out, JFrame rootFrame) throws IOException, InterruptedException {
		// initialize command
		List<String> command = new ArrayList<>();
		command.add(PROGRAM);
		command.add("-i");
		command.add("-b:v");
		command.add("500k");
		command.add("-r");
		command.add("25");
		command.add("-s");
		command.add("320*180");
	
		// initialize frame
		ProgressBarFrame pbf = new ProgressBarFrame();
		pbf.initPB("Convert Videos", source.size());
		JTextArea area = pbf.getTextArea();
		JProgressBar pb = pbf.getPB();
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				for (int i = 0; i < source.size(); i++) {
					// add source and output path
					command.add(2, source.get(i).getAbsolutePath());
					command.add(out.get(i).getAbsolutePath());
					// set progress
					processStart(command, area);
					pb.setValue(pb.getValue() + 1);
					// remove source and output path
					command.remove(command.size() - 1);
					command.remove(2);
				}
				pbf.enableFinish();
				rootFrame.setVisible(true);
				rootFrame.requestFocus();
				pbf.requestFocus();
				return null;
			}
		}.execute();
	}

	public static void sortVideos(File in) {
		// get prefix for videos
		String prefix = in.getName() + "_";

		// process videos
		File[] list = in.listFiles();
		list = mergeSortOnFilesNames(list);
		for (File dir : list) {
			if (dir.isHidden()) {
				continue;
			}
			File[] videos = dir.listFiles();
			videos = mergeSortOnFilesNames(videos);
			for (File vob : videos) {
				if (vob.isHidden()) {
					continue;
				}
				String fileName = vob.getName().substring(vob.getName().indexOf('_') + 1);
				vob.renameTo(new File(in.getAbsolutePath()
						+ DIRECTORY_SEPERATOR + prefix + dir.getName() + "_" + fileName));
			}
			dir.delete();
		}
	}

	public static void combineVideos(File source,
			File out, JFrame rootFrame) throws IOException, InterruptedException {
		// ffmpeg command
		List<String> command = new ArrayList<>();
		command.add(PROGRAM);
		command.add("-i");
		command.add("-c");
		command.add("copy");
		File[] list = extensions.file.fileUtils.mergeSortOnFilesNames(source.listFiles());
		
		// initialize frame
		ProgressBarFrame pbf = new ProgressBarFrame();
		pbf.setTitle("Convert Videos");
		pbf.initPB("Combine Videos", list.length);
		JTextArea area = pbf.getTextArea();
		JProgressBar pb = pbf.getPB();
		new SwingWorker<Void, Void>() {
			protected Void doInBackground() throws Exception {
				String firstIndex = "";
				String filesToConcat = "";
				int namingIndex = 1;
				String diskNumber = "1";
				for (int i = 0; i < list.length; i++) {
					File f = list[i];
					if (f.isHidden() || f.isDirectory()
							|| !f.getName().split("\\.")[1].equals("VOB")) {
						pb.setValue(pb.getValue() + 1);
						continue;
					}
					String regex1 = ".*_.*_\\d*_\\d.VOB";
					String[] namingParts = f.getName().split("_");
					if (!namingParts[1].equals(diskNumber)) {
						namingIndex = 1;
						diskNumber = namingParts[1];
					}
		
					if (firstIndex.isEmpty()) {
						firstIndex = namingParts[2];
						filesToConcat = "concat:" + f.getAbsolutePath();
						command.add(out.getAbsolutePath() + DIRECTORY_SEPERATOR + f.getName()
								.substring(0, f.getName().lastIndexOf('_') - 2)
								+ namingIndex + ".mp4");
						namingIndex++;
					} else if (f.getName().matches(regex1)
							&& firstIndex.equals(f.getName().split("_")[2])) {
						filesToConcat = filesToConcat + "|" + f.getAbsolutePath();
					} else {
						command.add(2, filesToConcat);
						// set progress
						processStart(command, area);
						
						command.remove(command.size() - 1);
						command.remove(2);
						firstIndex = "";
						filesToConcat = "";
						i--; // not going forward
					}
					pb.setValue(pb.getValue() + 1);
				}
				// if last file's index is the same as the file before it, then it need to add manually
				if (!firstIndex.isEmpty()) {
					command.add(2, filesToConcat);
					// set progress
					processStart(command, area);
					pb.setValue(pb.getValue() + 1);
				}
				pbf.enableFinish();
				rootFrame.setVisible(true);
				rootFrame.requestFocus();
				pbf.requestFocus();
				return null;
			}
		}.execute();
	}

	public static void processStart(List<String> command, JTextArea area)
			throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command).start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (area.getLineCount() >= 400) {
				try {
					area.replaceRange("", 0, area.getLineEndOffset(200));
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			area.append(line + "\r\n");
		} 
		reader.close();
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((line = reader.readLine()) != null) {
			if (area.getLineCount() >= 400) {
				try {
					area.replaceRange("", 0, area.getLineEndOffset(200));
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			area.append(line + "\r\n");
		}
		reader.close();
	}
	
	public static boolean isFileQualified(File target, List<File> videoSource) throws IOException {
		if (!target.getName().matches("VTS_.*_[1-2].VOB")) {
			return false;
		}

		char targetIndex = target.getName().charAt(target.getName().indexOf('.') - 1);
		if (targetIndex == '2') {
			String lastIndex = videoSource.get(videoSource.size() - 1).getName().split("_")[1];
			return videoSource.size() != 0 && lastIndex.equals(target.getName().split("_")[1]);
		}
		
        List<String> command = new ArrayList<>();
        command.add("./ffmpeg.exe");
        command.add("-i");
        command.add(target.getAbsolutePath());

        // get information
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(p.getErrorStream()));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }

        Pattern regex = Pattern.compile("Duration: (.*?),");
        Matcher m = regex.matcher(buffer.toString());
        return m.find() && !m.group(1).split(":")[1].matches("0\\d");
    }
}
