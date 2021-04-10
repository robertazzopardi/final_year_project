package intelligence.Maddpg;

public class CriticJNI {
    static {
        System.loadLibrary("native");
    }

    // Declare a native method sayHello() that receives no arguments and returns void
    private native void sayHello();
}
