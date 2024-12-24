package weka;
import com.sun.xml.bind.v2.runtime.unmarshaller.IntArrayData;
import exceptions.EmptyARFFException;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.PlainText;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
public class Weka {
    // Random Forest, Naive Bayes, IBk
    public void classify(String ARFFTrainingSet, String ARFFTestingSet) throws IOException, EmptyARFFException {
        File file = new File(ARFFTrainingSet);
        FileReader fileReader = new FileReader(file);
        ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        file = new File(ARFFTestingSet);
        fileReader = new FileReader(file);
        arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        try {
            System.out.println(ARFFTrainingSet);
            System.out.println(ARFFTestingSet);
            DataSource trainSource = new DataSource(ARFFTrainingSet);
            DataSource testSource = new DataSource(ARFFTestingSet);
            Instances trainData = trainSource.getDataSet();
            Instances testData = testSource.getDataSet();
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "2";
            Remove remove = new Remove();
            remove.setOptions(options);
            remove.setInputFormat(trainData);
            Instances train = Filter.useFilter(trainData, remove);
            remove.setInputFormat(testData);
            Instances test = Filter.useFilter(testData, remove);
            ArffSaver arffSaver = new ArffSaver();
            arffSaver.setInstances(train);
            arffSaver.setFile(new File(ARFFTrainingSet));
            arffSaver.writeBatch();
            arffSaver.setInstances(test);
            arffSaver.setFile(new File(ARFFTestingSet));
            arffSaver.writeBatch();
            train.setClassIndex(train.numAttributes() - 1);
            test.setClassIndex(train.numAttributes() - 1);
            Evaluation eval = new Evaluation(train);
            NBClassification(train, test, eval);
            IBkClassification(train, test, eval);
            RFClassification(train, test, eval);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileReader.close();
        }
    }
    private void NBClassification(Instances train, Instances test, Evaluation eval) throws Exception {
        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(train);
        // evaluation
        System.out.println("Naive Bayes results: ");
        eval.evaluateModel(naiveBayes, test);
        System.out.println(eval.toSummaryString());
    }
    private void RFClassification(Instances train, Instances test, Evaluation eval) throws Exception {
        RandomForest randomForest = new RandomForest();
        randomForest.buildClassifier(train);
        // evaluation
        System.out.println("Random Forest results: ");
        eval.evaluateModel(randomForest, test);
        System.out.println(eval.toSummaryString());
    }
    private void IBkClassification(Instances train, Instances test, Evaluation eval) throws Exception {
        IBk iBk = new IBk();
        iBk.buildClassifier(train);
        // evaluation
        System.out.println("IBk results: ");
        eval.evaluateModel(iBk, test);
        System.out.println(eval.toSummaryString());
    }
}
