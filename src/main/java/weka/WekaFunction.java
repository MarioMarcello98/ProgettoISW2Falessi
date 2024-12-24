package weka;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import java.io.FileReader;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.IOException;
public class WekaFunction{
    public void CSVToARFF(File CSVFile) throws IOException {
        System.out.println("path: " + CSVFile.getAbsolutePath());
        CSVLoader loader = new CSVLoader();
        loader.setSource(CSVFile);
        Instances data = loader.getDataSet();
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(CSVFile.getPath().substring(0, CSVFile.getPath().length()-4) + ".arff"));
        saver.writeBatch();
    }
}