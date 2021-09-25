// swiftc lib.swift -emit-library -o libtouchidswift.dylib

@_cdecl("function")
public func function() {
}

// $ nm libswift.dylib
//     0000000000003fb0 T _$s5swift8functionyyF
//     0000000000003fb6 s ___swift_reflection_version
//     0000000000003fa0 T _function
//                      U dyld_stub_binder
