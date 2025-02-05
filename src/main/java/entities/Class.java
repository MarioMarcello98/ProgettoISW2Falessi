package entities;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.ArrayList;
import java.util.List;

public class Class {
    private String name;
    private String implementation;
    private Release release;
    private List<RevCommit> associatedCommits;
    private boolean isBuggy;
    private int size;
    private int locTouched;
    private int locAdded;
    private int maxLOCAdded;
    private double averageLOCAdded;
    private int churn;
    private int maxChurn;
    private double averageChurn;
    private int nR;
    private int nAuth;

    public Class(String name, String implementation, Release release) {
        this.name = name;
        this.implementation = implementation;
        this.release = release;
        this.associatedCommits = new ArrayList<>();
        this.isBuggy = false;
        this.size = 0;
        this.locAdded=0;
        this.locTouched =0;
        this.maxLOCAdded = 0;
        this.averageLOCAdded = 0;
        this.nR = 0;
        this.churn = 0;
        this.maxChurn = 0;
        this.averageChurn = 0;
        this.nAuth = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }

    public void setAssociatedCommits(List<RevCommit> associatedCommits) {
        this.associatedCommits = associatedCommits;
    }

    public boolean isBuggy() {
        return isBuggy;
    }

    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNR() {
        return nR;
    }

    public void setNR(int nR) {
        this.nR = nR;
    }

    public int getLOCAdded() {
        return locAdded;
    }

    public void setLOCAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public int getMaxLOCAdded() {
        return maxLOCAdded;
    }

    public void setMaxLOCAdded(int maxLOCAdded) {
        this.maxLOCAdded = maxLOCAdded;
    }

    public double getAverageLOCAdded() {
        return averageLOCAdded;
    }

    public void setAverageLOCAdded(int averageLOCAdded) {
        this.averageLOCAdded = averageLOCAdded;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public double getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(float averageChurn) {
        this.averageChurn = averageChurn;
    }

    public int getLOCTouched() {
        return locTouched;
    }

    public void setLOCTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public int getnAuth() {
        return nAuth;
    }

    public void setnAuth(int nAuth) {
        this.nAuth = nAuth;
    }
}