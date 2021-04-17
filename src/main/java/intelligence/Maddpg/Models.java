package intelligence.Maddpg;

public class Models {

    static {
        System.loadLibrary("torch_cpu");
        System.loadLibrary("torch");
        System.loadLibrary("c10");

        System.loadLibrary("native");
    }

    // public static void main(final String[] args) {
    // new Models().sayHello(new float[][] {new float[] {1.2f, 2, 3, 4}});
    // }

    // Declare a native method sayHello() that receives no arguments and returns void
    private native void sayHello(float[][] state);

    public native void initNetworks(int criticInputs, int criticOutputs, int actorInputs,
            int actorOutputs);

}
