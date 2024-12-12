package com.example.ndk_demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.Module;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    MainActivity() {
        // 创建模拟器实例,进程名建议依照实际进程名填写，可以规避针对进程名的校验
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.example.ndk_demo").build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/example/ndk_demo/app-release.apk"));
        //
//        vm = emulator.createDalvikVM(null);

        // 加载目标SO
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/example/ndk_demo/libndk_demo.so"), true); // 加载so到虚拟内存
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        vm.setJni(this); // 设置JNI
        vm.setVerbose(true); // 打印日志

        dm.callJNI_OnLoad(emulator); // 调用JNI OnLoad
    }

    public static void main(String[] args) {
        MainActivity activity = new MainActivity();
        activity.call_stringFromJNI3();
    }

    public void call_stringFromJNI() {
        DvmClass CActivity = vm.resolveClass("com/example/ndk_demo/MainActivity");
        DvmObject<?> obj = CActivity.newObject(null);
        DvmObject<String> result = obj.callJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        System.out.println("result=" + result);
    }

    // 下面这种方式只适合静态注册的函数
    public void call_stringFromJNI2() {
        //        Module.emulateFunction(emulator,)
        List<Object> list = new ArrayList<>(10);
        // arg1 env
        list.add(vm.getJNIEnv());
        // arg2 jobject/jclazz 一般用不到，直接填0
        list.add(0);
        // 返回的数字ret是一个引用地址，通过vm.getObject来进行解引用。
        Number ret = module.callFunction(emulator, "Java_com_example_ndk_1demo_MainActivity_stringFromJNI", list.toArray());
        System.out.println("result1=" + vm.getObject(ret.intValue()));
    }

    // 下面这种方式是通过函数的偏移量(通过IDA之类的工具查看)
    public void call_stringFromJNI3(){
        List<Object> list = new ArrayList<>(10);
        // arg1 env
        list.add(vm.getJNIEnv());
        // arg2 jobject/jclazz 一般用不到，直接填0
        list.add(0);
        Number ret = module.callFunction(emulator, 0x151F0, list.toArray());
        System.out.println("result2=" + vm.getObject(ret.intValue()));
    }
}
