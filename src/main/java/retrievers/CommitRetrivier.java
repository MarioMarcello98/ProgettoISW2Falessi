package retrievers;

import entities.Class;
import entities.Release;
import entities.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.jgit.util.io.NullOutputStream;
import utils.CSV;
import utils.FilterCommit;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

public class CommitRetriever {
    private static Repository repository;
    public static List<Class> retrieveCommits(String projName, List<Ticket> allTickets, int numVersions, boolean computeMetrics) throws IOException, JSONException, GitAPIException {
        List<Class> allClasses = new ArrayList<>();
        List<RevCommit> commits = new ArrayList<>();
        List<Class> filteredClasses = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, false, numVersions, false);
        System.out.println("retrieving the commits for " + projName);
        System.out.println(releases.size());
        File file = new File(projName.toLowerCase());
        if (file.exists() && file.isDirectory())
            repository = new FileRepository(projName.toLowerCase() + "/.git/");
        else {
            System.out.println("Cloning repository...");
            repository = Git.cloneRepository().setURI("https://github.com/apache/" + projName.toLowerCase() + ".git").call().getRepository();
        }
        try (Git git = new Git(repository)) {
            LocalDateTime lastRelease = releases.get(releases.size() - 1).getDate();
            String date = lastRelease.getYear() + "-" + lastRelease.getMonthValue() + "-" + lastRelease.getDayOfMonth() + ":23.59";
            Date lastReleaseDate = new SimpleDateFormat("yyyy-MM-dd:hh.mm").parse(date);
            System.out.println("Last release: " + lastRelease);
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    Date commitDate = commit.getAuthorIdent().getWhen();
                    System.out.println(commitDate);
                    if (!commits.contains(commit)) {
                        if (commitDate.before(lastReleaseDate) || commitDate.equals(lastReleaseDate))
                            commits.add(commit);
                    }
                }
            }
            System.out.println("Number of total commits: " + commits.size());
            for (Ticket ticket : allTickets) {
                System.out.println(ticket.key);
            }
            for (RevCommit commit : commits) {
                System.out.println(commit.getShortMessage());
            }
            for (int i = 0; i < releases.size(); i++) {
                LocalDateTime firstDate;
                if (i == 0) {
                    firstDate = LocalDateTime.of(1970, 1, 1, 0, 0);
                    initReleaseCommits(releases.get(i), firstDate, commits);
                } else {
                    firstDate = releases.get(i - 1).getDate();
                    initReleaseCommits(releases.get(i), firstDate, commits);
                }
            }
            System.out.println("Last commits initialized.");
            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty())
                    System.out.println(release.getId() + ": " + release.getAssociatedCommits().size() + ", last commit: " + release.getLastCommit().getAuthorIdent().getWhen() + " - " + release.getLastCommit().getShortMessage());
            }
            System.out.println("getting the classes");
            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty()) {
                    allClasses.addAll(getClassesFromReleaseCommit(release));
                }
            }
            System.out.println("Retrieved classes: " + allClasses.size());
            int count;
            for (Release release : releases) {
                count = 0;
                for (Class c : allClasses) {
                    if (c.getRelease().getId() == release.getId())
                        count++;
                }
                System.out.println("Classes for release " + release.getId() + ": " + count);
            }
            retrieveCommitsForClasses(commits, allClasses);
            System.out.println("commits associated to classes: " + commits.size());
            System.out.println("CHECK");
            System.out.println("tickets: " + allTickets.size());
            System.out.println("commits: " + commits.size());
            System.out.println("classes: " + allClasses.size());
            System.out.println("releases: " + releases.size());
            labelBuggyClasses(allTickets, commits, allClasses, releases);
            count = 0;
            List<Integer> versions = new ArrayList<>();
            for (Class cls : allClasses) {
                if (cls.isBuggy()) {
                    versions.add(cls.getRelease().getId());
                    count++;
                }
            }
            System.out.println("All classes: " + allClasses.size());
            System.out.println("Buggy classes: " + count);
            System.out.println(numVersions + ": Versions of the buggy classes: " + versions);
            ComputeMetrics cm = new ComputeMetrics();
            cm.computeMetrics(allClasses, projName);
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return allClasses;
    }

    private static void labelClasses(String className, Ticket ticket, List<Class> allClasses) {
        if (ticket.fixVersion == null)
            return;
        for (Class cls : allClasses) {
            if (cls.getName().equals(className) && cls.getRelease().getId() >= ticket.injectedVersion.getId() && cls.getRelease().getId() < ticket.fixVersion.getId()) {
                cls.setBuggy(true);
                System.out.println("Class " + cls.getName() + " of release " + cls.getRelease().getId() + "is buggy according to ticket " + ticket.key);
            }
        }
    }

    private static void labelBuggyClasses(List<Ticket> tickets, List<RevCommit> commits, List<Class> allClasses, List<Release> releases) throws JSONException, IOException, GitAPIException {
        System.out.println("LABELING");
        List<Ticket> ticketsWithAV = GetTicketInfo.getTicketsWithAV(tickets);
        System.out.println("Tickets with AV: " + ticketsWithAV.size() + " - " + releases.size());
        for (Ticket ticket : ticketsWithAV) {
            List<RevCommit> commitsAssociatedToTicket = FilterCommit.filterCommitsAssociatedToTicket(ticket, commits);
            System.out.println("Ticket " + ticket.key + " has " + commitsAssociatedToTicket.size() + " associated commits.");
            for (RevCommit commit : commitsAssociatedToTicket) {
                List<String> modifiedClassesNames = getModifiedClasses(commit);
                for (String modifiedClass : modifiedClassesNames) {
                    labelClasses(modifiedClass, ticket, allClasses);
                }
            }
        }
    }

    public static List<String> getModifiedClasses(RevCommit commit) {
        List<String> modifiedClasses = new ArrayList<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            ObjectId tree = commit.getTree();
            treeParser.reset(reader, tree);
            CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
            ObjectId parentTree = commit.getParent(0).getTree();
            parentTreeParser.reset(reader, parentTree);
            diffFormatter.setRepository(repository);
            List<DiffEntry> diffEntries = diffFormatter.scan(parentTree, tree);
            for (DiffEntry entry : diffEntries) {
                if (entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test/"))
                    modifiedClasses.add(entry.getNewPath());
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modifiedClasses;
    }

    private static void retrieveCommitsForClasses(List<RevCommit> commits, List<Class> allClasses) throws IOException, JSONException {
        for (RevCommit commit : commits) {
            List<String> modifiedClasses = getModifiedClasses(commit);
            for (String modifiedClass : modifiedClasses) {
                for (Class cls : allClasses) {
                    if (!cls.getAssociatedCommits().contains(commit)) {
                        if (modifiedClass.equals(cls.getName()) && getReleaseFromCommit(commit).getId() == cls.getRelease().getId())
                            cls.getAssociatedCommits().add(commit);
                    }
                }
            }
        }
    }

    private static List<Class> getClassesFromReleaseCommit(Release release) throws IOException {
        List<Class> classes = new ArrayList<>();
        Map<String, String> classesDescription = getClassesFromCommit(release.getLastCommit());
        for(Entry entry : classesDescription.entrySet()) {
            classes.add(new Class(entry.getKey().toString(), entry.getValue().toString(), release));
        }
        return classes;
    }

    private static void initReleaseCommits(Release release, LocalDateTime firstDateTime, List<RevCommit> commits) {
        LocalDateTime lastDateTime = release.getDate();
        List<RevCommit> associatedCommits = new ArrayList<>();
        for (RevCommit commit : commits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
            if (commitDate.isAfter(firstDateTime) && commitDate.isBefore(lastDateTime) || commitDate.isEqual(lastDateTime))
                associatedCommits.add(commit);
        }
        release.setAssociatedCommits(associatedCommits);
        initializeReleaseLastCommit(release);
    }

    private static void initializeReleaseLastCommit(Release release) {
        if (release.getAssociatedCommits().isEmpty())
            return;
        RevCommit lastCommit = release.getAssociatedCommits().get(0);
        for (RevCommit commit : release.getAssociatedCommits()) {
            if (commit.getAuthorIdent().getWhen().after(lastCommit.getAuthorIdent().getWhen())) {
                lastCommit = commit;
            }
        }
        release.setLastCommit(lastCommit);
    }

    private static Map<String, String> getClassesFromCommit(RevCommit commit) throws IOException {
        Map<String, String> classDescription = new HashMap<>();
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            if (treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                String path = treeWalk.getPathString();
                String content = new String(repository.open(treeWalk.getObjectId(0)).getBytes());
                classDescription.put(path, content);
            }
        }
        treeWalk.close();
        return classDescription;
    }

    private static Release getReleaseFromCommit(RevCommit commit) throws JSONException, IOException {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return GetTicketInfo.getRelease(commitDate);
    }
}