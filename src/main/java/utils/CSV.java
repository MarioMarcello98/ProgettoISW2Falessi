package utils;

import exception.ExecutionException;
import entities.Class;
import entities.EvaluationReport;
import entities.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSV {

    public enum Type {
        TRAINING_SET,
        TESTING_SET
    }

    private static final Logger logger = LoggerFactory.getLogger(CSV.class);
    private static final String FIRST_ROW = "Version, File Name, Size, LOCAdded, LOCTouched, MaxLOCAdded, averageLOCAdded, NR, Churn, MaxChurn, AverageChurn, nAuthors, Buggy";

    public static void generateCSV(List<Class> classes, String projName, int numVersions) throws IOException {
        logger.info("Generating the CSV file for {}, numVersions = {}", projName, numVersions);
        File file = new File(projName + ".csv");
        populateFile(classes, file);
        logger.info("CSV generated for {}, numVersions = {}", projName, numVersions);
    }

    private static void populateFile(List<Class> classes, File file) throws IOException {
        try (
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)
        ) {
            bufferedWriter.append(FIRST_ROW);
            bufferedWriter.append("\n");
            for (Class c : classes) {
                String row = c.getRelease().getId() + ", " +
                        c.getName() + ", " +
                        c.getSize() + ", " +
                        c.getLOCAdded() + "," +
                        c.getLOCTouched() + "," +
                        c.getMaxLOCAdded() + "," +
                        c.getAverageLOCAdded() + "," +
                        c.getNR() + "," +
                        c.getChurn() + "," +
                        c.getMaxChurn() + "," +
                        c.getAverageChurn() + "," +
                        c.getnAuth() + "," +
                        c.isBuggy();
                bufferedWriter.append(row);
                bufferedWriter.append("\n");
            }
        }
    }

    public static File generateCSVForWF(Type type, List<Class> classes, String projName, int iteration) throws IOException, ExecutionException {
        if (type == Type.TRAINING_SET) {
            String filenameCSV = getPath(projName, iteration, 0, 0);
            String filenameARFF = getPath(projName, iteration, 0, 1);
            File trainFile = new File(filenameCSV);
            trainFile.getParentFile().mkdirs();
            boolean res = trainFile.createNewFile();
            assert res;
            populateFile(classes, trainFile);
            ARFF.generateARFF(filenameARFF, classes);
            return trainFile;
        } else if (type == Type.TESTING_SET) {
            String filenameCSV = getPath(projName, iteration, 1, 0);
            String filenameARFF = getPath(projName, iteration, 1, 1);
            File testFile = new File(filenameCSV);
            testFile.getParentFile().mkdirs();
            boolean res = testFile.createNewFile();
            assert res;
            populateFile(classes, testFile);
            ARFF.generateARFF(filenameARFF, classes);
            return testFile;
        }
        return null;
    }

    private static String getPath(String projName, int iteration, int setType, int fileType) {
        String basePath = "walkforward/" + projName + "_" + iteration + "/";
        switch (setType) {
            case 0:
                switch (fileType) {
                    case 0:
                        return basePath + projName + "_" + iteration + "_training-set.csv";
                    case 1:
                        return basePath + projName + "_" + iteration + "_training-set.arff";
                    default:
                        return null;
                }
            case 1:
                switch (fileType) {
                    case 0:
                        return basePath + projName + "_" + iteration + "_testing-set.csv";
                    case 1:
                        return basePath + projName + "_" + iteration + "_testing-set.arff";
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    public static void generateCSVForReportsWithoutFS(List<EvaluationReport> reports) throws ExecutionException {
        String filename = "report/" + reports.get(0).getDataset() + "-report-withoutFS.csv";
        File file = new File(filename);
        file.getParentFile().mkdirs();
        try (FileWriter fileWriterNoFS = new FileWriter(file)) {
            fileWriterNoFS.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa\n");
            for (EvaluationReport report : reports) {
                if (!report.isFeatureSelection()) {
                    String row = report.getDataset().substring(0, report.getDataset().length() - 3).toLowerCase() + ", "
                            + report.getIteration() + ", "
                            + report.getClassifier().toString().toLowerCase() + ", "
                            + report.getMetrics().getPrecision() + ", "
                            + report.getMetrics().getRecall() + ", "
                            + report.getMetrics().getAuc() + ", "
                            + report.getMetrics().getKappa();
                    fileWriterNoFS.append(row).append("\n");
                }
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
        logger.info("Generated CSV without FS");
    }

    public static void generateCSVForReportsWithFS(List<EvaluationReport> reports) throws ExecutionException {
        EvaluationReportUtils evaluationReportUtils = new EvaluationReportUtils();
        List<List<EvaluationReport>> dividedReports = evaluationReportUtils.divideReportsBySearchMethod(reports);
        for (List<EvaluationReport> list : dividedReports) {
            String filename = "report/" + list.get(0).getDataset() + "-report-withFS-" + list.get(0).getFsSearchMethod().toString().toLowerCase() + ".csv";
            File file = new File(filename);
            file.getParentFile().mkdirs();
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa, Search Method\n");
                for (EvaluationReport report : list) {
                    fileWriter.append(report.getDataset() + ", "
                            + report.getIteration() + ", "
                            + report.getClassifier().toString().toLowerCase() + ", "
                            + report.getMetrics().getPrecision() + ", "
                            + report.getMetrics().getRecall() + ", "
                            + report.getMetrics().getAuc() + ", "
                            + report.getMetrics().getKappa() + ", "
                            + report.getFsSearchMethod() + "\n");
                }
            } catch (IOException e) {
                throw new ExecutionException(e);
            } catch (Exception e) {
                logger.error("Found error in generating the report for {}", list.get(0).getFsSearchMethod().toString().toLowerCase());
                throw new ExecutionException(e);
            }
        }
        logger.info("Generated CSV for FS");
    }

    public static void generateCSVForReportsWithSampling(List<EvaluationReport> reports) throws ExecutionException {
        EvaluationReportUtils evaluationReportUtils = new EvaluationReportUtils();
        List<List<EvaluationReport>> reportsWithSampling = evaluationReportUtils.divideReportsBySamplingMethod(reports);
        for (List<EvaluationReport> list : reportsWithSampling) {
            String filename = "report/" + list.get(0).getDataset() + "-report-withFS-best_first-with-" + list.get(0).getSamplingMethod().toString().toLowerCase() + ".csv";
            File file = new File(filename);
            file.getParentFile().mkdirs();
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa, Search Method, Sampling Method\n");
                for (EvaluationReport report : list) {
                    fileWriter.append(report.getDataset().substring(0, report.getDataset().length() - 3).toLowerCase())
                            .append(", ").append(String.valueOf(report.getIteration()))
                            .append(", ").append(report.getClassifier().toString().toLowerCase())
                            .append(", ").append(String.valueOf(report.getMetrics().getPrecision()))
                            .append(", ").append(String.valueOf(report.getMetrics().getRecall()))
                            .append(", ").append(String.valueOf(report.getMetrics().getAuc()))
                            .append(", ").append(String.valueOf(report.getMetrics().getKappa()))
                            .append(", ").append(String.valueOf(report.getFsSearchMethod()))
                            .append(", ").append(String.valueOf(report.getSamplingMethod()))
                            .append("\n");
                }
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        logger.info("Generated CSV for sampling");
    }

    public static void generateCSVForReportsWithCSC(List<EvaluationReport> reports) throws ExecutionException {
        String filename = "report/" + reports.get(0).getDataset() + "-report-with-CSC.csv";
        File file = new File(filename);
        file.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa, Search Method\n");
            for (EvaluationReport report : reports) {
                fileWriter.append(report.getDataset().substring(0, report.getDataset().length() - 3).toLowerCase())
                        .append(", ").append(String.valueOf(report.getIteration()))
                        .append(", ").append(report.getClassifier().toString().toLowerCase())
                        .append(", ").append(String.valueOf(report.getMetrics().getPrecision()))
                        .append(", ").append(String.valueOf(report.getMetrics().getRecall()))
                        .append(", ").append(String.valueOf(report.getMetrics().getAuc()))
                        .append(", ").append(String.valueOf(report.getMetrics().getKappa()))
                        .append(", ").append(String.valueOf(report.getFsSearchMethod()))
                        .append("\n");
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
        logger.info("Generated report for CSC");
    }

    public static void generateCSVForVersions(List<Release> releases, String projName) throws ExecutionException {
        try (FileWriter fileWriter = new FileWriter(projName + "VersionInfo.csv")) {
            fileWriter.append("Index, Version Name, Date\n");
            for (Release release : releases) {
                fileWriter.append(
                        release.getId() + ", " +
                                release.getName() + ", " +
                                release.getDate() + "\n"
                );
            }
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }
}
