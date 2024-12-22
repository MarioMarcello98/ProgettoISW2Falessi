package retrievers;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.eclipse.jgit.api.errors.GitAPIException;
public class Main { //da errore perchè minuscolo?
    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = GetTicketInfo.retrieveTickets("BOOKKEEPER");
            CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets);
             allTickets = GetTicketInfo.retrieveTickets("OPENJPA");
            CommitRetriever.retrieveCommits("OPENJPA", allTickets);
        } catch (JSONException | IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}