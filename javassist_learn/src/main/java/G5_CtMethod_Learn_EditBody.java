import javassist.*;
import javassist.expr.*;

public class G5_CtMethod_Learn_EditBody {
    // setBody 测试
    public static void test1() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_EditBody$Test");
        CtMethod m = cc.getDeclaredMethod("add");
        m.setName("mul");
        m.setBody("{System.out.println($1 * $2);}");
        cc.writeFile("javassist_learn/src/main/java/class_repository");
        Class<?> aClass = cc.toClass();
        Object instance = aClass.newInstance();
        aClass.getDeclaredMethod("mul", int.class, int.class).invoke(instance, 10, 20);
    }

    // MethodCall、ExprEditor 测试
    public static void test2() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_EditBody$Test");
        CtMethod m = cc.getDeclaredMethod("square");
        m.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall mc) throws CannotCompileException {
                mc.replace("{ System.out.println(\""+mc.where().getName()+"调用"+mc.getClassName()+"类的方法: "+mc.getMethodName() +"\");$1 = 10; $_ = $proceed($$); }");
            }
        });
        cc.writeFile("javassist_learn/src/main/java/class_repository");
        Class<?> aClass = cc.toClass();
        Test instance = (Test)aClass.getDeclaredConstructor().newInstance();
        System.out.println(instance.square(5));
    }

    // ConstructorCall 测试
    public static void test3() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_EditBody$Test");
        CtConstructor ctConstructor = cc.getDeclaredConstructor(null);
        ctConstructor.instrument(new ExprEditor() {
            @Override
            public void edit(ConstructorCall cc) throws CannotCompileException {
                cc.replace("{System.out.println(\"Hello Ko1sh1\");$proceed($$);}");
            }
        });
        cc.writeFile("javassist_learn/src/main/java/class_repository");
        cc.toClass().getDeclaredConstructor().newInstance();
    }

    // ConstructorCall 测试
    public static void test4() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("G5_CtMethod_Learn_EditBody$Test");
        CtMethod m = cc.getDeclaredMethod("fieldTest");
        m.instrument(new ExprEditor() {
            @Override
            public void edit(FieldAccess fa) throws CannotCompileException {
                // 如果是写操作
                if (fa.isWriter()) {
                    fa.replace("{System.out.println(\"写入的值是：\"+$1);}");
                }
                // 如果是读操作
                if (fa.isReader()) {
                    fa.replace("{System.out.println($_=\""+fa.getFieldName()+"被读取\"); }");
                }
            }
        });
        cc.writeFile("javassist_learn/src/main/java/class_repository");
        Test instance = (Test)cc.toClass().getDeclaredConstructor().newInstance();
        instance.fieldTest();
    }

    public static void main(String[] args) throws Throwable {
//        test1();
//        test2();
//        test3();
//        test4();
    }

     class Test {
        private String name;
        public Test(){
        }
        void add(int a, int b) {
            System.out.println(a + b);
        }

        int square(int a){
            System.out.println(a);
            return a*a;
        }
        String fieldTest(){
            this.name = "Ko1sh1";
            return this.name;
        }
    }
}