package me.zed_0xff.zombie_buddy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.V17;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.transformers.TransformedJar;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;

class PatchEngineMergedTargetTest {
    private static final String TARGET = "viewpoint.fixture.MergedHookTarget";

    @Patch(className = TARGET, methodName = "contextPick")
    public static class PlainAdvice {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) int result) {
            result += 10;
        }
    }

    @Patch(className = TARGET, methodName = "frame")
    public static class ResolvingAdvice {
        @Advice.OnMethodExit
        public static void exit(@Patch.Field({"oldValue", "currentValue"}) int value,
                                @Advice.Return(readOnly = false) int result) {
            result += value;
        }
    }

    @Test
    void appliesBothPlainAdviceAndAdviceRequiringLateResolution() throws Exception {
        var instrumentation = ByteBuddyAgent.install();
        var previous = Loader.g_instrumentation;
        Loader.g_instrumentation = instrumentation;
        try {
            var jar = new TransformedJar(Map.of(TARGET, targetBytes()), Map.of(), List.of(), null, null);
            var loader = ModJarInjector.injectMerged(jar, null, instrumentation);
            var target = loader.loadClass(TARGET);
            assertThat(loader.getResourceAsStream(TARGET.replace('.', '/') + ".class")).isNull();
            var contextPick = target.getMethod("contextPick");
            var frame = target.getMethod("frame");
            assertThat(contextPick.invoke(null)).isEqualTo(1);
            assertThat(frame.invoke(null)).isEqualTo(1);

            PatchEngine.applyPatches(List.of(PlainAdvice.class.getName(), ResolvingAdvice.class.getName()), loader, null);

            assertAll(
                    () -> assertThat(contextPick.invoke(null)).as("plain advice").isEqualTo(11),
                    () -> assertThat(frame.invoke(null)).as("advice with field aliases").isEqualTo(8));
        } finally {
            Loader.g_instrumentation = previous;
        }
    }

    private static byte[] targetBytes() {
        var writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, TARGET.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "currentValue", "I", null, 7).visitEnd();
        for (String name : List.of("contextPick", "frame")) {
            var method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, name, "()I", null, null);
            method.visitCode();
            method.visitInsn(ICONST_1);
            method.visitInsn(IRETURN);
            method.visitMaxs(1, 0);
            method.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }
}
