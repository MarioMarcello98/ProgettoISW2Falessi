package entities;

import java.util.ArrayList;

public class Ticket {
    public String id;
    public String key;
    public ArrayList<Integer> affectedVersions = new ArrayList<>();
    public int fixVersion;    // fix version
    public int openingVersion;      // opening version
    public int injectedVersion;
    public float proportion = 0;
}
