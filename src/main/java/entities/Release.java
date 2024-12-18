package entities;
import java.time.LocalDateTime;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.List;
public class Release {
    private int id;
    private String name;
    private LocalDateTime date;
    private List<RevCommit> associatedCommits;
    private RevCommit lastCommit;
    public Release(int id, String name, LocalDateTime date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }
    public void setAssociatedCommits(List<RevCommit> associatedCommits) {
        this.associatedCommits = associatedCommits;
    }
    public RevCommit getLastCommit() {
        return lastCommit;
    }
    public void setLastCommit(RevCommit lastCommit) {
        this.lastCommit = lastCommit;
    }
}