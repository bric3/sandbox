// Copy of https://github.com/carldea/panama4newbies/blob/d9791acfa6399d9664909d16f3cd2beb7d6c24ed/macos-touchID/touchid.swift
import LocalAuthentication

@_cdecl("authenticate_user_touchid")
public func authenticateUserApi() {
    authenticateUser() { result in
        switch(result) {
            case .OK: print("OK")
            case .ERROR: print("Error")
            case .UNAVAILABLE: print("UNAVAILABLE")
        }
    }
}

enum AuthResult {
    case OK
    case ERROR
    case UNAVAILABLE
}

// Fixing
// |- error: sending 'runme' risks causing data races
// `- note: task-isolated 'runme' is captured by a main actor-isolated closure. main actor-isolated uses in closure may race against later nonisolated uses
//
// 1. Making `callback` a `@Sendable`
// 2.   
// https://www.donnywals.com/solving-main-actor-isolated-property-can-not-be-referenced-from-a-sendable-closure-in-swift/
// https://www.donnywals.com/dispatching-to-the-main-thread-with-mainactor-in-swift/

// TODO replace by swift actors ?
// https://www.andyibanez.com/posts/understanding-actors-in-the-new-concurrency-model-in-swift/


func authenticateUser(_ callback: @escaping @Sendable (_ result: AuthResult) -> Void) {
    // Create the Local Authentication Context
    let context = LAContext()

    context.localizedFallbackTitle = "Please use your password"
    context.localizedCancelTitle = "Abort"

    var authorizationError: NSError?
    let permitted = context.canEvaluatePolicy(
            //   LAPolicy.deviceOwnerAuthenticationWithBiometrics,
            LAPolicy.deviceOwnerAuthentication, // TouchId or passcode
            error: &authorizationError
    )

    switch (context.biometryType) {
        case .touchID: print("Biometry: Touch ID")
        case .faceID: print("Biometry: Face ID")
        case .opticID: print("Biometry: Optic ID")
        case .none: print("Biometry: none")
        @unknown default:
            print("Biometry: unknown")
    }

    if !permitted { // this only makes sense with LAPolicy.deviceOwnerAuthenticationWithBiometrics
        let ac = "Touch ID not available, Or Your device is not configured for Touch ID."
        print(ac)
        callback(.UNAVAILABLE)
        return
    }

    let reason = "Identify yourself!"
    print(reason)
    let semaphore = DispatchSemaphore(value: 0)

    context.evaluatePolicy(
            //  LAPolicy.deviceOwnerAuthenticationWithBiometrics,
            LAPolicy.deviceOwnerAuthentication,
            localizedReason: reason
    ) { (success: Bool, error: Error?) -> Void in
        DispatchQueue.main.async {
            if success {
                print(" You may enter")
                semaphore.signal()
                callback(.OK)
            } else {
                print(" Authentication failed")
                print(error?.localizedDescription ?? "Failed to authenticate")
                semaphore.signal()
                callback(.ERROR)
            }
        }
    }

    semaphore.wait()
}
