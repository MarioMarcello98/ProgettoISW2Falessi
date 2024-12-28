package utils;
import java.io.BufferedWriter;
import entities.Release;
import entities.Class;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
public class CSV {
    public enum Type {
        TRAINING_SET,
        TESTING_SET
    }
    private static String FIRST_ROW = "Version, File Name, LOC, NAuth, NFix, Revisions, LOCAdded, MaxLOCAdded, averageLOCAdded, Churn, MaxChurn, AverageChurn, Age, Buggy";
    public static void generateCSV(List<Class> classes, String projName, int numVersions) throws IOException {
        System.out.println("Generating the CSV file for " + projName + ", numVersions = " + numVersions);
        File file = new File(projName + ".csv");
        PopulateFile(classes, file);
    }
    public static File generateCSVForWF(Type type, List<Class> classes, String projName, int iteration) throws IOException {
        if (type == Type.TRAINING_SET) {
            File trainFile = new File("/home/MarioMarcello98/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.csv");
            trainFile.getParentFile().mkdirs();
            trainFile.createNewFile();
            PopulateFile(classes, trainFile);
            return trainFile;
        }
        else if (type == Type.TESTING_SET) {
            File testFile = new File("/home/MarioMarcello98/ISW2/" + projName + "_" + iteration + "/" + projName + "_" +  iteration + "_testing-set.csv");
            testFile.getParentFile().mkdirs();
            testFile.createNewFile();
            PopulateFile(classes, testFile);
            return testFile;
        }
        return null;
    }

    private static void PopulateFile(List<Class> classes, File file) throws IOException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append(FIRST_ROW);
            bufferedWriter.append("\n");
            for (Class c : classes) {
                String row = c.getRelease().getId() + ", " +
                        c.getName() + ", " +
                        c.getSize() + ", " +
                        c.getNAuth() + ", " +
                        c.getNFix() + ", " +
                        c.getNR() + ", " +
                        c.getLOCAdded() + ", " +
                        c.getMaxLOCAdded() + ", " +
                        c.getAverageLOCAdded() + ", " +
                        c.getChurn() + ", " +
                        c.getMaxChurn() + ", " +
                        c.getAverageChurn() + ", " +
                        c.getAge() + ", " +
                        c.isBuggy();
                bufferedWriter.append(row);
                bufferedWriter.append("\n");
            }
        }
        finally {
            try {
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
    public static List<File> generateCSVForWF(List<Class> classesForTrainSet, List<Class> classesForTestSet, String projName, int iteration) throws IOException {
        // outputFiles[0] = training set file, outputFiles[1] = testing set file
        List<File> outputFiles = new ArrayList<>();
        if (!classesForTrainSet.isEmpty()) {
            File trainFile = new File("/home/MarioMarcello98/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.csv");
            if (trainFile.getParentFile().mkdirs())
                System.out.println("Training directory created");
            else
                System.out.println("Training directory cannot be created");
            trainFile.createNewFile();
            PopulateFile(classesForTrainSet, trainFile);
            outputFiles.add(0, trainFile);
        }
        File testFile = new File("/home/MarioMarcello98/ISW2/" + projName + "_" + iteration + "/" + projName + "_" +  iteration + "_testing-set.csv");
        if (testFile.getParentFile().mkdirs()) {
            System.out.println("Testing directory created");
        }
        else {
            System.out.println("Testing directory cannot be created");
        }
        testFile.createNewFile();
        outputFiles.add(testFile);
        PopulateFile(classesForTestSet, testFile);
        return outputFiles;
    }
    public static void generateCSVForVersions(List<Release> releases, String projName) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(projName + "VersionInfo.csv");
            fileWriter.append("Index, Version Name, Date");
            fileWriter.append("\n");
            for (Release release : releases) {
                fileWriter.append(
                        release.getId() + ", " +
                                release.getName() + ", " +
                                release.getDate() + ", "
                );
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}