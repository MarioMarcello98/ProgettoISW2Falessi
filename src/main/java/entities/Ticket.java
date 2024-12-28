package entities;


import java.util.ArrayList;
import java.util.List;
import java.util.List;

public class Ticket {
private String id;
private String key;
private List<Release> affectedVersions = new ArrayList<>();
private Release fixVersion;    // fix version
private Release openingVersion;      // opening version
private Release injectedVersion = null;
private List<String> affectedComponents = new ArrayList<>();
private float proportion = 0;

public String getId() {
    return id;
}

public void setId(String id) {
    this.id = id;
}
public String getKey() {
    return key;
}
public void setKey(String key) {
    this.key = key;
}
public List<Release> getAffectedVersions() {
    return affectedVersions;
}
public void setAffectedVersions(List<Release> affectedVersions) {
    this.affectedVersions = affectedVersions;
}
public Release getFixVersion() {
    return fixVersion;
}
public void setFixVersion(Release fixVersion) {
    this.fixVersion = fixVersion;
}
public Release getOpeningVersion() {
    return openingVersion;
}
public void setOpeningVersion(Release openingVersion) {
    this.openingVersion = openingVersion;
}
public Release getInjectedVersion() {
    return injectedVersion;
}
public void setInjectedVersion(Release injectedVersion) {
    this.injectedVersion = injectedVersion;
}
public List<String> getAffectedComponents() {
    return affectedComponents;
}
public void setAffectedComponents(List<String> affectedComponents) {
    this.affectedComponents = affectedComponents;
}
public float getProportion() {
    return proportion;
}
public void setProportion(float proportion) {
    this.proportion = proportion;
}
}