package retrievers;
import utils.CSV;
import utils.FilterCommit;
import java.io.File;
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
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.NullOutputStream;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import java.util.*;

public class CommitRetriever {
    private static Repository repository;
    public static List<Class> retrieveCommits(String projName, List<Ticket> allTickets, int numVersions) throws IOException, JSONException, GitAPIException {
        List<Class> allClasses = new ArrayList<>();
        List<RevCommit> commits = new ArrayList<>();
        List<Release> releases = GetJiraInfo.getReleaseInfo(projName, false, numVersions, true);
        System.out.println(releases.size());
        File file = new File(projName.toLowerCase());
        if (file.exists() && file.isDirectory())
            repository = new FileRepository(projName.toLowerCase() + "/.git/");
        else {
            System.out.println("Cloning repository...");
            repository = Git.cloneRepository().setURI("https://github.com/apache/" + projName.toLowerCase() + ".git").call().getRepository();
        }
        try (Git git = new Git(repository)) {
            LocalDateTime lastRelease = releases.get(releases.size()-1).getDate();
            System.out.println("Last release: " + lastRelease);
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
                    if (!commits.contains(commit)) {
                        if (commitDate.isBefore(lastRelease) || commitDate.isEqual(lastRelease))
                            commits.add(commit);
                    }

                }
            }
            System.out.println("Number of total commits: " + commits.size());
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
                    System.out.println(release.getId() + ": " + release.getAssociatedCommits().size() + ", last commit: " + release.getLastCommit().getAuthorIdent().getWhen());
            }
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
            System.out.println("Classes retrieved");
            retrieveCommitsForClasses(commits, allClasses);
            for (Class c : allClasses){
                System.out.println(c.getName() + ", " + c.getRelease().getName() + "; " + c.getAssociatedCommits().size());


            }
            System.out.println(commits.size());
            labelBuggyClasses(allTickets, commits, allClasses, releases);
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
            computeMetrics.computeMetrics(allClasses, projName);

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return allClasses;
    }

    private static void labelClasses(String className, Ticket ticket, List<Class> allClasses) {
        for (Class cls : allClasses) {
            if (cls.getName().equals(className) && cls.getRelease().getId() >= ticket.injectedVersion.getId() && cls.getRelease().getId() < ticket.fixVersion.getId()) {
                cls.setBuggy(true);
            }
        }
    }
    private static void labelBuggyClasses(List<Ticket> tickets, List<RevCommit> commits, List<Class> allClasses, List<Release> releases) throws JSONException, IOException {
        List<Ticket> ticketsWithAV = GetTicketInfo.getTicketsWithAV(tickets, releases);
        System.out.println("Tickets with AV: " + ticketsWithAV.size());
        for (Ticket ticket : ticketsWithAV) {
            List<RevCommit> commitsAssociatedToTicket = FilterCommit.filterCommitsAssociatedToTicket(ticket, commits);
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
        } catch (ArrayIndexOutOfBoundsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modifiedClasses;
    }
    private static void retrieveCommitsForClasses(List<RevCommit> commits, List<Class> allClasses) throws IOException, JSONException {
        for (RevCommit commit : commits) {
            System.out.println(commit.getShortMessage());
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
        treeWalk.close();
        return classDescription;
    }
    private static Release getReleaseFromCommit(RevCommit commit) throws JSONException, IOException {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return GetTicketInfo.getRelease(commitDate);
    }
}


