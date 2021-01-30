package malte0811.ferritecore.mixin.nopropertymap;

import com.google.common.collect.ImmutableMap;
import malte0811.ferritecore.ducks.FastMapStateHolder;
import malte0811.ferritecore.ducks.NoPropertyStateHolder;
import malte0811.ferritecore.fastmap.FastMap;
import net.minecraft.state.Property;
import net.minecraft.state.StateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(StateHolder.class)
public abstract class NoPropertyStateHolderMixin implements NoPropertyStateHolder {
    @Shadow
    public abstract ImmutableMap<Property<?>, Comparable<?>> getValues();

    // All other Mixins: If the new data structures are initialized, use those. Otherwise (if populateNeighbors didn't
    // run yet) use the vanilla code using `properties`
    @Redirect(
            method = {"get", "func_235903_d_", "with"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableMap;get(Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false
            )
    )
    @Coerce
    public Object getValueOfProperty(ImmutableMap<?, ?> vanillaMap, Object key) {
        final FastMap<?> globalTable = ((FastMapStateHolder<?>) this).getStateMap();
        final int globalTableIndex = ((FastMapStateHolder<?>) this).getStateIndex();
        if (globalTable != null) {
            return globalTable.getValue(globalTableIndex, (Property<?>) key);
        } else {
            return vanillaMap.get(key);
        }
    }

    @Inject(method = "getValues", at = @At("HEAD"), cancellable = true)
    public void getValuesHead(CallbackInfoReturnable<ImmutableMap<Property<?>, Comparable<?>>> cir) {
        final FastMap<?> globalTable = ((FastMapStateHolder<?>) this).getStateMap();
        if (globalTable != null) {
            cir.setReturnValue(globalTable.makeValuesFor(((FastMapStateHolder<?>) this).getStateIndex()));
            cir.cancel();
        }
    }

    @Inject(method = "hasProperty", at = @At("HEAD"), cancellable = true)
    public <T extends Comparable<T>>
    void hasPropertyHead(Property<T> property, CallbackInfoReturnable<Boolean> cir) {
        final FastMap<?> globalTable = ((FastMapStateHolder<?>) this).getStateMap();
        if (globalTable != null) {
            cir.setReturnValue(globalTable.getProperties().contains(property));
            cir.cancel();
        }
    }

    @Inject(method = "getProperties", at = @At("HEAD"), cancellable = true)
    public void getPropertiesHead(CallbackInfoReturnable<Collection<Property<?>>> cir) {
        final FastMap<?> globalTable = ((FastMapStateHolder<?>) this).getStateMap();
        if (globalTable != null) {
            cir.setReturnValue(globalTable.getProperties());
            cir.cancel();
        }
    }
}
