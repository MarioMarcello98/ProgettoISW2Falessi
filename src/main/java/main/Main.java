package main;
import exception.ExecutionException;
import retrievers.WalkForward;

public class Main {
    public static void main(String[] args) throws ExecutionException {
        try {
            WalkForward.initSets("BOOKKEEPER");
            WalkForward.classify("BOOKKEEPER");
            WalkForward.initSets("OPENJPA");
            WalkForward.classify("OPENJPA");
        } catch (Exception e) {
            throw new ExecutionException();
        }

    }
}
