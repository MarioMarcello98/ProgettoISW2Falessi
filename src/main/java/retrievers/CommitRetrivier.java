package retrievers;
import entities.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class CommitRetriever {
    public static String projName = "BOOKKEEPER";
    public static LocalDateTime since = GetTicketInfo.releases.get(0).getDate();
    public static LocalDateTime until = GetTicketInfo.releases.get(GetTicketInfo.releases.size() - 1).getDate();
    public static int page = 1;
    public static void retrieveCommits() throws IOException {
        System.out.println("------------- retrieving the commits -----------------");
        FileRepository repository = new FileRepository("bookkeeper/.git");
        try (Git git = new Git(repository)) {
            List<RevCommit> commits = new ArrayList<>();
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    commits.add(commit);
                }
            }
            System.out.println(commits.size());
            for (RevCommit commit : commits) {
                System.out.println("---------------------------------------");
                System.out.println(commit.getAuthorIdent().getWhen());
                System.out.println(commit.getAuthorIdent());
                System.out.println(commit.getFullMessage());
                System.out.println(commit.getCommitTime());
                System.out.println(commit.getName());
                System.out.println(commit.getFooterLines());
            }
            for (int i = 0; i < 14; i++) {
                System.out.println(retrieveCommitsForRelease(commits, GetTicketInfo.releases.get(i)).size());
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<RevCommit> retrieveCommitsForRelease(List<RevCommit> allCommits, Release release) {
        List<RevCommit> releaseCommits = new ArrayList<>();
        LocalDateTime endDate = release.getDate();
        LocalDateTime startDate;
        if (release.getId() == 0)
            startDate = LocalDateTime.of(1900, Month.JANUARY, 1, 0, 0);
        else
            startDate = GetTicketInfo.releases.get(release.getId() - 1).getDate();
        for (RevCommit commit : allCommits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
            if (endDate.isAfter(commitDate) && startDate.isBefore(commitDate)){
                releaseCommits.add(commit);
            }
        }
        return releaseCommits;
    }
}
