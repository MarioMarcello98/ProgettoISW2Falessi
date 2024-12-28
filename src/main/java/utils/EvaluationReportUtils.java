package utils;

import entities.Classifier;
import entities.EvaluationReport;
import weka.Weka;

import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.List;

public class EvaluationReportUtils {

    public List<List<EvaluationReport>> divideReportsBySearchMethod(List<EvaluationReport> reports) throws Exception {
        List<List<EvaluationReport>> reportsLists = new ArrayList<>();

        List<EvaluationReport> reportsBS = new ArrayList<>();
        List<EvaluationReport> reportsFS = new ArrayList<>();
        List<EvaluationReport> reportsBF = new ArrayList<>();

        for (EvaluationReport report : reports) {
            if (report.getFSSearchMethod() == Weka.SearchMethods.BACKWARD_SEARCH)
                reportsBS.add(report);
            else if (report.getFSSearchMethod() == Weka.SearchMethods.FORWARD_SEARCH)
                reportsFS.add(report);
            else if (report.getFSSearchMethod() == Weka.SearchMethods.BEST_FIRST)
                reportsBF.add(report);
            else
                throw new Exception("divideReportsBySearchMethod: Unexpected search method");
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

    public List<List<EvaluationReport>> divideReportsByClassifier(List<EvaluationReport> reports) throws Exception {
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
                throw new Exception("divideReportsByClassifier: Unexpected classifier");
        }

        list.add(reportsNB);
        list.add(reportsRF);
        list.add(reportsIBk);

        return list;
    }

    public List<List<EvaluationReport>> divideReportsBySamplingMethod(List<EvaluationReport> reports) throws Exception {
        List<List<EvaluationReport>> list = new ArrayList<>();
        List<EvaluationReport> undersamplingReports = new ArrayList<>();
        List<EvaluationReport> oversamplingReports = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.getSamplingMethod() == Weka.SamplingMethod.UNDERSAMPLING)
                undersamplingReports.add(report);
            else if (report.getSamplingMethod() == Weka.SamplingMethod.OVERSAMPLING)
                oversamplingReports.add(report);
            else if (report.getSamplingMethod() != null)
                throw new Exception("divideReportsBySamplingMethod: Unexpected sampling method");
        }
        list.add(undersamplingReports);
        list.add(oversamplingReports);
        return list;
    }
    public List<EvaluationReport> getReportsWithSampling(List<EvaluationReport> reports) {
        List<EvaluationReport> list = new ArrayList<>();
        for (EvaluationReport report : reports) {
            if (report.getSamplingMethod() != null)
                list.add(report);
        }
        return list;
    }
    /*
     *  this method, given a list of reports regarding the same classifier on different iterations, computes the mean precision, recall, AUC and kappa
     */
    public EvaluationReport getMeanValuesForClassifier(List<EvaluationReport> reports) {
        double meanPrecision = 0;
        double meanRecall = 0;
        double meanAUC = 0;
        double meanKappaValue = 0;

        for (EvaluationReport report : reports) {
            meanPrecision += report.getPrecision();
            meanRecall += report.getRecall();
            meanAUC += report.getAUC();
            meanKappaValue += report.getKappa();
        }

        meanPrecision = meanPrecision / reports.size();
        meanRecall = meanRecall / reports.size();
        meanAUC = meanAUC / reports.size();
        meanKappaValue = meanKappaValue / reports.size();

        return new EvaluationReport(0, reports.get(0).getClassifier(), reports.get(0).getDataset(), meanPrecision, meanRecall, meanAUC, meanKappaValue, reports.get(0).isFeatureSelection(), reports.get(0).getFSSearchMethod());
        return new EvaluationReport(0, reports.get(0).getClassifier(), reports.get(0).getDataset(), meanPrecision, meanRecall, meanAUC, meanKappaValue, reports.get(0).isFeatureSelection(), reports.get(0).getFSSearchMethod(), reports.get(0).getSamplingMethod());
    }
}
