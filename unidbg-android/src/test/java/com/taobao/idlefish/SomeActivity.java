package com.taobao.idlefish; // 闲鱼包名

import com.example.ndk_demo.MainActivity;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class SomeActivity extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    SomeActivity(){
        // 创建模拟器实例,进程名建议依照实际进程名填写，可以规避针对进程名的校验
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.taobao.idlefish").build();
//        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.corleone.flutter_1").build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/taobao/idlefish/xianyu.apk"));
//         vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/taobao/idlefish/app-release.apk"));
//        vm = emulator.createDalvikVM(null);

        // 加载目标SO
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/taobao/idlefish/libflutter.so"), true); // 加载so到虚拟内存
//        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/taobao/idlefish/libflutter1.so"), true);
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        vm.setJni(this); // 设置JNI
        vm.setVerbose(true); // 打印日志

        dm.callJNI_OnLoad(emulator); // 调用JNI OnLoad
    }

    public static void main(String[] args) {
        SomeActivity activity = new SomeActivity();
    }
}
