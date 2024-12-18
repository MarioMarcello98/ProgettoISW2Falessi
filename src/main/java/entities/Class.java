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
    private int LOCTouched;
    private int NAuth;
    private int NFix;
    private int NR;
    private int LOCAdded;
    private int maxLOCAdded;
    private int averageLOCAdded;
    private int churn;
    private int changeSetSize;
    public Class(String name, String implementation, Release release) {
        this.name = name;
        this.implementation = implementation;
        this.release = release;
        this.associatedCommits = new ArrayList<>();
        this.isBuggy = false;
        this.size = 0;
        this.LOCTouched = 0;
        this.NAuth = 0;
        this.NFix = 0;
        this.NR = 0;
        this.LOCAdded = 0;
        this.maxLOCAdded = 0;
        this.averageLOCAdded = 0;
        this.churn = 0;
        this.changeSetSize = 0;
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
    public int getLOCTouched() {
        return LOCTouched;
    }
    public void setLOCTouched(int LOCTouched) {
        this.LOCTouched = LOCTouched;
    }
    public int getNAuth() {
        return NAuth;
    }
    public void setNAuth(int NAuth) {
        this.NAuth = NAuth;
    }
    public int getNFix() {
        return NFix;
    }
    public void setNFix(int NFix) {
        this.NFix = NFix;
    }
    public int getNR() {
        return NR;
    }
    public void setNR(int NR) {
        this.NR = NR;
    }
    public int getLOCAdded() {
        return LOCAdded;
    }
    public void setLOCAdded(int LOCAdded) {
        this.LOCAdded = LOCAdded;
    }
    public int getMaxLOCAdded() {
        return maxLOCAdded;
    }
    public void setMaxLOCAdded(int maxLOCAdded) {
        this.maxLOCAdded = maxLOCAdded;
    }
    public int getAverageLOCAdded() {
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
    public int getChangeSetSize() {
        return changeSetSize;
    }
    public void setChangeSetSize(int changeSetSize) {
        this.changeSetSize = changeSetSize;
    }
    public String getImplementation() {
        return implementation;
    }
    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

}