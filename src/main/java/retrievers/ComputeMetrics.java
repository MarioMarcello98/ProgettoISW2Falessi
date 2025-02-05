package retrievers;
import exception.ExecutionException;
import entities.Class;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputeMetrics {
    private String projName;

    private void setSize(Class c) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(c.getImplementation());
        int lines = 0;
        while (m.find())
        {
            lines ++;
        }
        c.setSize(lines);
    }

    private void setNAuth(Class c) {
        Set<String> authors = new HashSet<>();
        for (RevCommit commit : c.getAssociatedCommits()) {
            authors.add(commit.getAuthorIdent().getName());
        }
        c.setnAuth(authors.size());
    }

    private void setNR(Class c) {
        c.setNR(c.getAssociatedCommits().size());
    }


    private void setLOCAndChurn(Class c) throws IOException{
        List<RevCommit> commits = c.getAssociatedCommits();
        List<Integer> locAdded = new ArrayList<>();
        List<Integer> locDeleted = new ArrayList<>();
        Repository repository = new FileRepository(projName.toLowerCase() + "/.git/");

        for (RevCommit commit : commits) {
            try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                RevCommit parentCommit = commit.getParent(0);
                df.setRepository(repository);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                List<DiffEntry> diffEntryList = df.scan(parentCommit.getTree(), commit.getTree());

                for (DiffEntry diffEntry : diffEntryList) {
                    if (diffEntry.getNewPath().equals(c.getName())) {
                        int line= 0;
                        int delLine=0;
                        for (Edit edit : df.toFileHeader(diffEntry).toEditList()) {
                            line += edit.getEndB() - edit.getBeginB();
                            delLine += edit.getEndA() - edit.getBeginA();
                        }
                        locAdded.add(line);
                        locDeleted.add(delLine);
                    }
                }
            }
        }

        locAddedMetrics(c, locAdded);
        churnMetrics(c, locAdded, locDeleted);
        locTouched(c, locAdded, locDeleted);
    }
    private void locAddedMetrics(Class c, List<Integer> locAdded) {
        int maxLOC = 0;
        int sumLines = 0;
        for (Integer line : locAdded) {
            sumLines += line;
            if (line > maxLOC)
                maxLOC = line;
        }
        c.setLOCAdded(sumLines);
        c.setMaxLOCAdded(maxLOC);
        c.setAverageLOCAdded((int)(1.0 * sumLines / locAdded.size()));
    }
    private void churnMetrics(Class c, List<Integer> addedLines, List<Integer> deletedLines) {
        int churnSum = 0;
        int maxChurn = 0;
        for (int i = 0; i < addedLines.size(); i++) {
            int churn = addedLines.get(i) - deletedLines.get(i);
            churnSum += churn;
            if (churn > maxChurn)
                maxChurn = churn;
        }
        c.setChurn(churnSum);
        c.setMaxChurn(maxChurn);
        c.setAverageChurn((float) (1.0 * churnSum / addedLines.size()));
    }
    private void locTouched(Class c, List<Integer> addedLines, List<Integer> deletedLines) {
        int totAddedLines = 0;
        int totDeletedLines = 0;
        for (Integer line : addedLines)
            totAddedLines += line;
        for (Integer line : deletedLines)
            totDeletedLines += line;
        c.setLOCTouched(totAddedLines + totDeletedLines);
    }


    public void computeMetrics(List<Class> allClasses, String projName) throws IOException, ExecutionException {
        this.projName = projName;
        for (Class c : allClasses) {
            setSize(c);
            setNAuth(c);
            setNR(c);
            setLOCAndChurn(c);
        }
    }
}