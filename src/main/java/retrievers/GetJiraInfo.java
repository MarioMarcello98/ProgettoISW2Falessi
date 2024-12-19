package retrievers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;

import java.time.LocalDateTime;

import utils.CSV;
import entities.Release;
import org.json.JSONException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import static utils.JSON.readJsonFromUrl;

public class GetJiraInfo {
    public static HashMap<LocalDateTime, String> releaseNames;
    public static HashMap<LocalDateTime, String> releaseID;
    //public static ArrayList<LocalDateTime> releases;    public static Integer numVersions;
    public static ArrayList<Release> releases;

    public static ArrayList<Release> getReleaseInfo(String projName) throws IOException, JSONException, org.codehaus.jettison.json.JSONException {
        releases = new ArrayList<>();
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        org.codehaus.jettison.json.JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        releaseNames = new HashMap<LocalDateTime, String>();
        releaseID = new HashMap<LocalDateTime, String>();
        ArrayList<LocalDate> dateArray = new ArrayList<>();
        JSONArray versionsWithReleaseDate = new JSONArray();
        for (i = 0; i < versions.length(); i++) {
            if (versions.getJSONObject(i).has("releaseDate")) {
                dateArray.add(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()));
                versionsWithReleaseDate.put(versions.getJSONObject(i));
            }
        }
        Collections.sort(dateArray);
        ArrayList<org.codehaus.jettison.json.JSONObject> releasesOrderedArray = new ArrayList<>();
        i = 0;
        do {
            for (int j = 0; j < versionsWithReleaseDate.length(); j++) {
                if (LocalDate.parse(versionsWithReleaseDate.getJSONObject(j).get("releaseDate").toString()).isEqual(dateArray.get(i))) {
                    releasesOrderedArray.add(i, versionsWithReleaseDate.getJSONObject(j));
                    i++;
                    break;
                }


            }
        } while (i < versionsWithReleaseDate.length());
        int j = 0;
        for (i = 0; i < versionsWithReleaseDate.length(); i++) {
            if (!existsReleaseWithDate(LocalDate.parse(releasesOrderedArray.get(i).get("releaseDate").toString()))) {
                addRelease(j + 1, releasesOrderedArray.get(i));
                j++;
            }

        CSV.generateCSVForVersions(releases, projName);
        }
        return releases;
    }

    private static void addRelease(int id, JSONObject release) throws org.codehaus.jettison.json.JSONException {
        LocalDate releaseDate = LocalDate.parse(release.get("releaseDate").toString());
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