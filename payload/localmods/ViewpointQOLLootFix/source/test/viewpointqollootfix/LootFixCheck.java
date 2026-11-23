package viewpointqollootfix;

import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import static net.bytebuddy.matcher.ElementMatchers.named;

// Offline check, no game: runs both patch classes through ZombieBuddy's own PatchTransformer and Byte Buddy Advice
// against Viewpoint's real LootTargets, verifies the result, and runs the advice on a probe method.
// Needs the game jar, ZombieBuddy.jar, Viewpoint.jar, ProjectViewpointQOL.jar and this mod on the class path.
public class LootFixCheck {
    static int fails;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "OK   " : "FAIL ") + what);
        if (!ok) fails++;
    }

    // ZombieBuddy's PatchTransformer (ZB annotations -> Byte Buddy Advice annotations), with a capturing
    // Instrumentation like ZBBatch's preTransformPatches: the in-game path redefines the patch class with these bytes.
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
        TypeDescription td = TypePool.Default.of(locator).describe(patch.getName()).resolve();
        return Advice.to(td, locator);
    }

    static byte[] redefine(Class<?> lootTargets, Advice advice) {
        return new ByteBuddy().redefine(lootTargets).visit(advice.on(named("aimed"))).make().getBytes();
    }

    // Loads the patched LootTargets (and its own package) in a throwaway loader and initializes it: runs the verifier.
    static Class<?> loadPatched(byte[] bytes) throws Exception {
        ClassLoader app = LootFixCheck.class.getClassLoader();
        ClassLoader l = new ClassLoader(app) {
            @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
                if (!n.startsWith("viewpoint.interact.")) return super.loadClass(n, r);
                synchronized (getClassLoadingLock(n)) {
                    Class<?> c = findLoadedClass(n);
                    if (c != null) return c;
                    byte[] b;
                    if (n.equals("viewpoint.interact.LootTargets")) b = bytes;
                    else try (InputStream in = app.getResourceAsStream(n.replace('.', '/') + ".class")) {
                        if (in == null) throw new ClassNotFoundException(n);
                        b = in.readAllBytes();
                    } catch (java.io.IOException e) { throw new ClassNotFoundException(n, e); }
                    return defineClass(n, b, 0, b.length);
                }
            }
        };
        return Class.forName("viewpoint.interact.LootTargets", true, l);
    }

    public static void main(String[] a) throws Exception {
        Class<?> lootTargets = Class.forName("viewpoint.interact.LootTargets", false, LootFixCheck.class.getClassLoader());
        Class<?> targetCls = Class.forName("viewpoint.interact.LootTargets$Target", false, LootFixCheck.class.getClassLoader());
        Method aimed = null;
        for (Method m : lootTargets.getDeclaredMethods()) if (m.getName().equals("aimed")) aimed = m;
        check(aimed != null && aimed.getReturnType() == targetCls, "Viewpoint LootTargets.aimed returns LootTargets$Target: " + aimed);

        // 1. QOL's own patch: Byte Buddy must refuse it (the bug).
        Advice qol = zbAdvice(Class.forName("viewpointqol.Patch_LootTargetsContainers"));
        String err = null;
        try { redefine(lootTargets, qol); } catch (Throwable t) { err = String.valueOf(t.getMessage()); }
        check(err != null, "QOL Patch_LootTargetsContainers is refused by Byte Buddy (" + err + ")");

        // 2. This mod's patch: applies, and the patched class passes the verifier.
        check(Fix.ON, "Fix.ON: the QOL patch class on the class path is the known broken one");
        Advice ours = zbAdvice(Patch_LootTargetsAimed.class);
        byte[] patched = redefine(lootTargets, ours);
        check(patched.length > 0, "Patch_LootTargetsAimed applies to LootTargets.aimed (" + patched.length + " bytes)");
        Class<?> loaded = loadPatched(patched);
        check(loaded.getClassLoader() != LootFixCheck.class.getClassLoader(), "patched LootTargets verifies and initializes");
        String text = new String(patched, java.nio.charset.StandardCharsets.ISO_8859_1);
        check(text.contains("viewpointqollootfix/Fix") && text.contains("apply"), "patched bytecode calls viewpointqollootfix.Fix.apply");

        // 3. Advice only uses public members of this mod (it is inlined into Viewpoint's class).
        check(Modifier.isPublic(Fix.class.getModifiers()) && Modifier.isPublic(Fix.class.getField("ON").getModifiers())
                && Modifier.isPublic(Fix.class.getMethod("apply", Object.class).getModifiers()), "Fix, Fix.ON, Fix.apply are public");

        // 4. Behaviour: same advice on a probe with aimed()'s exact signature, in Viewpoint's package and loader.
        Class<?> frame = Class.forName("viewpoint.core.Frame", false, LootFixCheck.class.getClassLoader());
        Class<?> player = Class.forName("zombie.characters.IsoPlayer", false, LootFixCheck.class.getClassLoader());
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(lootTargets, MethodHandles.lookup());
        Class<?> probe = new ByteBuddy().subclass(Object.class).name("viewpoint.interact.AimedProbe")
                .defineMethod("aimed", targetCls, Visibility.PUBLIC, Ownership.STATIC).withParameters(frame, player, targetCls)
                .intercept(FixedValue.argument(2))
                .visit(ours.on(named("aimed")))
                .make().load(lootTargets.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup)).getLoaded();
        Method probeAimed = probe.getMethod("aimed", frame, player, targetCls);
        Constructor<?> ctor = targetCls.getDeclaredConstructor();
        ctor.setAccessible(true);
        Field containersF = targetCls.getDeclaredField("containers");
        containersF.setAccessible(true);

        Class<?> qc = Class.forName("viewpointqol.ViewpointQOLContainers");
        Class<?> qv = Class.forName("viewpointqol.ViewpointQOLVersion");
        check((Boolean) qv.getMethod("isSupported").invoke(null), "QOL sees Viewpoint 0.1.7 (isSupported)");

        Object t1 = ctor.newInstance();
        @SuppressWarnings("unchecked") List<Object> c1 = (List<Object>) containersF.get(t1);
        c1.add("aimed-container");
        qc.getMethod("clear").invoke(null);
        Object r = probeAimed.invoke(null, null, null, t1);
        check(r == t1 && c1.equals(List.of("aimed-container")), "QOL nearby containers off: target unchanged " + c1);

        qc.getMethod("begin").invoke(null);
        qc.getMethod("addContainer", Object.class).invoke(null, "nearby-1");
        qc.getMethod("addContainer", Object.class).invoke(null, "nearby-2");
        qc.getMethod("setActive", boolean.class).invoke(null, true);
        r = probeAimed.invoke(null, null, null, t1);
        check(r == t1 && c1.equals(List.of("nearby-1", "nearby-2")), "QOL nearby containers on: aimed target shows them " + c1);
        r = probeAimed.invoke(null, null, null, t1);
        check(c1.equals(List.of("nearby-1", "nearby-2")), "running twice gives the same list (idempotent) " + c1);
        check(probeAimed.invoke(null, null, null, null) == null, "null target passes through");
        qc.getMethod("clear").invoke(null);

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
