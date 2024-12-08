package retrievers;

import entities.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import retrievers.GetJiraInfo;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;
import java.net.URL;

public class TicketRetriever {
    public static ArrayList<Ticket> tickets;
    public static ArrayList<LocalDateTime> versions = new ArrayList<>();

    public static void main(String[] args) throws JSONException, IOException {
        String projectName = "BOOKKEEPER";
        String query = "search?jql=project=" + projectName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000";
        String url = "https://issues.apache.org/jira/rest/api/2/" + query;

        JSONObject json = readJsonFromUrl(url);
        JSONArray issues = json.getJSONArray("issues");
        System.out.println(issues.length());

        tickets = new ArrayList<>();

        versions = GetJiraInfo.GetJiraInfo(projectName);
        System.out.println(versions);
        for (int i = 0; i < issues.length(); i++) {
            try {
                tickets.add(getTicket(issues.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        /*
        int x = 0;
        for (Ticket ticket : tickets) {
            System.out.println(ticket.id);
            System.out.println(ticket.key);
            System.out.println(ticket.affectedVersions);
            System.out.println(ticket.openingVersion);
            System.out.println(ticket.injectedVersion);
            System.out.println(ticket.fixVersion);
            System.out.println('\n');
            if (!ticket.affectedVersions.isEmpty())
                x++;
        }
        System.out.println(x);
         */
        System.out.println("proportion mean: " + getProportionMean());
        Proportion.estimateInjectedVersion(tickets, getProportionMean());
        for (Ticket ticket : tickets) {
            System.out.println("id: " + ticket.id);
            System.out.println(ticket.key);
            System.out.println("Affected versions: " + ticket.affectedVersions);
            System.out.println("Opening version: " + ticket.openingVersion);
            System.out.println("Injected version: " + ticket.injectedVersion);
            System.out.println("Fix version: " + ticket.fixVersion);
            System.out.println('\n');
        }
    }

    public static Ticket getTicket(JSONObject ticketInfo) throws JSONException {

        Ticket ticket = new Ticket();

        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();


        JSONObject fields = ticketInfo.getJSONObject("fields");


        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        ticket.fixVersion = getVersion(resolutionDate);
        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        ticket.openingVersion = getVersion(creationDate);



        JSONArray versions = fields.getJSONArray("versions");

        String name = "";

        ArrayList<LocalDateTime> injectedVersion = new ArrayList<>();
        injectedVersion.add(resolutionDate);

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);

            LocalDate releaseDate = LocalDate.parse(version.get("releaseDate").toString());
            LocalDateTime releaseDateTime = releaseDate.atStartOfDay();


                if (releaseDateTime.isBefore(resolutionDate)) {
                    ticket.affectedVersions.add(getVersion(releaseDateTime));
                }
            }


                if (!ticket.affectedVersions.isEmpty()) {
                    Collections.sort(ticket.affectedVersions);
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

    private static int getVersion(LocalDateTime date) {
        int i = 0;
        while (versions.get(i).isBefore(date)) {
            i++;
        }
        return i;
    }
    /*
     *   proportion = (fixVersion - injectedVersion)/(fixVersion - openingVersion)
     */
    public static void computeProportion(Ticket ticket) {

        if (ticket.fixVersion > ticket.openingVersion && ticket.fixVersion > ticket.injectedVersion && ticket.injectedVersion < ticket.openingVersion) {
            ticket.proportion = (float)(ticket.fixVersion - ticket.injectedVersion)/(ticket.fixVersion - ticket.openingVersion);
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
