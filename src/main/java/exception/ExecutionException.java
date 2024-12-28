package exception;


public class ExecutionException extends Exception{
    public ExecutionException() {super();}
    public ExecutionException(Exception e) {super(e);}
    public ExecutionException(String e) {super(e);}
}