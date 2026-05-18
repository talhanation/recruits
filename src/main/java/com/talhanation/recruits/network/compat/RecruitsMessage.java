package com.talhanation.recruits.network.compat;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public interface RecruitsMessage<T extends RecruitsMessage<T>> extends Message<T> {
    ConcurrentHashMap<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    Method ABSENT_METHOD = AbsentMethodHolder.SENTINEL;

    record MethodKey(Class<?> owner, String name, Class<?> paramType) {
    }

    final class AbsentMethodHolder {
        private static final Method SENTINEL;

        static {
            try {
                SENTINEL = AbsentMethodHolder.class.getDeclaredMethod("sentinel");
            } catch (NoSuchMethodException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private AbsentMethodHolder() {
        }

        @SuppressWarnings("unused")
        private static void sentinel() {
        }
    }

    default T fromBytes(FriendlyByteBuf buf) {
        @SuppressWarnings("unchecked")
        T self = (T) this;
        return self;
    }

    default void toBytes(FriendlyByteBuf buf) {
    }

    default void executeServerSide(RecruitsNetworkContext context) {
    }

    default void executeClientSide(RecruitsNetworkContext context) {
    }

    @Override
    default CustomPacketPayload.Type<T> type() {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                Main.MOD_ID,
                getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT)
        ));
    }

    @SuppressWarnings("unchecked")
    @Override
    default T fromBytes(RegistryFriendlyByteBuf buf) {
        invokeLegacy("fromBytes", FriendlyByteBuf.class, buf);
        return (T) this;
    }

    @Override
    default void toBytes(RegistryFriendlyByteBuf buf) {
        invokeLegacy("toBytes", FriendlyByteBuf.class, buf);
    }

    @Override
    default void executeServerSide(IPayloadContext context) {
        invokeLegacy("executeServerSide", RecruitsNetworkContext.class, new RecruitsNetworkContext(context));
    }

    @Override
    default void executeClientSide(IPayloadContext context) {
        invokeLegacy("executeClientSide", RecruitsNetworkContext.class, new RecruitsNetworkContext(context));
    }

    default void invokeLegacy(String methodName, Class<?> parameterType, Object argument) {
        Method method = METHOD_CACHE.computeIfAbsent(
                new MethodKey(getClass(), methodName, parameterType),
                RecruitsMessage::resolveMethod);
        if (method == ABSENT_METHOD) {
            return;
        }
        try {
            method.invoke(this, argument);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access packet method " + methodName + " on " + getClass().getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Packet method " + methodName + " failed on " + getClass().getName(), cause);
        }
    }

    private static Method resolveMethod(MethodKey key) {
        try {
            return key.owner().getMethod(key.name(), key.paramType());
        } catch (NoSuchMethodException e) {
            return ABSENT_METHOD;
        }
    }
}
