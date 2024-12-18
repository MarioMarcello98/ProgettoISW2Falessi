package retrievers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;

import java.util.Collections;
import java.util.Comparator;
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
        for (i = 0; i < versions.length(); i++) {
            dateArray.add(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()));
        }
        Collections.sort(dateArray);
        ArrayList<org.codehaus.jettison.json.JSONObject> releasesOrderedArray = new ArrayList<>();
        i = 0;
        do {
            for (int j = 0; j < versions.length(); j++) {
                if (LocalDate.parse(versions.getJSONObject(j).get("releaseDate").toString()).isEqual(dateArray.get(i))) {
                    releasesOrderedArray.add(i, versions.getJSONObject(j));
                    i++;
                    break;
                }


            }
        } while (i < versions.length());
        for (i = 0; i < versions.length(); i++) {
            addRelease(i, releasesOrderedArray.get(i));
        }
        return releases;
    }

    public static void addRelease(int id, JSONObject release) throws org.codehaus.jettison.json.JSONException {
        LocalDate releaseDate = LocalDate.parse(release.get("releaseDate").toString());
        LocalDateTime releaseDateTime = releaseDate.atStartOfDay();
        Release r = new Release(id, release.get("name").toString(), releaseDateTime);
        releases.add(r);
    }

}