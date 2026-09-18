package dev.rdh.pulse.spark;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.service.MixinService;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FabricClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    private static final Pattern LINE = Pattern.compile("(\\d+)(?:#(\\d+))?(?:,(\\d+))?:(\\d+)(?:,(\\d+))?");
    private final Map<Path, String> sources = new HashMap<>();
    private final Map<String, Map<Integer, String>> sourceMaps = new ConcurrentHashMap<>();

    public FabricClassSourceLookup() {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            for (Path root : mod.getRootPaths()) {
                URI uri = root.toUri();
                Path source = uri.getScheme().equals("jar") && root.toString().equals("/")
                        ? Path.of(root.getFileSystem().toString())
                        : root;
                this.sources.put(source.toAbsolutePath().normalize(), mod.getMetadata().getId());
            }
        }
    }

    @Override
    public String identifyFile(Path path) {
        return this.sources.get(path.toAbsolutePath().normalize());
    }

    @Override
    public String identify(MethodCall methodCall) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> target = Class.forName(methodCall.getClassName(), false, loader);
        for (Method method : target.getDeclaredMethods()) {
            if (!method.getName().equals(methodCall.getMethodName()) || !descriptor(method).equals(methodCall.getMethodDescriptor())) {
                continue;
            }
            MixinMerged mixin = method.getDeclaredAnnotation(MixinMerged.class);
            if (mixin != null) {
                return identify(Class.forName(mixin.mixin(), false, loader));
            }
        }
        return null;
    }

    @Override
    public String identify(MethodCallByLine methodCall) throws Exception {
        if (methodCall.getClassName().equals("native") || methodCall.getMethodName().startsWith("<")) {
            return null;
        }
        String mixin = this.sourceMaps.computeIfAbsent(methodCall.getClassName(), FabricClassSourceLookup::sourceMap).get(methodCall.getLineNumber());
        return mixin == null ? null : identify(Class.forName(mixin, false, Thread.currentThread().getContextClassLoader()));
    }

    private static Map<Integer, String> sourceMap(String className) {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(className.replace('.', '/'));
            if (node == null || node.sourceDebug == null) {
                return Map.of();
            }
            return parseSourceMap(node.sourceDebug);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    static Map<Integer, String> parseSourceMap(String sourceDebug) {
        String[] lines = sourceDebug.replace("\r\n", "\n").replace('\r', '\n').split("\n");
            Map<Integer, String> files = new HashMap<>();
            Map<Integer, String> output = new HashMap<>();
            int index = 0;
            while (index < lines.length && !lines[index].equals("*F")) index++;
            for (index++; index < lines.length && !lines[index].startsWith("*"); index++) {
                String file = lines[index];
                String path = null;
                if (file.startsWith("+ ")) {
                    file = file.substring(2);
                    path = lines[++index];
                }
                int separator = file.indexOf(' ');
                if (separator > 0) files.put(Integer.parseInt(file.substring(0, separator)), (path == null ? file.substring(separator + 1) : path).replaceAll("\\.java$", "").replace('/', '.'));
            }
            while (index < lines.length && !lines[index].equals("*L")) index++;
            int previousFile = 0;
            for (index++; index < lines.length && !lines[index].startsWith("*"); index++) {
                Matcher match = LINE.matcher(lines[index]);
                if (!match.matches()) continue;
                int file = match.group(2) == null ? previousFile : Integer.parseInt(match.group(2));
                int count = match.group(3) == null ? 1 : Integer.parseInt(match.group(3));
                int start = Integer.parseInt(match.group(4));
                int increment = match.group(5) == null ? 1 : Integer.parseInt(match.group(5));
                for (int repeat = 0; repeat < count; repeat++) for (int line = 0; line < increment; line++) output.putIfAbsent(start + repeat * increment + line, files.get(file));
                previousFile = file;
            }
        return output;
    }

    private static String descriptor(Method method) {
        StringBuilder descriptor = new StringBuilder("(");
        for (Class<?> parameter : method.getParameterTypes()) {
            descriptor.append(descriptor(parameter));
        }
        return descriptor.append(')').append(descriptor(method.getReturnType())).toString();
    }

    private static String descriptor(Class<?> type) {
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        }
        if (!type.isPrimitive()) {
            return "L" + type.getName().replace('.', '/') + ";";
        }
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        return "D";
    }
}
