package com.skillw.sts.core.type


sealed interface Member<S> :Accessible, Specializable<S> {
    val type:Type
    override val locParent: Location
    override val packagePath: String
        get() = locParent.packagePath
    override val any: Proto.Any?
        get() = locParent.any
    override val id: String
        get() = "${accessibility.name.lowercase()} $name : ${type.id}"

    class Field(
        override val accessibility: Accessibility,
         val mutable:Boolean,
        override val name: String,
        override val type: Type,
        override val locParent: Location
    ) :Member<Field> {
        override val id: String
            get() = "${accessibility.name.lowercase()} $name : ${type.id}"
        override fun <G : Proto.Generic> specialize(context: GenericContext<G>) =  Field(accessibility, mutable, name, type.specialize(context),locParent)
    }
    class Method(
        override val accessibility: Accessibility,
        override val name: String,
        override val type: Type.Function,
        override val locParent: Location
    ) :Member<Method> {
        override val id: String
            get() = "${accessibility.name.lowercase()} $name : ${type.id}"

        override fun <G : Proto.Generic> specialize(context: GenericContext<G>) = Method(accessibility, name, type.specialize(context),locParent)
    }
}



