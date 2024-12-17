package retrievers;

import entities.Ticket;
import entities.Release;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import retrievers.GetJiraInfo;
import java.awt.desktop.SystemEventListener;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;
import java.net.URL;

public class GetTicketInfo {
    public static ArrayList<Ticket> tickets;
    public static ArrayList<Release> releases = new ArrayList<>();

    public static void main(String[] args) throws JSONException, IOException {
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
    }

    public static Ticket getTicket(JSONObject ticketInfo) throws JSONException {

        Ticket ticket = new Ticket();

        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();


        JSONObject fields = ticketInfo.getJSONObject("fields");


        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        ticket.fixVersion = getRelease(resolutionDate);
        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        ticket.openingVersion = getRelease(creationDate);



        JSONArray versions = fields.getJSONArray("versions");

        if (versions.isNull(0)) {
            // this ticket does not have the affected versions field
            ticket.affectedVersions = null;
        } else {
            for (int i = 0; i < versions.length(); i++) {
                ticket.affectedVersions.add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay()));
                }
            }


        if (ticket.affectedVersions != null) {
                    ticket.injectedVersion = ticket.affectedVersions.get(0);
                    computeProportion(ticket);
                }


            return ticket;
        }
public static float getProportionMean() {
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

    private static Release getRelease(LocalDateTime date) {
        int i = 0;
        while (releases.get(i).getDate().isBefore(date)) {
            i++;
        }
        return i;
    }
    /*
     *   proportion = (fixVersion - injectedVersion)/(fixVersion - openingVersion)
     *
     *   ASSUMPTION: we're considering in proportion's computation only the tickets with fixVersion > openingVersion > injectedVersion, in order to:
     *   - avoid fixVersion = openingVersion -> proportion = infinity
     *   - avoid fixVersion = injectedVersion -> proportion = 0
     *   - discard invalid affectedVersions, i.e. with openingVersion > fixVersion, injectedVersion > fixVersion and injectedVersion > openingVersion
     */

    public static void computeProportion(Ticket ticket) {

        if (ticket.fixVersion.getId() > ticket.openingVersion.getId()
                && ticket.fixVersion.getId() > ticket.injectedVersion.getId()
                && ticket.injectedVersion.getId() < ticket.openingVersion.getId()) {
            ticket.proportion = (float)(ticket.fixVersion.getId() - ticket.injectedVersion.getId())/(ticket.fixVersion.getId() - ticket.openingVersion.getId());ù
        System.out.println(ticket.proportion);
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

}
