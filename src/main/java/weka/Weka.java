package weka;

import exception.EmptyARFFException;
import entities.Classifier;
import entities.EvaluationReport;
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

    public void classify(String ARFFTrainingSet, String ARFFTestingSet, int iteration, String projName) throws IOException, EmptyARFFException {
        File trainFile = new File(ARFFTrainingSet);
        FileReader fileReader = new FileReader(trainFile);
        ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        if (arffReader.getStructure().attribute(arffReader.getStructure().numAttributes() - 1).numValues() == 1)
            throw new EmptyARFFException();
        File testFile = new File(ARFFTestingSet);
        fileReader = new FileReader(testFile);
        arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();
        try {
            System.out.println("Iteration " + iteration);
            DataSource trainSource = new DataSource(ARFFTrainingSet);
            DataSource testSource = new DataSource(ARFFTestingSet);
            Instances trainData = trainSource.getDataSet();
            Instances testData = testSource.getDataSet();
            trainData.setClassIndex(trainData.numAttributes() - 1);
            testData.setClassIndex(trainData.numAttributes() - 1);
            Evaluation eval = new Evaluation(trainData);
            NaiveBayes nb = new NaiveBayes();
            nb.buildClassifier(trainData);
            eval.evaluateModel(nb, testData);
            System.out.println(eval.toMatrixString("Confusion matrix (NB)"));
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null, false));
            RandomForest rf = new RandomForest();
            rf.buildClassifier(trainData);
            eval.evaluateModel(rf, testData);
            System.out.println(eval.toMatrixString("Confusion matrix (RF)"));
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null, false));
            IBk iBk = new IBk();
            iBk.buildClassifier(trainData);
            eval.evaluateModel(iBk, testData);
            System.out.println(eval.toMatrixString("Confusion matrix (IBk)"));
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null, false));

            weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();
            CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();

            /* Backward Search */
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
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null, false));
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null));
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null, false));


            /* Forward Search */
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
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null, false));
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null, false));
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null, false));


            /* Best First */
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
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false));
            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false));
            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, null, false));



            /* Undersampling */
            Filter samplingFilter = new SpreadSubsample();
            String[] spreadSubSampleOptions = new String[2];
            spreadSubSampleOptions[0] = "-M";
            spreadSubSampleOptions[1] = "1.0";

            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setFilter(samplingFilter);

            eval = new Evaluation(filteredTrainingData);
            nb = new NaiveBayes();
            filteredClassifier.setClassifier(nb);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false));

            rf = new RandomForest();
            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false));

            iBk = new IBk();
            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.UNDERSAMPLING, false));

            /* Oversampling */
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
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false));

            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false));

            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(filteredClassifier, filteredTestingData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BIDIRECTIONAL_SEARCH, SamplingMethod.OVERSAMPLING, false));


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
            System.out.println(eval.toMatrixString("Confusion matrix (Naive Bayes)"));
            reportsWithCSC.add(new EvaluationReport(
                    iteration,
                    Classifier.Type.NAIVE_BAYES,
                    projName,
                    eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa(),
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            ));
            rf = new RandomForest();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(rf);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(costSensitiveClassifier, filteredTestingData);
            System.out.println(eval.toMatrixString("Confusion matrix (Random Forest)"));
            reportsWithCSC.add(new EvaluationReport(
                    iteration,
                    Classifier.Type.RANDOM_FOREST,
                    projName,
                    eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa(),
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            ));
            iBk = new IBk();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(iBk);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);
            eval.evaluateModel(costSensitiveClassifier, filteredTestingData);
            System.out.println(eval.toMatrixString("Confusion matrix (IBk)"));
            reportsWithCSC.add(new EvaluationReport(
                    iteration,
                    Classifier.Type.IBK,
                    projName,
                    eval.precision(0),
                    eval.recall(0),
                    eval.areaUnderROC(0),
                    eval.kappa(),
                    true,
                    SearchMethods.BIDIRECTIONAL_SEARCH,
                    null,
                    true
            ));
            System.out.println("\n");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileReader.close();
        }
    }

    public void generateFiles() {

        CSV.generateCSVForReportsWithoutFS(reportsWithoutFS);
        CSV.generateCSVForReportsWithFS(reportsWithFS);
        CSV.generateCSVForReportsWithSampling(reportsWithSampling);
        CSV.generateCSVForReportsWithCSC(reportsWithCSC);
    }


}