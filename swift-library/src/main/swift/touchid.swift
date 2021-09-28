// Copy of https://github.com/carldea/panama4newbies/blob/d9791acfa6399d9664909d16f3cd2beb7d6c24ed/macos-touchID/touchid.swift

// Modified
// See https://www.advancedswift.com/face-id-touch-id-swift/

import LocalAuthentication

@_cdecl("authenticate_user_touchid")
public func authenticateUser() {

  // Create the Local Authentication Context
  let context = LAContext()

  context.localizedFallbackTitle = "Please use your Passcode"
  context.localizedCancelTitle = "Enter Username/Password"

  var authorizationError: NSError?
  var permissions = context.canEvaluatePolicy(
   //   LAPolicy.deviceOwnerAuthenticationWithBiometrics,
     LAPolicy.deviceOwnerAuthentication, // TouchId or passcode
     error: &authorizationError
  )

  if permissions {
    let biometry = context.biometryType
    if(biometry != LABiometryType.touchID) {
      print("TouchID not available")
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
        if success {
           print(" You may enter")
           runme = false
        } else {
           print(" Authentication failed")
           print(error?.localizedDescription ?? "Failed to authenticate")
           runme = false
        }
    }

    // can be replaced by actors in later swift versions
    // https://www.andyibanez.com/posts/understanding-actors-in-the-new-concurrency-model-in-swift/
    while runme {}
  } else {
     let ac = "Touch ID not available, Or Your device is not configured for Touch ID."
     print(ac)
  }
}

