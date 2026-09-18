package dev.rdh.pulse.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Mappings {
    private static final String RESOURCE = "/assets/pulse/mappings.txt";

    private Mappings() {}

    private static final class Holder {
        static final Map<String, String> MAP = load();

        private static Map<String, String> load() {
            Map<String, String> map = new HashMap<>(16384);
            try (InputStream in = Mappings.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    return Map.of();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = reader.readLine()) != null; ) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        map.put(line.substring(0, tab), line.substring(tab + 1));
                    }
                }
            } catch (IOException e) {
                return Map.of();
            }
            return map;
        }
    }

    public static boolean isEmpty() {
        return Holder.MAP.isEmpty();
    }

    public static String className(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String mapped = Holder.MAP.get(name.replace('.', '/'));
        return mapped == null ? name : mapped.replace('/', '.');
    }

    public static String methodName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Holder.MAP.getOrDefault(name, name);
    }

    public static String descriptor(String descriptor) {
        if (descriptor == null || descriptor.indexOf('L') < 0) {
            return descriptor;
        }
        StringBuilder out = new StringBuilder(descriptor.length());
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c != 'L') {
                out.append(c);
                i++;
                continue;
            }
            int end = descriptor.indexOf(';', i);
            if (end < 0) {
                out.append(descriptor, i, descriptor.length());
                break;
            }
            String internal = descriptor.substring(i + 1, end);
            out.append('L').append(Holder.MAP.getOrDefault(internal, internal)).append(';');
            i = end + 1;
        }
        return out.toString();
    }
}
