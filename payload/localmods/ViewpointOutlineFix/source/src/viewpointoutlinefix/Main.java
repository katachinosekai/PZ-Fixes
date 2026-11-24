package viewpointoutlinefix;

public final class Main {
    public static final String VERSION = "1.0.1";

    private Main() {}

    // Always logs that ZombieBuddy loaded the jar, then runs Fix's check (its one "active"/"off" line).
    public static void main(String[] args) {
        System.out.println("[ViewpointOutlineFix] " + VERSION + " loaded by ZombieBuddy");
        if (!Fix.ON) return;
    }
}
