package retrievers;
import exception.ExecutionException;
import entities.Release;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.json.JSONException;
import utils.CSV;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static utils.JSON.readJsonFromUrl;

public class GetJiraInfo {
    private static List<Release> releases;
    private static final String RELEASE_DATE = "releaseDate";
    private GetJiraInfo() {}

    public static List<Release> getReleases() {
        return releases;
    }

    public static List<Release> getReleaseInfo(String projName, boolean ignoreCSV, int numVersions, boolean splitReleases) throws IOException, JSONException, org.codehaus.jettison.json.JSONException, ExecutionException {
        releases = new ArrayList<>();
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        org.codehaus.jettison.json.JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        ArrayList<LocalDate> dateArray = new ArrayList<>();
        JSONArray versionsWithReleaseDate = new JSONArray();
        for (i = 0; i < versions.length(); i++) {
            if (versions.getJSONObject(i).has(RELEASE_DATE)) {
                dateArray.add(LocalDate.parse(versions.getJSONObject(i).get(RELEASE_DATE).toString()));
                versionsWithReleaseDate.put(versions.getJSONObject(i));
            }
        }
        Collections.sort(dateArray);
        ArrayList<org.codehaus.jettison.json.JSONObject> releasesOrderedArray = new ArrayList<>();
        i = 0;
        do {
            for (int j = 0; j < versionsWithReleaseDate.length(); j++) {
                if (LocalDate.parse(versionsWithReleaseDate.getJSONObject(j).get(RELEASE_DATE).toString()).isEqual(dateArray.get(i))) {
                    releasesOrderedArray.add(i, versionsWithReleaseDate.getJSONObject(j));
                    i++;
                    break;
                }
            }
        } while (i < versionsWithReleaseDate.length());
        int j = 0;
        for (i = 0; i < versionsWithReleaseDate.length(); i++) {
            if (!existsReleaseWithDate(LocalDate.parse(releasesOrderedArray.get(i).get(RELEASE_DATE).toString()))) {
                addRelease(j + 1, releasesOrderedArray.get(i));
                j++;
            }
        }
        if (!ignoreCSV)
            CSV.generateCSVForVersions(releases, projName);
        if (numVersions > 0)
            return releases.subList(0, numVersions);
        else if (splitReleases)
            return releases.subList(0, Math.round((float)releases.size() / 2));
        return releases;
    }

    private static void addRelease(int id, JSONObject release) throws org.codehaus.jettison.json.JSONException {
        LocalDate releaseDate = LocalDate.parse(release.get(RELEASE_DATE).toString());
        LocalDateTime releaseDateTime = releaseDate.atStartOfDay();
        Release r = new Release(id, release.get("name").toString(), releaseDateTime);
        releases.add(r);
    }

    private static boolean existsReleaseWithDate(LocalDate localDate) {
        for (Release release : releases) {
            if (Objects.equals(release.getDate(), localDate.atStartOfDay()))
                return true;
        }
        return false;
    }
}