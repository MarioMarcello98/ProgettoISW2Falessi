package main;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import weka.Weka;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<List<File>> files = WalkForward.initSets("BOOKKEEPER");
            //List<List<File>> files = WalkForward.initSets("OPENJPA");
            WalkForward.classify("BOOKKEEPER");
            //WalkForward.classify("OPENJPA");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}