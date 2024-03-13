//package Demo;
//
//import com.sun.tools.attach.VirtualMachine;
//import com.sun.tools.attach.VirtualMachineDescriptor;
//import java.util.List;
//
//public class MainDemo {
//    public static void main(String[] args) throws Exception{
//        String path = "agentmain/src/main/java/Demo/jars/agentmain.jar";
//        List<VirtualMachineDescriptor> list = VirtualMachine.list();
//        for (VirtualMachineDescriptor v:list){
////            System.out.println(v.displayName());
//            if (v.displayName().contains("MainDemo")){
//                System.out.println("已找到目标类，将 jvm 虚拟机的 pid 号传入 attach 来进行远程连接，并将 agent.jar 发送给虚拟机");
//                VirtualMachine vm = VirtualMachine.attach(v.id());
//                // 获取连接后将 agent.jar 发送给虚拟机
//                vm.loadAgent(path);
//                // 移除连接
//                vm.detach();
//            }
//        }
//    }
//}