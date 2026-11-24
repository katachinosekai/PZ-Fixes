package viewpointoutlinefix;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AdviceAccessTest {
    public static void main(String[] a) throws Exception {
        int bad = 0, checked = 0, patches = 0;
        try (Stream<Path> s = Files.list(Path.of(a[0]))) {
            for (Path p : s.filter(x -> { String n = x.getFileName().toString(); return n.endsWith(".class") && n.startsWith("Patch_") && !n.contains("$"); }).sorted().toList()) {
                patches++;
                ClassModel cm = ClassFile.of().parse(p);
                for (MethodModel m : cm.methods()) {
                    if (m.code().isEmpty()) continue;
                    for (var e : m.code().get()) {
                        String owner, name;
                        boolean field;
                        if (e instanceof InvokeInstruction inv) { owner = inv.owner().asInternalName(); name = inv.name().stringValue(); field = false; }
                        else if (e instanceof FieldInstruction fi) { owner = fi.owner().asInternalName(); name = fi.name().stringValue(); field = true; }
                        else continue;
                        owner = owner.replace('/', '.');
                        if (!owner.startsWith("viewpointoutlinefix.")) continue;
                        checked++;
                        Class<?> c = Class.forName(owner);
                        boolean ok = Modifier.isPublic(c.getModifiers()), found = false;
                        if (field) {
                            for (Field f : c.getDeclaredFields()) if (f.getName().equals(name)) { found = true; ok &= Modifier.isPublic(f.getModifiers()); }
                        } else {
                            for (Method t : c.getDeclaredMethods()) if (t.getName().equals(name)) { found = true; ok &= Modifier.isPublic(t.getModifiers()); }
                        }
                        if (!ok || !found) {
                            bad++;
                            System.out.println("FAIL " + p.getFileName() + " uses non-public " + owner + "." + name);
                        }
                    }
                }
            }
        }
        System.out.println((bad == 0 && patches > 0 && checked > 0 ? "OK   " : "FAIL ") + "advice in " + patches + " patch classes only uses public members of the mod (" + checked + " uses checked)");
        System.exit(bad == 0 && patches > 0 && checked > 0 ? 0 : 1);
    }
}
