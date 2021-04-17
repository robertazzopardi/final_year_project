#include "intelligence_Maddpg_Models.h"

#include <torch/torch.h>

#include <deque>
#include <iostream>
#include <tuple>
#include <vector>

float** create2dArray(JNIEnv*, jobjectArray*);

struct CriticNet : torch::nn::Module {
    CriticNet(const int obsDim, const int actionDim) {
        // Construct and register two Linear submodules.
        fc1 = register_module("fc1", torch::nn::Linear(obsDim, 1024));
        fc2 = register_module("fc2", torch::nn::Linear(1024 + actionDim, 512));
        fc3 = register_module("fc3", torch::nn::Linear(512, 300));
        fc4 = register_module("fc4", torch::nn::Linear(300, 1));
    }

    // Implement the CriticNet's algorithm.
    torch::Tensor forward(float** xIn, int* aIn) {
        torch::Tensor x = torch::tensor(xIn, {torch::kF64});
        torch::Tensor a = torch::tensor(aIn, {torch::kF64});

        x = torch::relu(fc1(x));
        torch::Tensor xaCat = torch::cat({x, a}, 1);
        torch::Tensor xa = torch::relu(fc2(xaCat));
        xa = torch::relu(fc3(xa));
        torch::Tensor qValue = fc4(xa);

        return qValue;
    }

    void update() {
    }

    // Use one of many "standard library" modules.
    torch::nn::Linear fc1{nullptr},
        fc2{nullptr}, fc3{nullptr}, fc4{nullptr};
};

struct ActorNet : torch::nn::Module {
    ActorNet(const int obsDim, const int actionDim) {
        // Construct and register two Linear submodules.
        fc1 = register_module("fc1", torch::nn::Linear(obsDim, 512));
        fc2 = register_module("fc2", torch::nn::Linear(512, 128));
        fc3 = register_module("fc3", torch::nn::Linear(128, actionDim));
    }

    // Implement the CriticNet's algorithm.
    torch::Tensor forward(float* obsIn) {
        torch::Tensor obs = torch::tensor(obsIn, {torch::kF64});
        torch::Tensor x = torch::relu(fc1(obs));
        x = torch::relu(fc2(x));
        x = torch::tanh(fc3(x));

        return x;
    }

    // Use one of many "standard library" modules.
    torch::nn::Linear fc1{nullptr}, fc2{nullptr}, fc3{nullptr};
};

// struct CriticNet criticNets[4];
// struct CriticNet criticTargetNets[4];
// struct ActorNet actorNets[4];
// struct ActorNet actorTargetNets[4];
// std::vector<CriticNet> criticNets;
// std::vector<CriticNet> criticTargetNets;
// std::vector<ActorNet> actorNets;
// std::vector<ActorNet> actorTargetNets;
// ActorNet m(10, 4);

JNIEXPORT void JNICALL
Java_intelligence_Maddpg_Models_sayHello(JNIEnv* env, jobject thisObject, jobjectArray states) {
    std::cout << env->GetArrayLength(states) << std::endl;

    create2dArray(env, &states);
}

// criticInputs, int criticOutputs, int actorInputs, int actorOutputs
JNIEXPORT void JNICALL Java_intelligence_Maddpg_Models_initNetworks(JNIEnv* env, jobject thisObject, jint criticInputs, jint criticOutputs, jint actorInputs, jint actorOutputs) {
    for (size_t i = 0; i < 4; i++) {
        // criticNets.push_back(CriticNet(criticInputs, criticOutputs));
        // criticTargetNets.push_back(CriticNet(criticInputs, criticOutputs));

        // actorNets.push_back(ActorNet(actorInputs, actorOutputs));
        // actorTargetNets.push_back(ActorNet(actorInputs, actorOutputs));
    }
}

float** create2dArray(JNIEnv* env, jobjectArray* states) {
    jsize OuterDim = env->GetArrayLength(*states);
    std::vector<std::vector<float> > destinationListCpp(OuterDim);

    for (jsize i = 0; i < OuterDim; ++i) {
        jfloatArray inner = static_cast<jfloatArray>(env->GetObjectArrayElement(*states, i));

        // again: null pointer check
        if (inner) {
            // Get the inner array length here. It needn't be the same for all
            // inner arrays.
            jsize InnerDim = env->GetArrayLength(inner);
            destinationListCpp[i].resize(InnerDim);

            jfloat* data = env->GetFloatArrayElements(inner, 0);
            std::copy(data, data + InnerDim, destinationListCpp[i].begin());
            env->ReleaseFloatArrayElements(inner, data, 0);
        }
    }

    float** temp = new float*[destinationListCpp.size()];
    for (unsigned i = 0; (i < destinationListCpp.size()); i++) {
        temp[i] = new float[destinationListCpp.at(i).size()];
        for (unsigned j = 0; (j < 4); j++) {
            temp[i][j] = destinationListCpp[i][j];
        }
    }

    return temp;
}

/*
javac -h . *.java

cd src/main/java/intelligence/Maddpg
clang++ -arch x86_64 -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -I/Users/rob/_CODE/Development/libtorch/include/ -I/Users/rob/_CODE/Development/libtorch/include/torch/csrc/api/include/ intelligence_Maddpg_Models.cpp -o intelligence_Maddpg_Models.o
clang++ -arch x86_64 -dynamiclib -L/Users/rob/_CODE/Development/libtorch/lib/ -D_GLIBCXX_USE_CXX11_ABI=0 -dynamiclib -o libnative.dylib intelligence_Maddpg_Models.o -ltorch -ltorch_cpu -lc10



*/
