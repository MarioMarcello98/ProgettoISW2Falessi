import entities.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
public class GetTicketInfo {
    public static ArrayList<Ticket> tickets;
    public static HashSet<LocalDateTime> allVersionsHashSet = new HashSet<>();
    public static List<LocalDateTime> allVersions = new ArrayList<>();
    // https://issues.apache.org/jira/rest/api/2/search?jql=project=bookkeeper+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed
    public static void main(String[] args) throws JSONException, IOException {
        String projectName = "BOOKKEEPER";
        String query = "search?jql=project=" + projectName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=500";
        String url = "https://issues.apache.org/jira/rest/api/2/" + query;
        JSONObject json = readJsonFromUrl(url);
        JSONArray issues = json.getJSONArray("issues");
        System.out.println(issues.length());
        tickets = new ArrayList<>();
        for (int i = 0; i < issues.length(); i++) {
            try {
                tickets.add(getTicket(issues.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        System.out.println(url);
        /*
        int x = 0;
        for (Ticket ticket : tickets) {
            System.out.println(ticket.id);
            System.out.println(ticket.key);
            System.out.println(ticket.affectedVersions);
            System.out.println(ticket.resolutionDate);
            System.out.println(ticket.injectedVersion);
            System.out.println('\n');
            if (!ticket.affectedVersions.isEmpty())
                x++;
        }
        System.out.println(x);
         */
        System.out.println(allVersionsHashSet);
        allVersions.addAll(allVersionsHashSet);
        Collections.sort(allVersions);
        System.out.println(allVersions);
    }
    public static Ticket getTicket(JSONObject ticketInfo) throws JSONException {
        Ticket ticket = new Ticket();
        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();
        JSONObject fields = ticketInfo.getJSONObject("fields");
        ticket.resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        ticket.creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        JSONArray versions = fields.getJSONArray("versions");
        String name = "";
        HashMap<String, LocalDateTime> injectedVersion = new HashMap<>();
        injectedVersion.put(name, ticket.resolutionDate);
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            LocalDate releaseDate = LocalDate.parse(version.get("releaseDate").toString());
            LocalDateTime releaseDateTime = releaseDate.atStartOfDay();
            if (releaseDateTime.isBefore(ticket.resolutionDate)) {
                ticket.affectedVersions.put(version.get("name").toString(), releaseDateTime);
                if (injectedVersion.get(name).isAfter(releaseDateTime)) {
                    injectedVersion.remove(name);
                    name = version.get("name").toString();
                    injectedVersion.put(name, releaseDateTime);
                    allVersionsHashSet.add(injectedVersion.get(name));
                }
            }
        }
        if (!injectedVersion.containsKey("")) {
            ticket.injectedVersion = injectedVersion;
            ticket.proportion =
        }
        return ticket;
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