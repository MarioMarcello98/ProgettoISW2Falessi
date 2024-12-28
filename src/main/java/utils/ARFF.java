package utils;
package utils;
import model.Class;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
public class ARFF {
    public static void generateARFF(String filename, List<Class> classes) {
        File file = new File(filename);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            String name = filename.substring(0, filename.length() - 4);
            fileWriter.append(
                    "@relation " + filename + "\n" +
                            "\n" +
                            "@attribute ' LOC' numeric\n" +
                            "@attribute ' NAuth' numeric\n" +
                            "@attribute ' Fan-Out' numeric\n" +
                            "@attribute ' Revisions' numeric\n" +
                            "@attribute ' LOCAdded' numeric\n" +
                            "@attribute ' MaxLOCAdded' numeric\n" +
                            "@attribute ' averageLOCAdded' numeric\n" +
                            "@attribute ' Churn' numeric\n" +
                            "@attribute ' MaxChurn' numeric\n" +
                            "@attribute ' AverageChurn' numeric\n" +
                            "@attribute ' Time Span (days)' numeric\n" +
                            "@attribute Buggy {'true','false'}\n\n" +
                            "@data\n"
            );
            for (Class c : classes) {
                fileWriter.append(
                        c.getSize() + "," +
                                c.getNAuth() + "," +
                                c.getFanOut() + "," +
                                c.getNR() + "," +
                                c.getLOCAdded() + "," +
                                c.getMaxLOCAdded() + "," +
                                c.getAverageLOCAdded() + "," +
                                c.getChurn() + "," +
                                c.getMaxChurn() + "," +
                                c.getAverageChurn() + "," +
                                c.getTimeSpan() + "," +
                                c.isBuggy()
                );
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}