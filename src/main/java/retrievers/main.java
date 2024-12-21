package retrievers;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import java.io.IOException;
import java.util.List;
public class Main {
    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = GetTicketInfo.retrieveTickets("BOOKKEEPER");
            CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets);
            //List<Ticket> allTickets = GetTicketInfo.retrieveTickets("OPENJPA");
            //CommitRetriever.retrieveCommits("OPENJPA", allTickets);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}