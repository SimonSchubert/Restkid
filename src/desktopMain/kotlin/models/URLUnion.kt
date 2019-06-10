package models

sealed class URLUnion {
    class StringValue(val u: String) : URLUnion()
    class URLClassValue(val u: URLClass) : URLUnion()
}