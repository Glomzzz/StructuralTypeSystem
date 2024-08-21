@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.skillw.sts.core.type

sealed interface Proto:Identifiable {
    open class Any internal constructor(
        final override val locParent: Location,
        override val name:String,
        override val accessibility: Accessibility = Accessibility.Public,
        val parent: Type.Struct.Any? = Builtin.Any,
        val traits: List<Type.Struct.Trait> = mutableListOf(),
        override val generics: List<ClassGeneric> = mutableListOf(),
        override val genericParent: GenericHolder<ClassGeneric>? = null,
        val fields: Container.Field = Container.Field(),
        val methods: Container.Method = Container.Method(),
    ) : Proto,Accessible,GenericHolder<ClassGeneric> {
        override val id:String
            get() = name + generics.joinToString(prefix = if (generics.isNotEmpty()) "<" else "", postfix = if (generics.isNotEmpty()) ">" else "") { it.id }
        override val packagePath = locParent.packagePath
        override val any by lazy{ this }
        override val fullName: String
            get() = "${locParent.fullName}.$name"
        fun hasParent(any:Any):Boolean = (this == any) || parent?.hasParent(any) == true
        fun isActual() = generics.isEmpty() || generics.all { it is Type.Generic.Class && it.type is Proto.Any }
    }
    open class Trait internal constructor(
        locParent: Location,
        name:String,
        accessibility: Accessibility = Accessibility.Public,
        traits: List<Type.Struct.Trait> = mutableListOf(),
        generics: List<ClassGeneric> = mutableListOf(),
        fields: Container.Field = Container.Field(),
        methods: Container.Method = Container.Method(),
    ) : Any(locParent,name,accessibility, Builtin.Any, traits, generics, null, fields,methods)

    open class Object internal constructor(
        locParent: Location,
        name: String,
        accessibility: Accessibility,
        parent: Type.Struct.Any?,
        traits: List<Type.Struct.Trait>,
        fields: Container.Field = Container.Field(),
        methods: Container.Method = Container.Method(),
    ) : Any(locParent,name, accessibility, parent, traits, emptyList(), null, fields, methods)
    open class Generic internal constructor(
        override val name:String,
        val upper: Type = Builtin.Any,
        val lower: Type = Builtin.Nothing
    ) : Proto,IsAssignable<Type> {
        override val id
            get() = "${lower.id} <: $name <: $upper"

        override fun isAssignableFrom(other: Type,location: Location) =
            upper.isAssignableFrom(other,location) && other.isAssignableFrom(lower,location)
    }
    open class ClassGeneric internal constructor(
        name:String,
        upper: Type = Builtin.Any,
        lower: Type = Builtin.Nothing,
        val variance: Variance = Variance.Covariant
    ): Generic(name,upper,lower){
        override val id: String
            get() = "${lower.id} <: $name(${variance.name.lowercase()}) <: ${upper.id}"
    }
}