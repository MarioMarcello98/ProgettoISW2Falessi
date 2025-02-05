package entities;
import org.eclipse.jgit.revwalk.RevCommit;

public class TicketCommit {
    private RevCommit commit;
    private Ticket ticket;

    public RevCommit getCommit() {
        return commit;
    }

    public TicketCommit(RevCommit commit, Ticket ticket) {
        this.commit = commit;
        this.ticket = ticket;
    }

    public void setCommit(RevCommit commit) {
        this.commit = commit;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}