package retrievers;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class GetJiraInfo {
    public static int numIssues;
    public static ArrayList<String> issuesKeys;
    // https://issues.apache.org/jira/rest/api/2/search?jql=project=bookkeeper+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed
    public static void main(String[] args) throws JSONException, IOException {
        String projectName = "BOOKKEEPER";
        String query = "search?jql=project=" + projectName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=500";
        String url = "https://issues.apache.org/jira/rest/api/2/" + query;
        JSONObject json = readJsonFromUrl(url);
        JSONArray issues = json.getJSONArray("issues");
        issuesKeys = new ArrayList<>();
        System.out.println(issues.length());
        for (int i = 0; i < issues.length(); i++) {
            String key = "";
            if (issues.getJSONObject(i).has("key")) {
                issuesKeys.add(issues.getJSONObject(i).get("key").toString());
            }
        }
        System.out.println(url);
        System.out.println(issuesKeys);
    }
    public static void getFields(JSONArray issues) {
    }
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
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

