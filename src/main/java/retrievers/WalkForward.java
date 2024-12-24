package retrievers;
import entities.Class;
import entities.Release;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.IOException;

import java.util.List;
import utils.CSV;
import weka.WekaFunction;
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
public class WalkForward {
    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException {
        List<List<File>> files = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, true);
        WekaFunction wekaFunction = new WekaFunction();
        for (int i = 1; i <= releases.size(); i++) {
            System.out.println("\n\nRetrieving tickets for the first " + i + " releases");
            List<Ticket> tickets = GetTicketInfo.retrieveTickets(projName, i);
            List<Class> allClasses = CommitRetriever.retrieveCommits(projName, tickets, i);
            if (i > 1) {
                List<Class> classesForTrainingSet = new ArrayList<>();
                List<Class> classesForTestingSet = new ArrayList<>();
                for (Class c : allClasses) {
                    if (c.getRelease().getId() < i)
                        classesForTrainingSet.add(c);
                    else
                        classesForTestingSet.add(c);
                }
                List<File> outputFiles = CSV.generateCSVForWF(classesForTrainingSet, classesForTestingSet, projName, i);
                File trainFile = outputFiles.get(0);
                File testFile = outputFiles.get(1);
                WekaFunction.CSVToARFF(trainFile);
                WekaFunction.CSVToARFF(testFile);
                files.add(outputFiles);
            } else {
                List<File> outputFiles =  CSV.generateCSVForWF(Collections.emptyList(), allClasses, projName, i);
                File testFile = outputFiles.get(0);
                WekaFunction.CSVToARFF(testFile);
            }
        }
        return files;
    }
    public static void classify(String projName) throws JSONException, IOException {
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();
        for (int i = 2; i <= releases.size(); i++) {
            String trainSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.arff";
            String testSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_testing-set.arff";
            try {
                weka.classify(trainSetPath, testSetPath);
            } catch (EmptyARFFException e) {
                System.out.println("empty arff");
                // ignore, empty ARFF file
            }
        }
    }
}