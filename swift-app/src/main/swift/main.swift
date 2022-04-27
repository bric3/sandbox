/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
