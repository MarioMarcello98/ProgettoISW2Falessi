package entities;

public class Metrics {
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    public Metrics(double precision, double recall, double auc, double kappa) {
        this.precision = precision;
        this.recall = recall;
        this.auc = auc;
        this.kappa = kappa;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getAuc() {
        return auc;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }
}