/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
// class App {
//     class func hello(_ s: String) -> String {
//         return "Hello " + s
//     }
// }

// let result = App.hello("World")
// print(result)

// https://trycombine.com/posts/swift-actors/
import Foundation
import CryptoKit

@available(macOS 9999, *)
@main
struct App {
  static let cache = HashCache()
  
  static func main() async {
    await cache.addHash(for: 7778)
    await cache.compute()
    await print(cache.hashes[34]!)
  }
}

@available(macOS 9999, *)
actor HashCache {
  private(set) var hashes = [Int: String]()
  
  func addHash(for number: Int) {
    let string = SHA512.hash(data: 
      Data(String(number).utf8)
    ).description
        
    hashes[number] = string
  }
  
  func compute() async {
    addHash(for: 42)
    
    await withTaskGroup(of: Bool.self) { group in
      for number in 0 ... 15_000 {
        group.spawn {
          await self.addHash(for: number)
          return true
        }
      }
    }
  }
  
}