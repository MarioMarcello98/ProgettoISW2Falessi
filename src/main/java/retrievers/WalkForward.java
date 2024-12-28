import retrievers.GetJiraInfo;
import retrievers.GetTicketInfo;
import entities.Ticket;
import entities.Release;
import weka.Weka;
import weka.WekaFunction;
import retrievers.CommitRetriever;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WalkForward {

    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException {
        List<List<File>> files = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);
        WekaFunction wekaUtils = new WekaFunction();
        List<Class> allClasses = new ArrayList<>();

        // here we retrieve all tickets and classes, in order to create the testing sets for all the iterations
        List<Ticket> allTickets = GetTicketInfo.retrieveTickets(projName, releases.size());
        if (!new File(projName + ".csv").exists()) {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size(), true);
            CSV.generateCSV(allClasses, projName, releases.size());
        } else {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size(), false);
        }

        // we have to create different CSV files: for each iteration, starting just with the first release, we add the next release
        for (int i = 2; i <= Math.round((float) releases.size() / 2); i++) {

            /* i must be > 1: we skip the first iteration, as it doesn't have the training set -> inaccurate predictions */

            // training set
            System.out.println("\n\nRetrieving tickets for the first " + i + " releases");

            String filenameCSV = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.csv";

            if (!new File(filenameCSV).exists()) {
                List<Ticket> ticketsForTrainSet = GetTicketInfo.retrieveTickets(projName, i - 1);
                List<Class> classesForTrainSet = CommitRetriever.retrieveCommits(projName, ticketsForTrainSet, i - 1, true);

                File trainFile = CSV.generateCSVForWF(CSV.Type.TRAINING_SET, classesForTrainSet, projName, i);
            }

            // testing set
            List<Class> classesForTestSet = new ArrayList<>();
            for (Class c : allClasses) {
                if (c.getRelease().getId() == i)
                    classesForTestSet.add(c);
            }

            File testFile = CSV.generateCSVForWF(CSV.Type.TESTING_SET, classesForTestSet, projName, i);
        return files;
    }

    public static void classify(String projName) throws JSONException, IOException {
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();

        for (int i = 2; i <= releases.size(); i++) {

            String trainSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.arff";
            String testSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_testing-set.arff";

            try {
                weka.classify(trainSetPath, testSetPath, i, projName);
            } catch (EmptyARFFException e) {
                System.out.println("empty arff");
                // ignore, empty ARFF file
            }
        }

        weka.generateFiles();
    }

    private static void getDominantClassifier(List<EvaluationReport> reports) {
        EvaluationReportUtils evaluationReportUtils = new EvaluationReportUtils();
        List<EvaluationReport> meanReports = new ArrayList<>();

        List<EvaluationReport> reportsWithoutFS = evaluationReportUtils.getReportsWithoutFS(reports);

        try {

            // evaluating reports with no FS
            List<List<EvaluationReport>> reportsDividedByClassifierNoFS = evaluationReportUtils.divideReportsByClassifier(reportsWithoutFS);

            for (List<EvaluationReport> list : reportsDividedByClassifierNoFS) {
                meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(list));
            }

            // evaluating reports with FS
            List<EvaluationReport> reportsWithFS = evaluationReportUtils.getReportsWithFS(reports);

            List<List<EvaluationReport>> divReportsWithFS = evaluationReportUtils.divideReportsBySearchMethod(reportsWithFS);
            for (List<EvaluationReport> list : divReportsWithFS) {
                List<List<EvaluationReport>> reportsDividedByClassifierFS = evaluationReportUtils.divideReportsByClassifier(list);
                for (List<EvaluationReport> listDivByClassifier : reportsDividedByClassifierFS) {
                    meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(listDivByClassifier));
                }
            }

            // evaluating reports with FS and sampling
            List<EvaluationReport> reportsWithSampling = evaluationReportUtils.getReportsWithSampling(reports);
            List<List<EvaluationReport>> reportsDividedByClassifierSampling = evaluationReportUtils.divideReportsByClassifier(reportsWithSampling);
            for (List<EvaluationReport> list : reportsDividedByClassifierSampling) {
                meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(list));
            }

            for (EvaluationReport report : meanReports) {
                if (report.isFeatureSelection()) {
                    if (report.getSamplingMethod() == null)
                        System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " with FS search method " + report.getFSSearchMethod().toString().toLowerCase());
                    else
                        System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " with FS search method " + report.getFSSearchMethod().toString().toLowerCase() + "and with " + report.getSamplingMethod().toString().toLowerCase());

                }
                else
                    System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " without feature selection");

                System.out.println("Mean precision = " + report.getPrecision());
                System.out.println("Mean recall = " + report.getRecall());
                System.out.println("Mean AUC = " + report.getAUC());
                System.out.println("Mean kappa = " + report.getKappa());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}