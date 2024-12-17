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
import org.json.JSONObject;
import org.json.JSONArray;

public class GetJiraInfo {
    public static HashMap<LocalDateTime, String> releaseNames;
    public static HashMap<LocalDateTime, String> releaseID;
    //public static ArrayList<LocalDateTime> releases;    public static Integer numVersions;
    public static ArrayList<Release> releases;
    public static ArrayList<Release> getReleaseInfo(String projName) throws IOException, JSONException {
        releases = new ArrayList<>();
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        releaseNames = new HashMap<LocalDateTime, String>();
        releaseID = new HashMap<LocalDateTime, String> ();
        ArrayList<LocalDate> dateArray = new ArrayList<>();
        for (i = 0; i < versions.length(); i++ ) {
            dateArray.add(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()));
        }
        Collections.sort(dateArray);
        ArrayList<JSONObject> releasesOrderedArray = new ArrayList<>();
        i = 0;
        do {
            for (int j = 0; j < versions.length(); j++) {
                if (LocalDate.parse(versions.getJSONObject(j).get("releaseDate").toString()).isEqual(dateArray.get(i))) {
                    releasesOrderedArray.add(i, versions.getJSONObject(j));
                    i++;
                    break;
                }


            }
        }   while (i < versions.length());
        for (i = 0; i < versions.length(); i++ ) {
            addRelease(i, releasesOrderedArray.get(i));
        }
        return releases;
    }
    public static void addRelease(int id, JSONObject release) {
        LocalDate releaseDate = LocalDate.parse(release.get("releaseDate").toString());
        LocalDateTime releaseDateTime = releaseDate.atStartOfDay();
        Release r = new Release(id, release.get("name").toString(), releaseDateTime);
        releases.add(r);
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
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