package retrievers;
import entities.Class;
import entities.Ticket;
import entities.TicketCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.InternalHttpServerGlue;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.NullOutputStream;
import utils.FilterCommit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
public class ComputeMetrics {
    private String projName;    /*
     *  This class has the responsibility to compute all metrics for all classes
     *  The metrics here considered are:
     *  - buggyness
     *  - size
     *  - LOC touched
     *  - NAuth
     *  - NFix
     *  - NR
     *  - LOC added
     *  - max LOC added
     *  - average LOC added
     *  - churn
     *  - changeSetSize
     *
     */
    private void setSize(Class c) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(c.getImplementation());
        int lines = 1;
        while (m.find())
        {
            lines ++;
        }
        c.setSize(lines);
    }
    private void setNAuth(Class c) {
        Set<PersonIdent> authors = new HashSet<>();
        for (RevCommit commit : c.getAssociatedCommits()) {
            authors.add(commit.getAuthorIdent());
        }
        c.setNAuth(authors.size());
    }
    private void setNFix(Class c) {
    }
    private void setNR(Class c) {
        c.setNR(c.getAssociatedCommits().size());
    }
    private void setLOCAdded(Class c) throws IOException {
        List<Integer> locAdded = getLOCAdded(c.getAssociatedCommits());
        int sumLOCAdded = 0;
        for (int l : locAdded) {
            sumLOCAdded += l;
        }
        c.setLOCAdded(sumLOCAdded);
    }
    private void setMaxLOCAdded(Class c) throws IOException {
        List<Integer> locAdded = getLOCAdded(c.getAssociatedCommits());
        c.setMaxLOCAdded(getMax(locAdded));
    }
    private void setAverageLOCAdded(Class c) throws IOException {
        List<Integer> locAdded = getLOCAdded(c.getAssociatedCommits());
        c.setAverageLOCAdded(computeAverage(locAdded));
    }
    private void setChurn(Class c) throws IOException {
        int churn = 0;
        List<Integer> locAdded = getLOCAdded(c.getAssociatedCommits());
        List<Integer> locDeleted = getLOCDeleted(c.getAssociatedCommits());

        for (int i = 0; i < locAdded.size(); i++) {
            churn += Math.abs(locAdded.get(i) - locDeleted.get(i));
        }
        c.setChurn(churn);
    }
    private void setMaxAverageChurn(Class c) throws IOException {
        List<Integer> locAdded = getLOCAdded(c.getAssociatedCommits());
        List<Integer> locDeleted = getLOCDeleted(c.getAssociatedCommits());
        List<Integer> churnValues = new ArrayList<>();
        for (int i = 0; i < locAdded.size(); i++) {
            churnValues.add(i, Math.abs(locAdded.get(i) - locDeleted.get(i)));
        }
        c.setMaxChurn(getMax(churnValues));
        c.setAverageChurn(computeAverage(churnValues));
        private void setAge(Class c) {
            c.setAge(c.getRelease().getId());
        }
    }


    private int getMax(List<Integer> array) {
        int max = 0;


        for (int val : array) {
            if (val > max)
                max = val;
        }        return max;
    }
    private float computeAverage(List<Integer> array) {
        if (array.isEmpty())
            return 0;
        int sum = 0;
        for (int val : array) {
            sum += val;
        }
        return (float) sum / array.size();
    }
    private List<Integer> getLOCAdded(List<RevCommit> commits) throws IOException {
        List<Integer> locAdded = new ArrayList<>();
        Repository repository = new FileRepository(projName.toLowerCase() + "/.git/");
        for (RevCommit commit : commits) {
            try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {  // we're not interested in the output
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                diffFormatter.setDetectRenames(true);
                List<DiffEntry> diffEntries = diffFormatter.scan(commit.getParent(0).getTree(), commit.getTree());
                for (DiffEntry entry : diffEntries) {
                    for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                        locAdded.add(edit.getEndB() - edit.getBeginB());
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return locAdded;
    }
    private List<Integer> getLOCDeleted(List<RevCommit> commits) throws IOException {
        List<Integer> locDeleted = new ArrayList<>();
        Repository repository = new FileRepository(projName.toLowerCase() + "/.git/");
        for (RevCommit commit : commits) {
            try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {  // we're not interested in the output
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                diffFormatter.setDetectRenames(true);
                List<DiffEntry> diffEntries = diffFormatter.scan(commit.getParent(0).getTree(), commit.getTree());
                for (DiffEntry entry : diffEntries) {
                    for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                        locDeleted.add(edit.getEndA() - edit.getBeginA());
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return locDeleted;
    }
    public void computeMetrics(List<Class> allClasses, String projName) throws IOException {
        this.projName = projName;
        for (Class c : allClasses) {
            setSize(c);
            setNAuth(c);
            setNR(c);
            setNFix(c);
            setLOCAdded(c);
            setMaxLOCAdded(c);
            setAverageLOCAdded(c);
            setChurn(c);
            setMaxAverageChurn(c);
            setAge(c);

        }
    }
}