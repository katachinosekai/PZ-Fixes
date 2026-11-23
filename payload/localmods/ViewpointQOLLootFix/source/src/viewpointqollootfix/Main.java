package viewpointqollootfix;

public final class Main {
    private Main() {}

    // Runs Fix's version check (and its log line) at mod load instead of on the first aimed() call.
    public static void main(String[] args) {
        if (!Fix.ON) return;
    }
}
