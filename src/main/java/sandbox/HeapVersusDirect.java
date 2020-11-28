package sandbox;

import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;


/**
 * Usage:
 * <p>
 * $ cat > /tmp/sandbox.HeapVersusDirect.java <<EOF
 * > this file content...
 * > ...
 * > EOF
 * <p>
 * $ env -u JDK_JAVA_OPTIONS java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED /tmp/sandbox.HeapVersusDirect.java
 */
public class HeapVersusDirect {
    public static void main(String[] args) throws Exception {
        System.out.printf("max: %d%n", Runtime.getRuntime().maxMemory());

        var classLoader = HeapVersusDirect.class.getClassLoader();
        var internalUnsafeClass = classLoader.loadClass("jdk.internal.misc.Unsafe");
        var method = internalUnsafeClass.getDeclaredMethod("getUnsafe");
        var unsafe = method.invoke(null);
        var allocateUninitializedArray = unsafe.getClass()
                .getDeclaredMethod("allocateUninitializedArray", Class.class, int.class);

        System.out.printf("Java heap buffers");
        for (int i = 0; i < 30; i++) {
            byte[] arena = (byte[]) allocateUninitializedArray.invoke(unsafe, byte.class, 16 * 1024 * 1024);
            arena[0] = 0x01;
            arena[4096] = 0x01;
            arena[4096 * 2] = 0x01;
            arena[4096 * 3] = 0x01;
            arena[4096 * 4] = 0x01;
            System.out.printf("%s%n", Long.toHexString(addressOf(unsafe, arena)));
        }

        var address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);
        System.out.printf("native heap (pmap shows [heap] mapping");
        for (var i = 0; i < 30; i++) {
            var byteBuffer = ByteBuffer.allocateDirect(16 * 1024 * 1024);
            byteBuffer.putInt(0, 0x01);
            byteBuffer.putInt(4096, 0x01);
            byteBuffer.putInt(4096 * 2, 0x01);
            byteBuffer.putInt(4096 * 3, 0x01);
            byteBuffer.putInt(4096 * 4, 0x01);
            System.out.printf("%s%n", Long.toHexString(address.getLong(byteBuffer)));
        }

        new ProcessBuilder("pmap", "-X", Long.toString(ProcessHandle.current().pid()))
                .redirectOutput(Redirect.INHERIT)
                .start();
    }

    // Based on this SO answer https://stackoverflow.com/a/7060500/48136
    public static long addressOf(Object unsafe, Object object) throws Exception {

        var array = new Object[]{object};

        var baseOffset = (int) unsafe.getClass()
                .getDeclaredMethod("arrayBaseOffset", Class.class)
                .invoke(unsafe, Object[].class);
        var addressSize = (int) unsafe.getClass()
                .getDeclaredMethod("addressSize")
                .invoke(unsafe);
        long objectAddress;
        switch (addressSize) {
            case 4:
                objectAddress = (int) unsafe.getClass()
                        .getDeclaredMethod("getInt", Object.class, long.class)
                        .invoke(unsafe, array, baseOffset);
                break;
            case 8:
                objectAddress = (long) unsafe.getClass()
                        .getDeclaredMethod("getLong", Object.class, long.class)
                        .invoke(unsafe, array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }

        return (objectAddress);
    }
}
