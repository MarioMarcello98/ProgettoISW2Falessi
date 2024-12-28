package entities;

import org.eclipse.jgit.revwalk.RevCommit;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Class {
    private String name;
    private String implementation;
    private Release release;


    private List<RevCommit> associatedCommits;
    private boolean isBuggy;

    private int size;
    private int LOCTouched;
    private int LOCAdded;
    private int maxLOCAdded;
    private float averageLOCAdded;
    private int churn;
    private int maxChurn;
    private float averageChurn;
    private int NFix;
    private final Set<String> authors = new HashSet<>();








    public Class(String name, String implementation, Release release) {
        this.name = name;
        this.implementation = implementation;
        this.release = release;
        this.associatedCommits = new ArrayList<>();
        this.isBuggy = false;
        this.size = 0;
        this.LOCAdded=0;
        this.LOCTouched=0;
        this.maxLOCAdded = 0;
        this.averageLOCAdded = 0;
        this.NFix = 0;
        this.churn = 0;
        this.maxChurn = 0;
        this.averageChurn = 0;
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

    public int getNFix() {
        return NFix;
    }
    public void setNFix(int NFix) {
        this.NFix = NFix;
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
    public float getAverageLOCAdded() {
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
    public float getAverageChurn() {
        return averageChurn;
    }
    public void setAverageChurn(float averageChurn) {
        this.averageChurn = averageChurn;
    }
    public int getLOCTouched() {
        return LOCTouched;
    }

    public void setLOCTouched() {
        this.LOCTouched = LOCTouched;
    }
    public void addAuthor(String author){
        authors.add(author);
    }
    public int getNAuth(){
        return authors.size();
    }
}