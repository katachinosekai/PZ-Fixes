package viewpointoutlinefix;

import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.pool.TypePool;
import static net.bytebuddy.matcher.ElementMatchers.named;

// Offline check, no game start: both patches through ZombieBuddy's own PatchTransformer and Byte Buddy Advice
// against the real IsoObject and Viewpoint Characters classes (applied + JVM-verified), the setter advice on a
// probe, and the latch against a simulated Viewpoint frame loop.
// Needs the game jar, ZombieBuddy.jar, Viewpoint.jar and this mod on the class path.
public class OutlineFixCheck {
    static int fails;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "OK   " : "FAIL ") + what);
        if (!ok) fails++;
    }

    static Advice zbAdvice(Class<?> patch) throws Exception {
        Method m = Class.forName("me.zed_0xff.zombie_buddy.PatchTransformer").getDeclaredMethod("transformPatchClass",
                Class.class, Instrumentation.class, int.class, boolean.class);
        m.setAccessible(true);
        byte[][] got = new byte[1][];
        Instrumentation capture = (Instrumentation) Proxy.newProxyInstance(Instrumentation.class.getClassLoader(),
                new Class<?>[] {Instrumentation.class}, (p, im, args) -> {
                    if (im.getName().equals("redefineClasses")) got[0] = ((ClassDefinition[]) args[0])[0].getDefinitionClassFile();
                    return null;
                });
        m.invoke(null, patch, capture, 0, false);
        if (got[0] == null) throw new IllegalStateException("ZombieBuddy did not transform " + patch.getName());
        ClassFileLocator locator = new ClassFileLocator.Compound(ClassFileLocator.Simple.of(patch.getName(), got[0]),
                ClassFileLocator.ForClassLoader.ofSystemLoader());
        return Advice.to(TypePool.Default.of(locator).describe(patch.getName()).resolve(), locator);
    }

    // Redefines a class from the class path without loading it, then runs the JVM verifier rules on the result.
    static byte[] patched(String cls, String method, Advice advice) {
        ClassFileLocator sys = ClassFileLocator.ForClassLoader.ofSystemLoader();
        TypeDescription td = TypePool.Default.of(sys).describe(cls).resolve();
        byte[] b = new ByteBuddy().redefine(td, sys).visit(advice.on(named(method))).make().getBytes();
        List<VerifyError> errs = ClassFile.of().verify(b);
        check(errs.isEmpty(), cls + "." + method + " patched and verified" + (errs.isEmpty() ? "" : ": " + errs));
        return b;
    }

    static int count(byte[] b, String s) {
        String t = new String(b, StandardCharsets.ISO_8859_1);
        int n = 0;
        for (int i = t.indexOf(s); i >= 0; i = t.indexOf(s, i + 1)) n++;
        return n;
    }

    // Old Viewpoint rule vs the latch, at 144 captures/s with a mod that sets the outline (marked "not during UI",
    // so stale for Viewpoint's clear) at 30 Hz, like A-Life's OnTick under Viewpoint's fixed tick or
    // SafeUserLogin's text bar. Returns captures without outline in the first 2 s.
    static int simulate(boolean latch, long[] offAfterStop, long[] offAfterFalse) {
        Latch.clear();
        Object npc = new Object();
        boolean flag = false;
        long frame = 1_000_000_000L / 144, set = 1_000_000_000L / 30, nextSet = 0;
        int dark = 0;
        long stopAt = 2_000_000_000L, falseAt = 3_000_000_000L;
        offAfterStop[0] = -1;
        offAfterFalse[0] = -1;
        for (long t = 0; t < 4_000_000_000L; t += frame) {
            while (nextSet <= t) {   // setter calls due before this capture
                if (nextSet < stopAt) { flag = true; Latch.set(npc, true, nextSet); }
                nextSet += set;
            }
            if (t >= falseAt - frame && t < falseAt) { flag = true; Latch.set(npc, true, t); }   // one last set just before false
            if (t >= falseAt && t < falseAt + frame) { flag = false; Latch.set(npc, false, t); }  // explicit setOutlineHighlight(false)
            boolean shown = flag;   // the capture reads the flag
            if (t < stopAt && !shown && t > frame) dark++;
            if (t >= stopAt && t < falseAt - frame && !shown && offAfterStop[0] < 0) offAfterStop[0] = t - stopAt;
            if (t >= falseAt && !shown && offAfterFalse[0] < 0) offAfterFalse[0] = t - falseAt;
            if (flag && !(latch && Latch.recent(npc, t))) flag = false;   // Viewpoint's clear after the capture
        }
        return dark;
    }

    public static void main(String[] a) throws Exception {
        check(Fix.ON, "Fix.ON: Viewpoint Characters.class matches the pin and the game members exist");

        Advice set = zbAdvice(Patch_SetOutline.class);
        byte[] iso = patched("zombie.iso.IsoObject", "setOutlineHighlight", set);
        int calls = 0;
        for (MethodModel mm : ClassFile.of().parse(iso).methods()) {
            if (!mm.methodName().equalsString("setOutlineHighlight") || mm.code().isEmpty()) continue;
            for (var e : mm.code().get())
                if (e instanceof InvokeInstruction inv && inv.owner().asInternalName().equals("viewpointoutlinefix/Latch")
                        && inv.name().equalsString("onSet")) calls++;
        }
        check(calls == 2, "IsoObject: both setOutlineHighlight overloads call Latch.onSet (" + calls + " calls)");
        Advice shown = zbAdvice(Patch_OutlinesShown.class);
        byte[] chars = patched("viewpoint.models.Characters", "outlinesShown", shown);
        check(count(chars, "viewpointoutlinefix/Shown") > 0, "Characters.outlinesShown calls Shown.run first");

        // Setter advice behaviour on a probe with both overload shapes.
        Class<?> probe = new ByteBuddy().subclass(Object.class).name("viewpointoutlinefix.SetterProbe")
                .defineMethod("setOutlineHighlight", void.class, Visibility.PUBLIC).withParameters(boolean.class).intercept(StubMethod.INSTANCE)
                .defineMethod("setOutlineHighlight", void.class, Visibility.PUBLIC).withParameters(int.class, boolean.class).intercept(StubMethod.INSTANCE)
                .visit(set.on(named("setOutlineHighlight")))
                .make().load(OutlineFixCheck.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
        Object p = probe.getConstructor().newInstance();
        Method one = probe.getMethod("setOutlineHighlight", boolean.class);
        Method two = probe.getMethod("setOutlineHighlight", int.class, boolean.class);
        Latch.clear();
        one.invoke(p, true);
        check(Latch.recent(p, System.nanoTime()), "setOutlineHighlight(true) latches");
        one.invoke(p, false);
        check(!Latch.recent(p, System.nanoTime()) && Latch.size() == 0, "setOutlineHighlight(false) drops the latch at once");
        two.invoke(p, 0, true);
        check(Latch.recent(p, System.nanoTime()), "setOutlineHighlight(0, true) latches");
        two.invoke(p, 0, false);
        check(!Latch.recent(p, System.nanoTime()), "setOutlineHighlight(0, false) drops the latch at once");

        // Window and pruning.
        Latch.clear();
        Object o = new Object();
        Latch.set(o, true, 0);
        check(Latch.recent(o, Latch.WINDOW_NANOS) && !Latch.recent(o, Latch.WINDOW_NANOS + 1), "latch lasts exactly WINDOW_MS");
        Latch.prune(Latch.WINDOW_NANOS + 1);
        check(Latch.size() == 0, "prune drops expired entries");
        for (int i = 0; i < 1000; i++) Latch.set(new Object(), true, 0);
        for (int i = 0; i < 300; i++) Latch.set(o, true, 10 * Latch.WINDOW_NANOS);
        check(Latch.size() == 1, "expired entries are pruned by the setters too, no 3D capture needed (" + Latch.size() + " left of 1001)");

        // Frame loop.
        long[] stop = new long[1], off = new long[1];
        int oldDark = simulate(false, stop, off);
        check(oldDark > 100, "old rule: outline missing in " + oldDark + " of 288 captures (the flicker)");
        int newDark = simulate(true, stop, off);
        check(newDark == 0, "latch: outline in every capture (" + newDark + " missing)");
        check(stop[0] >= 0 && stop[0] <= Latch.WINDOW_NANOS + 1_000_000_000L / 144 + 1_000_000_000L / 30,
                "latch: outline gone " + stop[0] / 1_000_000 + " ms after the mod stops setting it (bounded by the window)");
        check(off[0] >= 0 && off[0] < 1_000_000_000L / 144, "latch: explicit setOutlineHighlight(false) hides it at the next capture");

        // Stats line (in-game diagnosis): one line per period with the counts, capped per session.
        Stats.reset();
        Latch.clear();
        java.io.PrintStream out = System.out;
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            Object s1 = new Object();
            Latch.set(s1, true, 1);
            Latch.set(s1, false, 2);          // false right after a true: what the latch can't keep
            Latch.set(s1, true, 3);
            Stats.onCapture(2, 1, 1, 4);
            Latch.set(s1, true, Stats.PERIOD_NANOS + 5);   // period over -> line
            for (int i = 2; i < Stats.MAX_LINES + 10; i++) Latch.set(s1, true, i * Stats.PERIOD_NANOS + 5);
        } finally {
            System.setOut(out);
        }
        String log = buf.toString(StandardCharsets.UTF_8);
        String first = log.lines().findFirst().orElse("");
        check(first.contains("3D captures 1") && first.contains("true 3 / false 1") && first.contains("of a true: 1, first from")
                && first.contains("kept by the latch 2, cleared 1, off before capture 1"), "stats line: " + first);
        check(log.lines().count() == Stats.MAX_LINES && log.contains("(last stats line)"),
                "stats lines capped at " + Stats.MAX_LINES + " (" + log.lines().count() + ")");
        Stats.reset();

        check(Modifier.isPublic(Latch.class.getModifiers()) && Modifier.isPublic(Shown.class.getModifiers())
                && Modifier.isPublic(Fix.class.getField("ON").getModifiers()), "advice targets are public");

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
