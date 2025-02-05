package retrievers;
import entities.Release;
import entities.Ticket;
import exception.ExecutionException;
import exception.InvalidTicketException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static utils.JSON.readJsonFromUrl;

public class GetTicketInfo {
    private static final Logger logger = LoggerFactory.getLogger(GetTicketInfo.class);
    private GetTicketInfo() {}

    public static List<Ticket> retrieveTickets(String projName, int numReleases) throws JSONException, IOException, ExecutionException {
        int startAt = 0;
        ArrayList<Ticket> tickets = new ArrayList<>();
        JSONObject json;
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, false, numReleases, false);
        LocalDateTime lastDate = releases.get(releases.size() - 1).getDate();
        logger.info("Retrieving tickets for project {}", projName);
        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;
            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(getTicket(issues.getJSONObject(i), releases, lastDate));
                } catch (JSONException e) {
                    throw new ExecutionException(e);
                } catch (InvalidTicketException e) {
                    // ignore: invalid ticket
                }
            }
            startAt += 1000;
        } while (json.getJSONArray("issues").length() != 0);
        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getProportion() != 0) {
                count++;
            }
        }
        logger.info("Tickets having proportion (project {}): {} over {} tickets", projName,  count, tickets.size());
        Proportion.coldStartProportion(tickets, projName);
        count = 0;
        logger.info("Tickets having injected version: {}", count);
        return tickets;
    }

    public static Ticket getTicket(JSONObject ticketInfo, List<Release> rels, LocalDateTime lastDate) throws JSONException, InvalidTicketException {
        Ticket ticket = new Ticket();
        JSONObject fields = ticketInfo.getJSONObject("fields");
        if (!fields.has("resolutiondate")) {
            throw new InvalidTicketException();
        }
        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        if (resolutionDate.isAfter(rels.get(rels.size() - 1).getDate())) {
            ticket.setFixVersion(null);
        } else {
            ticket.setFixVersion(getRelease(resolutionDate));
        }
        ticket.setId(ticketInfo.get("id").toString());
        ticket.setKey(ticketInfo.get("key").toString());

        // walk-forward
        if (creationDate.isAfter(lastDate)) {
            throw new InvalidTicketException();
        }
        ticket.setOpeningVersion(getRelease(creationDate));
        JSONArray components = fields.getJSONArray("components");
        for (int i = 0; i < components.length(); i++) {
            ticket.getAffectedComponents().add(components.getJSONObject(i).get("name").toString());
        }
        JSONArray versions = fields.getJSONArray("versions");
        if (versions.isNull(0)) {
            ticket.setAffectedVersions(null);
        } else {
            for (int i = 0; i < versions.length(); i++) {
                if (versions.getJSONObject(i).has("releaseDate"))
                    ticket.getAffectedVersions().add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay()));
            }
        }
        setIV(ticket);
        return ticket;
    }

    private static void setIV(Ticket ticket) {
        if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
            Release injVersion = ticket.getAffectedVersions().get(0);
            for (Release affVersion : ticket.getAffectedVersions()) {
                if (affVersion.getId() < injVersion.getId())
                    injVersion = affVersion;
            }
            if (injVersion.getId() <= ticket.getOpeningVersion().getId()) {
                ticket.setInjectedVersion(injVersion);
                Proportion.computeProportion(ticket);
            }
        }
    }

    public static float getProportionMean(List<Ticket> tickets) {
        float sum = 0;
        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getProportion() > 0) {
                sum += ticket.getProportion();
                count++;
            }
        }
        assert count != 0;
        return sum / count;
    }


    public static Release getRelease(LocalDateTime date) {
        int i = 0;
        List<Release> rels = GetJiraInfo.getReleases();
        while (rels.get(i).getDate().isBefore(date)) {
            i++;
        }
        return rels.get(i);
    }

    public static List<Ticket> getTicketsWithAV(List<Ticket> tickets) {
        List<Ticket> ticketsWithAV = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getFixVersion() == null) {
                ticketsWithAV.add(ticket);
                continue;
            }
            if (ticket.getFixVersion().getId() > ticket.getInjectedVersion().getId()) {
                // these tickets have fix version different from the injected version, so they have AV
                ticketsWithAV.add(ticket);
            }
        }
        return ticketsWithAV;
    }
}