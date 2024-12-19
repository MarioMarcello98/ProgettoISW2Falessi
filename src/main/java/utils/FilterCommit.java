package utils;
import entities.Ticket;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.ArrayList;
import java.util.List;
public class FilterCommit{
    public static List<RevCommit> filterCommitsAssociatedToTicket(Ticket ticket, List<RevCommit> allCommits) {
        List<RevCommit> assCommits = new ArrayList<>();
        for (RevCommit commit : allCommits) {
            if (!assCommits.contains(commit))
                if (commit.getFullMessage().contains(ticket.key + ":") || commit.getFullMessage().contains("[" + ticket.key))
                    assCommits.add(commit);
        }
        return assCommits;
    }
}
