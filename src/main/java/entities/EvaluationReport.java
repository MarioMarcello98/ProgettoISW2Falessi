package entities;
import weka.Weka;

public class EvaluationReport {
    private int iteration;
    private Classifier.Type classifier;
    private String dataset;
    private boolean featureSelection;
    private Weka.SearchMethods fsSearchMethod;
    private Weka.SamplingMethod samplingMethod;
    private boolean costSensitiveClassification;
    private Metrics metrics;

    public EvaluationReport(int iteration, Classifier.Type classifier, String dataset, boolean featureSelection, Weka.SearchMethods fsSearchMethod, Weka.SamplingMethod samplingMethod, boolean costSensitiveClassification) {
        this.iteration = iteration;
        this.classifier = classifier;
        this.dataset = dataset;
        this.featureSelection = featureSelection;
        this.fsSearchMethod = fsSearchMethod;
        this.samplingMethod = samplingMethod;
        this.costSensitiveClassification = costSensitiveClassification;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public boolean isCostSensitiveClassification() {
        return costSensitiveClassification;
    }

    public void setCostSensitiveClassification(boolean costSensitiveClassification) {
        this.costSensitiveClassification = costSensitiveClassification;
    }

    public Weka.SamplingMethod getSamplingMethod() {
        return samplingMethod;
    }

    public Weka.SearchMethods getFsSearchMethod() {
        return fsSearchMethod;
    }

    public boolean isFeatureSelection() {
        return featureSelection;
    }

    public int getIteration() {
        return iteration;
    }

    public Classifier.Type getClassifier() {
        return classifier;
    }

    public String getDataset() {
        return dataset;
    }

}