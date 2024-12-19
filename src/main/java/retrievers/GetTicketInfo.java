package retrievers;
import exception.InvalidTicketException;
import entities.Ticket;
import entities.Release;
import entities.Class;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;
import java.net.URL;
import static utils.JSON.readJsonFromUrl;

public class GetTicketInfo {
    public static ArrayList<Release> releases = new ArrayList<>();

    /*public static void main(String[] args) throws JSONException, IOException {
        String projectName = "BOOKKEEPER";
        String query = "search?jql=project=" + projectName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000";
        String url = "https://issues.apache.org/jira/rest/api/2/" + query;

        JSONObject json = readJsonFromUrl(url);
        JSONArray issues = json.getJSONArray("issues");
        System.out.println(issues.length());

        tickets = new ArrayList<>();

        releases = GetJiraInfo.getJiraInfo(projectName);
        for (int i = 0; i < releases.size(); i++) {
            System.out.println(releases.get(i).getId());
            System.out.println(releases.get(i).getName());
            System.out.println(releases.get(i).getDate());
            System.out.println("____________________________");
        }

        for (int i = 0; i < issues.length(); i++) {
            try {
                tickets.add(getTicketInfo(issues.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();


            }
        }
        System.out.println("-------- tickets having proportion ----------------------");
        for (Ticket ticket : tickets) {
            if (ticket.proportion != 0)
                System.out.println(ticket.fixVersion.getName());
        }

        System.out.println("proportion mean: " + getProportionMean());
        Proportion.estimateInjectedVersion(tickets, getProportionMean());
        for (Ticket ticket : tickets) {
            ArrayList<String> affVersionsNames = new ArrayList<>();
            if (ticket.affectedVersions != null) {
                for (int i = 0; i < ticket.affectedVersions.size(); i++) {
                    affVersionsNames.add(ticket.affectedVersions.get(i).getName());
                }
            }

            System.out.println("id: " + ticket.id);
            System.out.println(ticket.key);
            System.out.println("Affected versions: " + affVersionsNames);
            System.out.println("Opening version: " + ticket.openingVersion.getName());
            if (ticket.injectedVersion != null)
                System.out.println("Injected version: " + ticket.injectedVersion.getName());
            else System.out.println("Injected version: " + null);
            System.out.println("Fix version: " + ticket.fixVersion.getName());
            System.out.println('\n');
        }
        CommitRetriever.retrieveCommits();
    }
*/
    public static ArrayList<Ticket> retrieveTickets(String projName) throws JSONException, IOException {
        int startAt = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();
        JSONObject json;
        releases = GetJiraInfo.getReleaseInfo(projName);
        System.out.println("Retrieving tickets for project " + projName);
        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;
            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(getTicket(issues.getJSONObject(i), releases));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InvalidTicketException e) {
                    // ignore: invalid ticket
                }
            }
            startAt += 1000;
        } while (json.getJSONArray("issues").length() != 0);
        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.proportion != 0) {
                count++;
            }
        }

        System.out.println("Tickets having proportion (project " + projName + "):"  + count + " over " + tickets.size() + " tickets");
        System.out.println("proportion mean (project " + projName + "):" + getProportionMean(tickets));
        Proportion.coldStartProportion(tickets, projName);
        return tickets;
    }
    public static Ticket getTicket(JSONObject ticketInfo, List<Release> rels) throws JSONException, InvalidTicketException, IOException {
        Ticket ticket = new Ticket();

        JSONObject fields = ticketInfo.getJSONObject("fields");
        if (!fields.has("resolutiondate")) {
            throw new InvalidTicketException();
        }


        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        if (resolutionDate.isAfter(rels.get(rels.size() - 1).getDate())) {
            /* this ticket has been resolved in a release that hasn't been released yet, so it is ignored */
            throw new InvalidTicketException();
        }
        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();
        ticket.fixVersion = getRelease(resolutionDate, rels);
        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        ticket.openingVersion = getRelease(creationDate, rels);


        JSONArray versions = fields.getJSONArray("versions");

        if (versions.isNull(0)) {
            // this ticket does not have the affected versions field
            ticket.affectedVersions = null;
        } else {
            for (int i = 0; i < versions.length(); i++) {
                if (versions.getJSONObject(i).has("releaseDate"))
                    ticket.affectedVersions.add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay(), rels));
                }
            }


        if (ticket.affectedVersions != null && !ticket.affectedVersions.isEmpty()) {
            Release injVersion = ticket.affectedVersions.get(0);
            for (Release affVersion: ticket.affectedVersions) {
                if (affVersion.getId() < injVersion.getId())
                    injVersion = affVersion;
            }
            ticket.injectedVersion = injVersion;
            Proportion.computeProportion(ticket);
        }


            return ticket;
        }
    public static float getProportionMean(List<Ticket> tickets) {
        float sum = 0;
    int count = 0;
    for (Ticket ticket : tickets) {
        if (ticket.proportion > 0) {
            sum += ticket.proportion;
            count++;
        }
    }
        return sum/count;
    }

    public static Release getRelease(LocalDateTime date, List<Release> rels) throws JSONException, IOException {
        int i = 0;
        while (rels.get(i).getDate().isBefore(date)) {
            i++;
        }
        return rels.get(i);
    }


   /* public static void computeProportion(Ticket ticket) {

        if (ticket.fixVersion.getId() > ticket.openingVersion.getId()
                && ticket.fixVersion.getId() > ticket.injectedVersion.getId()
                && ticket.injectedVersion.getId() < ticket.openingVersion.getId()) {
            ticket.proportion = (float)(ticket.fixVersion.getId() - ticket.injectedVersion.getId())/(ticket.fixVersion.getId() - ticket.openingVersion.getId());ù
        System.out.println(ticket.proportion);
        }
    } */
   public static List<Ticket> getTicketsWithAV(List<Ticket> tickets) {
       List<Ticket> filteredTickets = filterTickets(tickets);
       System.out.println(filteredTickets.size());
        List<Ticket> ticketsWithAV = new ArrayList<>();
        for (Ticket ticket : filteredTickets) {
            if (ticket.fixVersion.getId() != ticket.injectedVersion.getId()) {
                // these tickets have fix version different from the injected version, so they have AV
                ticketsWithAV.add(ticket);
            }
        }

        return ticketsWithAV;
    }
    private static List<Ticket> filterTickets(List<Ticket> tickets) {
       List<Ticket> filteredList = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.injectedVersion.getId() < Math.round(releases.size()/2))
                filteredList.add(ticket);
        }
        return filteredList;
    }

}
