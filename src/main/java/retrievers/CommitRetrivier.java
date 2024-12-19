package retrievers;
import utils.CSV;
import utils.FilterCommit;
import org.codehaus.jettison.json.JSONException;
import static utils.CSV.generateCSV;
import entities.Release;
import entities.Ticket;
import entities.TicketCommit;
import entities.Class;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.jgit.util.io.NullOutputStream;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import java.util.*;

public class CommitRetriever {
    public static List<RevCommit> releaseCommits = new ArrayList<>();
    private static List<Class> allClasses = new ArrayList<>();
    private static Repository repository;
    private static List<RevCommit> commits = new ArrayList<>();
    private static List<Release> releases = new ArrayList<>();
    public static void retrieveCommits(String projName, List<Ticket> allTickets) throws IOException, JSONException {
        releases = GetJiraInfo.getJiraInfo(projName);
        System.out.println("------------- retrieving the commits for " + projName + "-----------------");
        repository = new FileRepository(projName.toLowerCase() + "/.git/");
        try (Git git = new Git(repository)) {
            LocalDateTime lastRelease = GetTicketInfo.releases.get(Math.round(GetTicketInfo.releases.size() / 2)).getDate();
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
                    if (commitDate.isBefore(lastRelease) || commitDate.isEqual(lastRelease) && !commits.contains(commit))
                        commits.add(commit);
                }
            }
            for (int i = 0; i < Math.round(GetTicketInfo.releases.size()/2); i++) {
                LocalDateTime firstDate;
                if (i == 0) {
                    firstDate = LocalDateTime.of(1970, 01, 01, 0, 0);
                    initReleaseCommits(commits, GetTicketInfo.releases.get(i), firstDate);
                } else {
                    firstDate = GetTicketInfo.releases.get(i-1).getDate();
                    initReleaseCommits(commits, GetTicketInfo.releases.get(i), firstDate);
                }
            }
            for (Release release : GetTicketInfo.releases.subList(0, Math.round(GetTicketInfo.releases.size()/2))) {
                if (!release.getAssociatedCommits().isEmpty())
                    System.out.println(release.getId() + ": " + release.getAssociatedCommits().size() + ", last commit: " + release.getLastCommit().getAuthorIdent().getWhen());
            }
            System.out.println("Number of total commits: " + commits.size());
            List<Release> releases = GetTicketInfo.releases.subList(0, Math.round(GetTicketInfo.releases.size()/2));
            List<List<Class>> classes = new ArrayList<>();
            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty()) {
                    System.out.println(release.getId());
                    classes.add(getClassesFromReleaseCommit(release));
                }
            }
            for (List<Class> classList : classes) {
                allClasses.addAll(classList);
            }
            retrieveCommitsForClasses(allClasses, commits);

            for (Class c : allClasses){
                System.out.println(c.getName() + ", " + c.getRelease().getName() + "; " + c.getAssociatedCommits().size());


            }
            System.out.println(commits.size());
            labelBuggyClasses(allTickets);
            int count = 0;
            List<Integer> versions = new ArrayList<>();
            for (Class cls : allClasses) {
                if (cls.isBuggy()) {
                    versions.add(cls.getRelease().getId());
                    count++;
                }
            }
            System.out.println("All classes: " + allClasses.size());
            System.out.println("Buggy classes: " + count);
            System.out.println("Versions of the buggy classes: " + versions);
            ComputeMetrics computeMetrics = new ComputeMetrics();
            computeMetrics.computeMetrics(allClasses);
            CSV.generateCSV(allClasses, projName);

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
    private static List<TicketCommit> filterCommitsWithAssTickets(List<RevCommit> allCommits, List<Ticket> allTickets) {
        List<TicketCommit> commitsWithAssociatedTickets = new ArrayList<>();
        for (RevCommit commit : allCommits) {
            for (Ticket ticket : allTickets) {
                //System.out.println(ticket.key);
                if (commit.getFullMessage().contains(ticket.key + ":") || commit.getFullMessage().contains("[" + ticket.key + "]") && !commitsWithAssociatedTickets.contains(commit)) {
                    //System.out.println(commit.getShortMessage());
                    TicketCommit ticketCommit = new TicketCommit(commit, ticket);
                    commitsWithAssociatedTickets.add(ticketCommit);
                }
            }
        }
        return commitsWithAssociatedTickets;
    }
    private static void labelClasses(List<Class> allClasses, String className, Ticket ticket) {
        for (Class cls : allClasses) {
            if (cls.getName().equals(className) && cls.getRelease().getId() >= ticket.injectedVersion.getId() && cls.getRelease().getId() < ticket.fixVersion.getId()) {
                cls.setBuggy(true);
            }
        }
    }
    private static void labelBuggyClasses(List<Ticket> tickets) {
        List<Ticket> ticketsWithAV = GetTicketInfo.getTicketsWithAV(tickets);        System.out.println("Tickets with AV: " + ticketsWithAV.size());
        for (Ticket ticket : ticketsWithAV) {
            List<RevCommit> commitsAssociatedToTicket = FilterCommit.filterCommitsAssociatedToTicket(ticket, commits);
            for (RevCommit commit : commitsAssociatedToTicket) {
                List<String> modifiedClassesNames = getModifiedClasses(commit);
                for (String modifiedClass : modifiedClassesNames) {
                    labelClasses(allClasses, modifiedClass, ticket);
                }
            }
        }

    }
    public static List<String> getModifiedClasses(RevCommit commit) {
        List<String> modifiedClasses = new ArrayList<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {  // we're not interested in the output
            diffFormatter.setRepository(repository);
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            ObjectId tree = commit.getTree();
            treeParser.reset(reader, tree);
            CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
            ObjectId parentTree = commit.getParent(0).getTree();
            parentTreeParser.reset(reader, parentTree);
            List<DiffEntry> diffEntries = diffFormatter.scan(parentTree, tree);
            for (DiffEntry entry : diffEntries) {
                // change types = ADD, MODIFY, COPY, DELETE, RENAME; we're interested in the modified classes
                if (entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test/"))
                    modifiedClasses.add(entry.getNewPath());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (IncorrectObjectTypeException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modifiedClasses;
    }
    private static void retrieveCommitsForClasses(List<Class> allClasses, List<RevCommit> allCommits) throws IOException, JSONException {
        for (RevCommit commit : allCommits) {
            List<String> modifiedClasses = getModifiedClasses(commit);
            for (String modifiedClass : modifiedClasses) {
                for (Class cls : allClasses) {
                    if (modifiedClass.equals(cls.getName()) && getReleaseFromCommit(commit).getId() == cls.getRelease().getId() && !cls.getAssociatedCommits().contains(commit))
                        cls.getAssociatedCommits().add(commit);
                }
            }
        }
    }
    /* private static void retrieveCommitsForClass(Class c) throws IOException {
         List<RevCommit> releaseCommits = c.getRelease().getAssociatedCommits();
         List<RevCommit> assCommits = new ArrayList<>();
         for (RevCommit commit : releaseCommits) {
             List<String> modifiedClasses = getModifiedClasses(commit);
             for (String className : modifiedClasses) {
                 if (className.equals(c.getName()))
                     assCommits.add(commit);
             }
         }
         c.setAssociatedCommits(assCommits);
     }*/
    private static List<Class> getClassesFromReleaseCommit(Release release) throws IOException {
        List<Class> classes = new ArrayList<>();
        HashMap<String, String> classesDescription = getClassesFromCommit(release.getLastCommit());
        Set<String> names = classesDescription.keySet();
        Collection<String> implementations = classesDescription.values();
        for (int i = 0; i < names.size(); i++) {
            String name = names.toArray()[i].toString();
            String implementation = implementations.toArray()[i].toString();
            Class newClass = new Class(name, implementation, release);
            classes.add(newClass);
        }
        return classes;
    }
    private static void initReleaseCommits(List<RevCommit> allCommits, Release release, LocalDateTime firstDateTime) {
        LocalDateTime lastDateTime = release.getDate();
        List<RevCommit> associatedCommits = new ArrayList<>();
        for (RevCommit commit : allCommits) {
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
    /* public static List<RevCommit> retrieveCommitsForRelease(Release release) {        List<RevCommit> releaseCommits = new ArrayList<>();
        LocalDateTime endDate = release.getDate();
        LocalDateTime startDate;
        if (release.getId() == 0)
            startDate = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0);
        else
            startDate = GetTicketInfo.releases.get(release.getId() - 1).getDate();
        for (RevCommit commit : commits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
            if (endDate.isAfter(commitDate) && startDate.isBefore(commitDate)) {
                releaseCommits.add(commit);
            }
        }

        return releaseCommits;

    } */

    private static HashMap<String, String> getClassesFromCommit(RevCommit commit) throws IOException {
        HashMap<String, String> classDescription = new HashMap<>();
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
        return classDescription;
    }
    private static List<Class> createAllClasses(List<RevCommit> allCommits) throws IOException, JSONException {
        List<Class> classes = new ArrayList<>();
        for (RevCommit commit : allCommits) {
            HashMap<String, String> classesDescription = getClassesFromCommit(commit);
            for (int i = 0; i < classesDescription.size(); i++) {
                Class newClass = new Class(classesDescription.keySet().toArray()[i].toString(), classesDescription.values().toArray()[i].toString(), getReleaseFromCommit(commit));
                classes.add(newClass);
            }
        }

        return classes;
    }
    private static Release getReleaseFromCommit(RevCommit commit) throws JSONException, IOException {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return GetTicketInfo.getRelease(commitDate, releases);
    }
}


