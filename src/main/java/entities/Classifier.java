package entities;
import weka.Weka;
public class Classifier {
    public enum Type {
        RANDOM_FOREST,
        IBK,
        NAIVE_BAYES
    }
    private Type type;
    private Weka.SearchMethods searchMethod;

    public Classifier(Type type, Weka.SearchMethods searchMethod) {
        this.type = type;
        this.searchMethod = searchMethod;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Weka.SearchMethods getSearchMethod() {
        return searchMethod;
    }

    public void setSearchMethod(Weka.SearchMethods searchMethod) {
        this.searchMethod = searchMethod;
    }
}