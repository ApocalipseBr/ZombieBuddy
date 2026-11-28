package me.zed_0xff.zombie_buddy.transformers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class JarContextClassLoaderTest {
    @Test
    void resolvesTypesVisibleOnlyThroughSuppliedClassLoader() throws Exception {
        String target = "viewpoint.models.ModelCapture";
        byte[] targetBytes = classBytes(target);
        ClassLoader gameLoader = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                if (!name.equals(target.replace('.', '/') + ".class")) return null;
                try {
                    return new URL(null, "memory:/" + name, new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) {
                            return new URLConnection(url) {
                                @Override public void connect() {}
                                @Override public java.io.InputStream getInputStream() {
                                    return new ByteArrayInputStream(targetBytes);
                                }
                            };
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        try (JarContext context = JarContext.forClass("test.Patch", classBytes("test.Patch"), gameLoader)) {
            assertThat(context.getOrigTypeDesc(target)).isNotNull();
            assertThat(context.getOrigTypeDesc(target).getName()).isEqualTo(target);
        }
    }

    private static byte[] classBytes(String binaryName) {
        String internalName = binaryName.replace('.', '/');
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
