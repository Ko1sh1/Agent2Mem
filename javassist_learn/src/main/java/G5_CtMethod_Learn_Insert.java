import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class G5_CtMethod_Learn_Insert {
    static class Test {
        void add(int a, int b) {
            System.out.println(a + b);
        }

        void sub(int a, int b) {
            System.out.println(Math.abs(a - b));
        }

        int fact(int n) {
            if (n <= 1)
                return n;
            else
                return n * fact(n - 1);
        }

        String word(){
            return "1";
        }
    }

    public static void main(String[] args) throws Throwable {
//        test1();
//        test2();
//        test3();
        test4();
    }

    public static void test1() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_Insert$Test");
        CtMethod m = cc.getDeclaredMethod("add");
//        m.insertBefore("{ System.out.println($0.getClass().getName());System.out.println(\"first num:\"+$1);\nSystem.out.println(\"second num:\"+$2);}");
//        m.insertBefore("{ System.out.println(java.util.Arrays.toString($args));\nSystem.out.println($args[0]+$args[1]);}");
//        m.insertBefore("{ sub($$);}");
//        m.insertBefore("{System.out.println(java.util.Arrays.toString($sig));}");
        m.insertBefore("{System.out.println($class);}");
        m.insertBefore("{System.out.println($0);}");
        cc.writeFile("javassist_learn/src/main/java/class_repository");

        Class<?> aClass = cc.toClass();
        Object instance = aClass.newInstance();
        aClass.getDeclaredMethod("add", int.class, int.class).invoke(instance, 10, 20);
    }

    public static void test2() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_Insert$Test");
        CtMethod m = cc.getDeclaredMethod("fact");
        m.useCflow("fact");
        m.insertBefore("{ System.out.println(\"函数递归深度：\"+$cflow(fact));}");
        cc.writeFile("javassist_learn/src/main/java/class_repository");


        Class<?> aClass = cc.toClass();
        Object instance = aClass.newInstance();
        aClass.getDeclaredMethod("fact",int.class).invoke(instance,5);
    }


    public static void test3() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_Insert$Test");
        CtMethod m =  cc.getDeclaredMethod("word");
        m.insertAfter("{Object result = \"hahaha\";$_=($r)result;}");
        cc.writeFile("javassist_learn/src/main/java/class_repository");


        Class<?> aClass = cc.toClass();
        Object instance = aClass.newInstance();
        System.out.println(aClass.getDeclaredMethod("word").invoke(instance));
    }

    public static void test4() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_Insert$Test");
        CtMethod m = cc.getDeclaredMethod("word");
        CtClass etype = ClassPool.getDefault().get("java.io.IOException");
        m.addCatch("{ System.out.println($e); throw $e; }", etype);
        cc.writeFile("javassist_learn/src/main/java/class_repository");
    }
}
