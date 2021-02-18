package intelligence;

public class NoAIException extends Exception {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Learning method not available";
    }

}
