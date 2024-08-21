package com.skillw.sts.core.type


object Builtin {
    val kant = Package("kant")

    val Any = object : Type.Struct.Any(Builder.Any(kant, "Any").buildProto()){
        override fun isAssignableFrom(other: Type, location: Location) = true
    }

    val Number = Builder.AnyVal(kant, "Number", parent = Any).buildType()
    val Double = Builder.AnyVal(kant, "Double", parent = Number).buildType()
    val Long = Builder.AnyVal(kant, "Long", parent = Number).buildType()
    val Float = Builder.AnyVal(kant, "Float", parent = Number).buildType()
    val Int = Builder.AnyVal(kant, "Int", parent = Number).buildType()
    val Short = Builder.AnyVal(kant, "Short", parent = Number).buildType()
    val Byte = Builder.AnyVal(kant, "Byte", parent = Number).buildType()
    val Char = Builder.AnyVal(kant, "Char", parent = Any).buildType()
    val String = Builder.AnyVal(kant, "String", parent = Any).buildType()
    val Unit = Builder.AnyVal(kant, "Unit", parent = Any).buildType()
    val Nothing = object : Type.Struct.Any(Builder.Any(kant, "Nothing").buildProto()){
        override fun isAssignableFrom(other: Type, location: Location) = other == this
    }

}