package utils;
import exception.ExecutionException;
import entities.Classifier;
import entities.EvaluationReport;
import weka.Weka;
import java.util.ArrayList;
import java.util.List;

public class EvaluationReportUtils {
    public List<List<EvaluationReport>> divideReportsBySearchMethod(List<EvaluationReport> reports) throws ExecutionException {
        List<List<EvaluationReport>> reportsLists = new ArrayList<>();
        List<EvaluationReport> reportsBS = new ArrayList<>();
        List<EvaluationReport> reportsFS = new ArrayList<>();
        List<EvaluationReport> reportsBF = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.getFsSearchMethod() == Weka.SearchMethods.BACKWARD_SEARCH)
                reportsBS.add(report);
            else if (report.getFsSearchMethod() == Weka.SearchMethods.FORWARD_SEARCH)
                reportsFS.add(report);
            else if (report.getFsSearchMethod() == Weka.SearchMethods.BIDIRECTIONAL_SEARCH)
                reportsBF.add(report);
            else
                throw new ExecutionException("divideReportsBySearchMethod: Unexpected search method");
        }
        reportsLists.add(reportsBS);
        reportsLists.add(reportsFS);
        reportsLists.add(reportsBF);
        return reportsLists;
    }

    public List<EvaluationReport> getReportsWithFS(List<EvaluationReport> reports) {
        List<EvaluationReport> reportsWithFS = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.isFeatureSelection())
                reportsWithFS.add(report);
        }
        return reportsWithFS;
    }

    public List<EvaluationReport> getReportsWithoutFS(List<EvaluationReport> reports) {
        List<EvaluationReport> reportsWithFS = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (!report.isFeatureSelection())
                reportsWithFS.add(report);
        }
        return reportsWithFS;
    }

    public List<List<EvaluationReport>> divideReportsByClassifier(List<EvaluationReport> reports) throws ExecutionException {
        List<List<EvaluationReport>> list = new ArrayList<>();
        List<EvaluationReport> reportsNB = new ArrayList<>();
        List<EvaluationReport> reportsRF = new ArrayList<>();
        List<EvaluationReport> reportsIBk = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.getClassifier() == Classifier.Type.NAIVE_BAYES)
                reportsNB.add(report);
            else if (report.getClassifier() == Classifier.Type.RANDOM_FOREST)
                reportsRF.add(report);
            else if (report.getClassifier() == Classifier.Type.IBK)
                reportsIBk.add(report);
            else
                throw new ExecutionException("divideReportsByClassifier: Unexpected classifier");
        }
        list.add(reportsNB);
        list.add(reportsRF);
        list.add(reportsIBk);
        return list;
    }

    public List<List<EvaluationReport>> divideReportsBySamplingMethod(List<EvaluationReport> reports) throws ExecutionException {
        List<List<EvaluationReport>> list = new ArrayList<>();
        List<EvaluationReport> undersamplingReports = new ArrayList<>();
        List<EvaluationReport> oversamplingReports = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.getSamplingMethod() == Weka.SamplingMethod.UNDERSAMPLING)
                undersamplingReports.add(report);
            else if (report.getSamplingMethod() == Weka.SamplingMethod.OVERSAMPLING)
                oversamplingReports.add(report);
            else if (report.getSamplingMethod() != null)
                throw new ExecutionException("divideReportsBySamplingMethod: Unexpected sampling method");
        }
        list.add(undersamplingReports);
        list.add(oversamplingReports);
        return list;
    }
}