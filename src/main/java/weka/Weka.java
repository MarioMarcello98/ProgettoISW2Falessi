package weka;
import exception.EmptyARFFException;
import exception.ExecutionException;
import entities.Classifier;
import entities.EvaluationReport;
import entities.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ARFF;
import utils.CSV;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Weka {
    private static Logger logger = LoggerFactory.getLogger(Weka.class);
    public enum SearchMethods {
        BACKWARD_SEARCH,
        FORWARD_SEARCH,
        BIDIRECTIONAL_SEARCH
    }
    public enum SamplingMethod {
        UNDERSAMPLING,
        OVERSAMPLING
    }
    private final List<EvaluationReport> reportsWithoutFS = new ArrayList<>();
    private final List<EvaluationReport> reportsWithFS = new ArrayList<>();
    private final List<EvaluationReport> reportsWithSampling = new ArrayList<>();
    private final List<EvaluationReport> reportsWithCSC = new ArrayList<>();

    public void classify(String arffTrainingSet, String arffTestingSet, int iteration, String projName) throws IOException, EmptyARFFException, ExecutionException {
        File trainFile = new File(arffTrainingSet);
        FileReader fileReader = new FileReader(trainFile);
        ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        if (arffReader.getStructure().attribute(arffReader.getStructure().numAttributes() - 1).numValues() == 1)
            throw new EmptyARFFException();
        File testFile = new File(arffTestingSet);
        fileReader = new FileReader(testFile);
        arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        try {
            logger.info("Iteration {}", iteration);

            // NO FILTER
            DataSource trainSource = new DataSource(arffTrainingSet);
            DataSource testSource = new DataSource(arffTestingSet);
            Instances trainData = trainSource.getDataSet();
            Instances testData = testSource.getDataSet();
            trainData.setClassIndex(trainData.numAttributes() - 1);
            testData.setClassIndex(trainData.numAttributes() - 1);
            Evaluation eval = new Evaluation(trainData);
            NaiveBayes nb = new NaiveBayes();
            nb.buildClassifier(trainData);
            eval.evaluateModel(nb, testData);
            Metrics metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            EvaluationReport reportWithoutFS = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, false, null, null, false);
            reportWithoutFS.setMetrics(metrics);
            reportsWithoutFS.add(reportWithoutFS);
            RandomForest rf = new RandomForest();
            rf.buildClassifier(trainData);
            eval.evaluateModel(rf, testData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithoutFS = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, false, null, null, false);
            reportWithoutFS.setMetrics(metrics);
            reportsWithoutFS.add(reportWithoutFS);
            IBk iBk = new IBk();
            iBk.buildClassifier(trainData);
            eval.evaluateModel(iBk, testData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithoutFS = new EvaluationReport(iteration, Classifier.Type.IBK, projName, false, null, null, false);
            reportWithoutFS.setMetrics(metrics);
            reportsWithoutFS.add(reportWithoutFS);
            // FEATURE SELECTION

            weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();
            CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();

            // Backward Search
            BestFirst bf = new BestFirst();     // -D <0 = backward | 1 = forward | 2 = bi-directional>
            bf.setOptions(new String[] {"-D", "0"});
            filter.setEvaluator(cfsSubsetEval);
            filter.setSearch(bf);
            filter.setInputFormat(trainData);
            Instances filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
            Instances filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);
            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            EvaluationReport reportWithFS = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, true, SearchMethods.BACKWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, true, SearchMethods.BACKWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.IBK, projName, true, SearchMethods.BACKWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);

            // Forward Search
            bf.setOptions(new String[] {"-D", "1"});
            filter.setSearch(bf);
            filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
            filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);
            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, true, SearchMethods.FORWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, true, SearchMethods.FORWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.IBK, projName, true, SearchMethods.FORWARD_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);

            // Bidirectional
            bf.setOptions(new String[] {"-D", "2"});
            filter.setSearch(bf);
            filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
            filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);
            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithFS = new EvaluationReport(iteration, Classifier.Type.IBK, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false);
            reportWithFS.setMetrics(metrics);
            reportsWithFS.add(reportWithFS);

            // SAMPLING

            // Undersampling
            Filter samplingFilter = new SpreadSubsample();
            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setFilter(samplingFilter);
            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            filteredClassifier.setClassifier(nb);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            EvaluationReport reportWithSampling = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);
            rf = new RandomForest();
            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithSampling = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);
            iBk = new IBk();
            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithSampling = new EvaluationReport(iteration, Classifier.Type.IBK, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);

            // Oversampling
            samplingFilter = new Resample();
            ARFF arff = new ARFF();
            int countTrue = arff.countNumOccurrences(trainFile, "true");
            int countFalse = arff.countNumOccurrences(trainFile, "false");
            double percentage;
            if (countTrue < countFalse)
                percentage = (double) (countFalse - countTrue) / Math.max(countTrue, 1);
            else
                percentage = (double) (countTrue - countFalse) / Math.max(countFalse, 1);
            String[] resampleOptions = new String[4];
            resampleOptions[0] = "-B";
            resampleOptions[1] = "1.0";
            resampleOptions[2] = "-Z";
            resampleOptions[3] = String.valueOf(percentage);
            samplingFilter.setOptions(resampleOptions);
            filteredClassifier.setFilter(samplingFilter);
            eval = new Evaluation(filteredTrainingData);
            filteredClassifier.setClassifier(nb);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithSampling = new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);
            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithSampling = new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);
            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa());
            reportWithSampling = new EvaluationReport(iteration, Classifier.Type.IBK, projName, true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false);
            reportWithSampling.setMetrics(metrics);
            reportsWithSampling.add(reportWithSampling);

            // COST SENSITIVE CLASSIFIER

            CostMatrix costMatrix = new CostMatrix(2);
            costMatrix.setCell(0, 0, 0.0);
            costMatrix.setCell(0, 1, 10.0);
            costMatrix.setCell(1, 0, 1.0);
            costMatrix.setCell(1, 1, 0.0);
            CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(nb);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(costSensitiveClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa());
            EvaluationReport reportWithCSC = new EvaluationReport(
                    iteration,
                    Classifier.Type.NAIVE_BAYES,
                    projName,
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            );
            reportWithCSC.setMetrics(metrics);
            reportsWithCSC.add(reportWithCSC);
            rf = new RandomForest();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(rf);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(costSensitiveClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa());
            reportWithCSC = new EvaluationReport(
                    iteration,
                    Classifier.Type.RANDOM_FOREST,
                    projName,
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            );
            reportWithCSC.setMetrics(metrics);
            reportsWithCSC.add(reportWithCSC);
            iBk = new IBk();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(iBk);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(costSensitiveClassifier, filteredTestingData);
            metrics = new Metrics(eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa());
            reportWithCSC = new EvaluationReport(
                    iteration,
                    Classifier.Type.IBK,
                    projName,
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            );
            reportWithCSC.setMetrics(metrics);
            reportsWithCSC.add(reportWithCSC);
        } catch (Exception e) {
            throw new ExecutionException(e);
        } finally {
            fileReader.close();
        }
    }

    public void generateFiles() throws ExecutionException {

        CSV.generateCSVForReportsWithoutFS(reportsWithoutFS);
        CSV.generateCSVForReportsWithFS(reportsWithFS);
        CSV.generateCSVForReportsWithSampling(reportsWithSampling);
        CSV.generateCSVForReportsWithCSC(reportsWithCSC);
    }
}