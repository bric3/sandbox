import LocalAuthentication

@_cdecl("authenticate")
public func authenticateUser() {
    let context = LAContext()

    context.localizedFallbackTitle = "Please use your password"
    context.localizedCancelTitle = "Abort"

    var authorizationError: NSError?
    let permissions = context.canEvaluatePolicy(
            LAPolicy.deviceOwnerAuthenticationWithBiometrics, // TouchId or passcode
            error: &authorizationError
    )

    if !permissions {
        let ac = "Touch ID not available, Or Your device is not configured for Touch ID."
        print(ac)
        return
    }

    let biometry = context.biometryType
    if (biometry != LABiometryType.touchID) {
        print("TouchID not available")
        return
    }

    let reason = "Identify yourself!"
    print(reason)

    var waitForResult = true
    context.evaluatePolicy(
            LAPolicy.deviceOwnerAuthentication,
            localizedReason: reason
    ) { (success: Bool, error: Error?) -> Void in
          if success {
              print("✅ You may enter")
          } else {
              print("⛔️ Authentication failed")
              print(error?.localizedDescription ?? "Failed to authenticate")
          }
          waitForResult = false
          return
    }

    while waitForResult {} // block here without a loop
//   RunLoop.main.run()
}

authenticateUser()
print("bye")
