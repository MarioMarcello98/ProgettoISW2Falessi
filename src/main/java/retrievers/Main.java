package retrievers;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import weka.Weka;
import java.io.File;
import org.eclipse.jgit.api.errors.GitAPIException;
public class Main {
    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = GetTicketInfo.retrieveTickets("BOOKKEEPER", 0);
            CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets, 0);
            allTickets = GetTicketInfo.retrieveTickets("OPENJPA", 0);
            CommitRetriever.retrieveCommits("OPENJPA", allTickets, 0);
            List<List<File>> files = WalkForward.initSets("BOOKKEEPER");
            WalkForward.classify("BOOKKEEPER");
            WalkForward.initSets("OPENJPA");
        }          catch (JSONException e) {
        throw new RuntimeException(e);
    } catch (IOException e) {
        throw new RuntimeException(e);
    } catch (GitAPIException e) {
        throw new RuntimeException(e);
        }
    }
}