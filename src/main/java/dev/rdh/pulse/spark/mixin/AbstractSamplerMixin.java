package dev.rdh.pulse.spark.mixin;

import dev.rdh.pulse.mappings.ProfileRemapper;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(value = AbstractSampler.class, remap = false)
abstract class AbstractSamplerMixin {
    @Inject(method = "writeDataToProto", at = @At("RETURN"), remap = false)
    private void pulse$remapToFeather(SamplerData.Builder proto, DataAggregator dataAggregator,
                                      Function<?, ?> nodeExporterFunction, ClassSourceLookup classSourceLookup,
                                      Supplier<?> classFinderSupplier, CallbackInfo ci) {
        ProfileRemapper.remap(proto);
    }
}
