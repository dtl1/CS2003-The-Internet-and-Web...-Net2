/**
 * Simple example creating a directory and a file.
 * <p>
 * Saleem Bhatti, https://saleem.host.cs.st-andrews.ac.uk/
 * 28 Aug 2019
 */

import java.io.*;

public class DirAndFile {

    /**
     * Class provided by studres, creates directory and writes message data to files
     * @param args directory name, filename and text of message to be written
     */
    public static void main(String[] args) {

        String dirName = args[1];
        String fileName = args[0];
        String text = args[2];

        File dir = new File(dirName);

        if (dir.exists()) {
            System.out.println("++ Directory already exists: " + dirName);
            System.out.println("++ Creating file in pre-existent directory...");
        } else {
            if (dir.mkdir()) {
                System.out.println("++ Created directory: " + dirName);
            } else {
                System.out.println("++ Failed to create directory: " + dirName);
                System.exit(0);
            }
        }

        fileName = dirName + File.separator + fileName;
        File file = new File(fileName);

        if (file.exists()) {
            System.out.println("++ File already exists: " + fileName);
            System.exit(0);
        }

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(text);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            System.out.println("IOException - write(): " + e.getMessage());
        }

        System.out.println("++ Wrote \"" + text + "\" to file: " + fileName);
    }
}
