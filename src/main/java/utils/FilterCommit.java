package utils;
import entities.Ticket;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.ArrayList;
import java.util.List;

public class FilterCommit {
    private FilterCommit() {}

    public static List<RevCommit> filterCommitsAssociatedToTicket(Ticket ticket, List<RevCommit> allCommits) {
        List<RevCommit> assCommits = new ArrayList<>();
        for (RevCommit commit : allCommits) {
            String fullMessage = commit.getFullMessage();
            if ((fullMessage.contains(ticket.getKey() + ":") || fullMessage.contains(ticket.getKey() + "]") || fullMessage.contains(ticket.getKey() + " ") || fullMessage.contains("/" + ticket.getKey())) && !assCommits.contains(commit))
                assCommits.add(commit);
        }
        return assCommits;
    }
}