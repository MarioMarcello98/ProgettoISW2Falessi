package retrievers;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import exception.ExecutionException;
import exception.InvalidTicketException;
import entities.Release;
import entities.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static utils.JSON.readJsonFromUrl;
public class Proportion {

    static org.slf4j.Logger logger = LoggerFactory.getLogger(Proportion.class);
    private Proportion() {
    }
    private static List<String> allProjects = Arrays.asList(
            "AVRO",
            "OPENJPA",
            "STORM",
            "ZOOKEEPER",
            "SYNCOPE",
            "TAJO",
            "BOOKKEEPER"
    );

    private static void initFile(String projName) throws ExecutionException {
        try (FileWriter fileWriter = new FileWriter("Proportion" + projName + ".csv")) {
            logger.warn("Proportion.csv does not exist for {}", projName);
            fileWriter.append("Project, Proportion Value");
            fileWriter.append("\n");
            for (String project : allProjects) {
                if (!project.equals(projName)) {
                    fileWriter.append(project).append(", ").append(String.valueOf(getProportionForProject(project)));
                    fileWriter.append("\n");
                }
            }
        } catch (IOException | JSONException e) {
            throw new ExecutionException(e);
        }
    }

    public static void coldStartProportion(List<Ticket> tickets, String projName) throws JSONException, ExecutionException, IOException {
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);
        Path proportionFile = Paths.get("proportion.Proportion" + projName + ".csv");
        if (!Files.exists(proportionFile)) {
            initFile(projName);
        }
        List<Float> proportionValues = readProportionFile(projName);
        Collections.sort(proportionValues);
        int proportionValue = Math.round(median(proportionValues));
        logger.info("Proportion value: {}", proportionValue);
        for (Ticket ticket : tickets) {
            if (ticket.getInjectedVersion() == null && ticket.getFixVersion() != null) {
                if (ticket.getOpeningVersion().getId() == ticket.getFixVersion().getId())
                    ticket.setInjectedVersion(releases.get(Math.max(0, ticket.getFixVersion().getId() - 3)));
                else
                    ticket.setInjectedVersion(releases.get(
                            Math.max(0, Math.round((float) ticket.getFixVersion().getId() - (ticket.getFixVersion().getId() - ticket.getOpeningVersion().getId()) * proportionValue) - 1)));
            }
        }
    }

    private static ArrayList<Float> readProportionFile(String projName) throws IOException, ExecutionException {
        ArrayList<Float> proportionValues = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader("Proportion" + projName + ".csv"))) {
            List<String[]> r = csvReader.readAll();
            for (String[] row : r) {
                for (String proj : allProjects) {
                    if (Arrays.stream(row).toArray()[0].toString().equals(proj)) {
                        proportionValues.add(Float.parseFloat(Arrays.stream(row).toArray()[1].toString().substring(1)));
                    }
                }
            }
        } catch (IOException | CsvException e) {
            throw new ExecutionException(e);
        }
        return proportionValues;
    }

    private static float median(List<Float> list) {
        float sum;
        if (list.size() % 2 == 0) {
            sum = list.get(list.size() / 2 - 1) + list.get(list.size() / 2);
            return sum / 2;
        } else {
            return list.get(list.size() / 2);
        }
    }

    public static void computeProportion(Ticket ticket) {
        if (ticket.getFixVersion() == null) {
            return;
        }
        if (ticket.getInjectedVersion().getId() <= ticket.getOpeningVersion().getId() && ticket.getFixVersion().getId() == ticket.getOpeningVersion().getId())
            ticket.setProportion((float)ticket.getFixVersion().getId() - ticket.getInjectedVersion().getId());
        else if (ticket.getInjectedVersion().getId() <= ticket.getOpeningVersion().getId() && ticket.getOpeningVersion().getId() < ticket.getFixVersion().getId()) {
            ticket.setProportion((float) (ticket.getFixVersion().getId() - ticket.getInjectedVersion().getId()) / (ticket.getFixVersion().getId() - ticket.getOpeningVersion().getId()));
        }

    }

    public static float getProportionForProject(String projName) throws JSONException, IOException, ExecutionException {
        int startAt = 0;
        ArrayList<Ticket> tickets = new ArrayList<>();
        JSONObject json;
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, true, 0, false);
        logger.info("Retrieving tickets for project {}", projName);
        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;
            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(GetTicketInfo.getTicket(issues.getJSONObject(i), releases, releases.get(releases.size() - 1).getDate()));
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
        logger.info("Tickets having proportion (project {}): {} over {} tickets", projName, count, tickets.size());
        logger.info("proportion mean (project {} ): {} ", projName, GetTicketInfo.getProportionMean(tickets));
        return GetTicketInfo.getProportionMean(tickets);
    }
}