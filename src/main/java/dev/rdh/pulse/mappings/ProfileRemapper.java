package dev.rdh.pulse.mappings;

import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.StackTraceNode;
import me.lucko.spark.proto.SparkSamplerProtos.ThreadNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ProfileRemapper {
    private ProfileRemapper() {}

    public static void remap(SamplerData.Builder data) {
        if (Mappings.isEmpty()) {
            return;
        }

        for (int i = 0; i < data.getThreadsCount(); i++) {
            ThreadNode.Builder thread = data.getThreads(i).toBuilder();
            for (int j = 0; j < thread.getChildrenCount(); j++) {
                StackTraceNode node = thread.getChildren(j);
                StackTraceNode.Builder remapped = node.toBuilder()
                        .setClassName(Mappings.className(node.getClassName()))
                        .setMethodName(Mappings.methodName(node.getMethodName()));
                if (!node.getMethodDesc().isEmpty()) {
                    remapped.setMethodDesc(Mappings.descriptor(node.getMethodDesc()));
                }
                thread.setChildren(j, remapped);
            }
            data.setThreads(i, thread);
        }

        remapKeys(data.getClassSourcesMap(), data::clearClassSources, data::putAllClassSources);
        remapKeys(data.getMethodSourcesMap(), data::clearMethodSources, data::putAllMethodSources);
        remapKeys(data.getLineSourcesMap(), data::clearLineSources, data::putAllLineSources);
    }

    private static void remapKeys(Map<String, String> sources, Runnable clear, Consumer<Map<String, String>> put) {
        if (sources.isEmpty()) {
            return;
        }
        Map<String, String> remapped = new LinkedHashMap<>(sources.size());
        sources.forEach((key, value) -> remapped.put(sourceKey(key), value));
        clear.run();
        put.accept(remapped);
    }

    static String sourceKey(String key) {
        String[] parts = key.split(";", 3);
        parts[0] = Mappings.className(parts[0]);
        if (parts.length > 1) {
            parts[1] = Mappings.methodName(parts[1]);
        }
        if (parts.length > 2 && parts[2].startsWith("(")) {
            parts[2] = Mappings.descriptor(parts[2]);
        }
        return String.join(";", parts);
    }
}
