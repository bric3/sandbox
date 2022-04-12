// Copy of https://github.com/carldea/panama4newbies/blob/d9791acfa6399d9664909d16f3cd2beb7d6c24ed/macos-touchID/touchid.swift

// Modified
// See https://www.advancedswift.com/face-id-touch-id-swift/

import LocalAuthentication
// import _Concurrency

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

func authenticateUser(_ callback: @escaping (_ result: AuthResult) -> Void) {

//         let handle = Task {
//             Thread.sleep(forTimeInterval: 5)
//             return await "bim"
//         }
//         let result = await handle.value
//         print(result)



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

//     let result = AuthenticationResult()

    switch (context.biometryType) {
        case .touchID: print("Biometry: Touch ID")
        case .faceID: print("Biometry: Face ID")
        case .none: print("Biometry: none")
    }

    if !permitted { // this only makes sense with LAPolicy.deviceOwnerAuthenticationWithBiometrics
        let ac = "Touch ID not available, Or Your device is not configured for Touch ID."
        print(ac)
        callback(.UNAVAILABLE)
        return
    }

    let reason = "Identify yourself!"
    print(reason)
    var runme = true

    context.evaluatePolicy(
            //  LAPolicy.deviceOwnerAuthenticationWithBiometrics,
            LAPolicy.deviceOwnerAuthentication,
            localizedReason: reason
    ) { (success: Bool, error: Error?) -> Void in
        DispatchQueue.main.async {
            if success {
                print(" You may enter")
                runme = false
                callback(.OK)
            } else {
                print(" Authentication failed")
                print(error?.localizedDescription ?? "Failed to authenticate")
                runme = false
                callback(.ERROR)
            }
        }
    }

    // can be replaced by actors in later swift versions
    // https://www.andyibanez.com/posts/understanding-actors-in-the-new-concurrency-model-in-swift/
    while runme {
    }
}

// actor AuthenticationResult {
//     private(set) var success: Bool?
//
//     func setSuccess(success: Bool) {
//       self.success = success
//     }
//
// }
