package ieeei2010;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Will parse the files produced by ParameterSetTesting.
 *
 * @author vekk
 */
public class ConvergenceAndGeneralizationAndStuff {

    public static void main(String[] args) throws IOException, Exception {
        String path = "../Test/";
        String[] datasetNames = {
            "autoprice.names",
            "bank32nh"
        };



        File folder = new File(path);
        File[] datasets = folder.listFiles();

        File fajl = datasets[0];
        System.out.println("file : " + fajl.getName());
        FileInputStream fis = new FileInputStream(fajl);
        DataInputStream din = new DataInputStream(fis);
        BufferedReader br = new BufferedReader(new InputStreamReader(din));
        // i'll read it as a string because of delimiters and transform into
        // doubles.
        String line;
      //  Vector
        while((line = br.readLine()) != null) {
            Double d = new Double(line);
            System.out.println(d);
        }

    }
}
