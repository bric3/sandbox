class App {
    class func hello(_ s: String) -> String {
        return "Hello " + s
    }
}

let result = App.hello("World")
print(result)