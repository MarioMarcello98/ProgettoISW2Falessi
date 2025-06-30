package retrievers;

import exception.EmptyARFFException;
import exception.ExecutionException;
import entities.Class;
import entities.Release;
import entities.Ticket;
import utils.CSV;
import weka.Weka;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class WalkForward {

    private WalkForward() {}

    private static final Logger logger = LoggerFactory.getLogger(WalkForward.class);
    private static final int CSV_FILE = 0;
    private static final int ARFF_FILE = 1;
    private static final int TRAIN_SET = 0;
    private static final int TEST_SET = 1;

    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException, ExecutionException, ParseException {
        List<List<File>> files = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);
        List<Class> allClasses;
        List<Ticket> allTickets = GetTicketInfo.retrieveTickets(projName, releases.size());
        if (!new File(projName + ".csv").exists()) {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size());
            CSV.generateCSV(allClasses, projName, releases.size());
        } else {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size());
        }
        for (int i = 2; i <= Math.round((float) releases.size() / 2); i++) {
            logger.info("Retrieving tickets for the first {} releases", i);
            String filenameCSV = getPath(projName, i, TRAIN_SET, CSV_FILE);
            if (!new File(filenameCSV).exists()) {
                List<Ticket> ticketsForTrainSet = GetTicketInfo.retrieveTickets(projName, i - 1);
                List<Class> classesForTrainSet = CommitRetriever.retrieveCommits(projName, ticketsForTrainSet, i - 1);
                CSV.generateCSVForWF(CSV.Type.TRAINING_SET, classesForTrainSet, projName, i);
            }
            List<Class> classesForTestSet = new ArrayList<>();
            for (Class c : allClasses) {
                if (c.getRelease().getId() == i) {
                    classesForTestSet.add(c);
                }
            }
            CSV.generateCSVForWF(CSV.Type.TESTING_SET, classesForTestSet, projName, i);
        }
        return files;
    }

    public static void classify(String projName) throws JSONException, ExecutionException, IOException {
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();
        for (int i = 2; i <= releases.size(); i++) {
            String trainSetPath = getPath(projName, i, TRAIN_SET, ARFF_FILE);
            String testSetPath = getPath(projName, i, TEST_SET, ARFF_FILE);
            try {
                weka.classify(trainSetPath, testSetPath, i, projName);
            } catch (EmptyARFFException e) {
                logger.warn("empty arff");
            }
        }
        weka.generateFiles();
    }

    private static String getPath(String projName, int iteration, int setType, int fileType) {
        String basePath = "walkforward/" + projName + "_" + iteration + "/";
        switch (setType) {
            case TRAIN_SET:
                switch (fileType) {
                    case CSV_FILE:
                        return basePath + projName + "_" + iteration + "_training-set.csv";
                    case ARFF_FILE:
                        return basePath + projName + "_" + iteration + "_training-set.arff";
                }
                break;
            case TEST_SET:
                switch (fileType) {
                    case CSV_FILE:
                        return basePath + projName + "_" + iteration + "_testing-set.csv";
                    case ARFF_FILE:
                        return basePath + projName + "_" + iteration + "_testing-set.arff";
                }
                break;
        }
        return null;
    }
}
