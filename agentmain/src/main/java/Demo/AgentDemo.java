package Demo;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentDemo implements ClassFileTransformer {
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new AgentDemo(), true);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println("加载类："+className);
        return classfileBuffer;
    }
}