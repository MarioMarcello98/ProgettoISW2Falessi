package retrievers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
public class GetJiraInfo {
    public static HashMap<LocalDateTime, String> releaseNames;
    public static HashMap<LocalDateTime, String> releaseID;
    public static ArrayList<LocalDateTime> releases;
    public static Integer numVersions;
    public static ArrayList<LocalDateTime> getReleaseInfo(String projName) throws IOException, JSONException {
        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        releases = new ArrayList<LocalDateTime>();
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        releaseNames = new HashMap<LocalDateTime, String>();
        releaseID = new HashMap<LocalDateTime, String> ();
        for (i = 0; i < versions.length(); i++ ) {
            System.out.println(versions.getJSONObject(i));
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
                        name,id);
            }
        }
        // order releases by date
        Collections.sort(releases, new Comparator<LocalDateTime>(){
            //@Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
        });
        return releases;
    }
    public static void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
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