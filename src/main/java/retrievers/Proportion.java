package retrievers;
import exception.InvalidTicketException;
import entities.Release;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static utils.JSON.readJsonFromUrl;
import entities.Ticket;
import java.util.ArrayList;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import utils.CSV;

public class Proportion {
    // Predicted IV = FV - (FV - OV) * P
        private static List<String> allProjects = Arrays.asList(
                "AVRO",
                "OPENJPA",
                "STORM",
                "ZOOKEEPER",
                "SYNCOPE",
                "TAJO",
                "BOOKKEEPER"
        );
    public static void coldStartProportion(ArrayList<Ticket> tickets, String projName) throws JSONException, IOException {

        ArrayList<Float> proportionValues = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);
        FileWriter fileWriter = null;
        Path proportionFile = Paths.get("Proportion" + projName + ".csv");
        if (!Files.exists(proportionFile)) {
            try {
                System.out.println("Proportion.csv does not exist for " + projName);
                fileWriter = new FileWriter("Proportion" + projName + ".csv");
                fileWriter.append("Project, Proportion Value");
                fileWriter.append("\n");
                for (String project : allProjects) {
                    if (!project.equals(projName)) {
                        fileWriter.append(project).append(", ").append(String.valueOf(getProportionForProject(project)));
                        fileWriter.append("\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fileWriter.close();
            }
        }

        proportionValues = readProportionFile(projName);
        Collections.sort(proportionValues);
        System.out.println("Proportion values: " + proportionValues);
        System.out.println(median(proportionValues));
        System.out.println("Proportion value: " + proportionValue);
        int proportionValue = Math.round(median(proportionValues));
        for (Ticket ticket : tickets) {
            if (ticket.proportion == 0) {
                ticket.injectedVersion = releases.get(
                        Math.max(0, Math.round((float) ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * proportionValue) - 1)
                );
            }
        }
    }
    private static ArrayList<Float> readProportionFile(String projName) throws IOException {
        CSVReader csvReader = null;
        ArrayList<Float> proportionValues = new ArrayList<>();
        try {
            csvReader = new CSVReader(new FileReader("Proportion" + projName + ".csv"));
            List<String[]> r = csvReader.readAll();
            for (String[] row : r) {
                for (String proj : allProjects) {
                    if (Arrays.stream(row).toArray()[0].toString().equals(proj)) {
                        proportionValues.add(Float.parseFloat(Arrays.stream(row).toArray()[1].toString().substring(1)));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CsvException e) {
            throw new RuntimeException(e);
        } finally {
            csvReader.close();
        }
        return proportionValues;
    }

    private static float median(List<Float> list) {
        float sum;
        if (list.size() % 2 == 0) {
            sum = (float)list.get(list.size() / 2 - 1) + (float)list.get(list.size() / 2);
            return sum/2;
        } else {
            return list.get(list.size() / 2);
        }
    }
    public static void computeProportion(Ticket ticket) {

        if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.fixVersion.getId() == ticket.openingVersion.getId())
            ticket.proportion = ticket.fixVersion.getId() - ticket.injectedVersion.getId();
        else if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.openingVersion.getId() < ticket.fixVersion.getId()) {
            ticket.proportion = (float) (ticket.fixVersion.getId() - ticket.injectedVersion.getId()) / (ticket.fixVersion.getId() - ticket.openingVersion.getId());
        }
    }
    public static float getProportionForProject(String projName) throws JSONException, IOException {
        int startAt = 0;
        ArrayList<Ticket> tickets = new ArrayList<>();
        JSONObject json;
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);        System.out.println("Retrieving tickets for project " + projName);
        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;
            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(GetTicketInfo.getTicket(issues.getJSONObject(i), releases, releases.get(releases.size()-1).getDate()));
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
        System.out.println("Tickets having proportion (project " + projName + "):" + count + " over " + tickets.size() + " tickets");
        System.out.println("proportion mean (project " + projName + "):" + GetTicketInfo.getProportionMean(tickets));
        return GetTicketInfo.getProportionMean(tickets);
    }




    }


