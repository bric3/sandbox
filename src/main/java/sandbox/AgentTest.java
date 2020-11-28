package sandbox;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;

import java.lang.management.ManagementFactory;

public class AgentTest {

    public static void main(String[] args) {
        Foo foo = new Foo();
        System.out.printf("[%.3fs][stdout] args %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, ProcessHandle.current().info().commandLine());
        System.out.printf("[%.3fs][stdout] m(): %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, foo.m());
        ByteBuddyAgent.install();

        new ByteBuddy()
                .redefine(Bar.class)
                .name(Foo.class.getName())
                .make()
                .load(Foo.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
        System.out.printf("[%.3fs][stdout] m(): %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, foo.m());
    }

    private static class Foo {
        String m() { return "full"; }
    }
    private static class Bar {
        String m() { return "bear"; }
    }
}

