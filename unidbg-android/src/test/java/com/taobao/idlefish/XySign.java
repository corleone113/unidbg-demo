package com.taobao.idlefish;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.ApplicationInfo;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;

public class XySign extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;

    final String PACKAGE_NAME = "com.taobao.idlefish";

    final String APK_INSTALL_PATH = "/data/app/com.taobao.idlefish-wPuP06nVcptv0MbLQ398xQ==";

    final String APK_NAME = "unidbg-android/src/test/java/com/taobao/idlefish/xianyu.apk";
    final String SO_SMAIN = "unidbg-android/src/test/java/com/taobao/idlefish/libsgmainso-6.6.13.so";

    public DvmClass JNICLibrary;

    XySign() {
        // 创建模拟器实例，进程名填写实际的进程名，避免针对进程名的校验
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(PACKAGE_NAME)
                .build();

        // 绑定IO重定向接口，没有这句，下面的resolve方法不会被调用
        emulator.getSyscallHandler().addIOResolver(this);

        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建虚拟机，传入apk，让unidbg为我们做部分签名校验的工作（最好填绝对路径）
        vm = emulator.createDalvikVM(new File(APK_NAME));

        // 设置JNI
        vm.setJni(this);
        // 打印日志
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        JNICLibrary = vm.resolveClass("com/taobao/wireless/security/adapter/JNICLibrary");

    }

    public static void main(String[] args) {
        XySign tppSign = new XySign();
        tppSign.initMain();
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("corleone! resolve so ==> " + pathname);
        return null;
    }

    // 这个方法重复调用，写成成员变量方便调用
    public String methodSign = "doCommandNative(I[Ljava/lang/Object;)Ljava/lang/Object;";

    public void initMain() {
        // 加载so
        DalvikModule dm = vm.loadLibrary(new File(SO_SMAIN), true);
        dm.callJNI_OnLoad(emulator);

        DvmObject<?> context = vm.resolveClass("com/taobao/idlefish/TaoBaoApplication", vm.resolveClass("android/content/Context")).newObject("taobao");

        DvmObject<?> ret = JNICLibrary.callStaticJniMethodObject(
                emulator, methodSign, 10101,
                new ArrayObject(
                        context,
                        DvmInteger.valueOf(vm, 3),
                        new StringObject(vm, ""),
                        new StringObject(vm, "/data/user/0/" + PACKAGE_NAME + "/app_SGLib"),
                        new StringObject(vm, ""),
                        new StringObject(vm, "com.taobao.idlefish"),
                        new StringObject(vm, "7.10.61"),
//                        DvmInteger.valueOf(vm, 10)
                        new StringObject(vm, "10")
                ));
        System.out.println("corleone!, initMain.ret-10101: " + ret);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/alibaba/wireless/security/mainplugin/SecurityGuardMainPlugin->getMainPluginClassLoader()Ljava/lang/ClassLoader;":
                return vm.resolveClass("java/lang/ClassLoader").newObject(null);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public int callStaticIntMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/uc2/crashsdk/JNIBridge->registerInfoCallback(Ljava/lang/String;IJI)I":
                return 1;
        }
        return super.callStaticIntMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/alibaba/wireless/security/framework/ApmMonitorAdapter->isEnableFullTrackRecord()Z":
                return false;
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "com/taobao/idlefish/TaoBaoApplication->getPackageCodePath()Ljava/lang/String;": {
                // APK_INSTALL_PATH : /data/app/com.alipictures.moviepro-lYhmlkDhtgvVNT1zH6qZIQ\==/base.apk
                return new StringObject(vm, APK_INSTALL_PATH + "/base.apk");
            }
            case "com/taobao/idlefish/TaoBaoApplication->getFilesDir()Ljava/io/File;": {
                return vm.resolveClass("java/io/File").newObject(signature);
            }
            case "java/io/File->getAbsolutePath()Ljava/lang/String;": {
                String sig = dvmObject.getValue().toString();
                System.out.println("corleone! sig:" + sig);
                if (sig.equals("com/taobao/idlefish/TaoBaoApplication->getFilesDir()Ljava/io/File;")) {
                    return new StringObject(vm, "/data/user/0/com.taobao.idlefish/files");
                }
                break;
            }
            case "com/taobao/idlefish/TaoBaoApplication->getApplicationInfo()Landroid/content/pm/ApplicationInfo;": {
                return new ApplicationInfo(vm);
            }

        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/ApplicationInfo->nativeLibraryDir:Ljava/lang/String;":
//                return new StringObject(vm, new File("target").getAbsolutePath());
                return new StringObject(vm, APK_INSTALL_PATH + "/lib/arm64");
        }

        return super.getObjectField(vm, dvmObject, signature);
    }

}
