package com.skillw.sts.core.type

sealed interface ProtoBuilder<P:Proto>{ fun buildProto():P }
sealed interface TypeBuilder<T:Type>{ fun buildType():T }
sealed interface MemberBuilder<M:Member<*>>{ fun buildMember():M }
sealed class Builder
    (
    val loc:Location,
    val name:String,
) {

    sealed class AnyBuilder(
        loc:Location,
        name:String
    ):Builder(loc,name){
        protected val generics = mutableListOf<Proto.ClassGeneric>()
        protected val fields: Container.Field = Container.Field()
        protected val methods: Container.Method = Container.Method()

        fun generic(
            name:String,
            upper:Type = Builtin.Any,
            lower:Type = Builtin.Nothing,
            variance: Variance = Variance.Covariant
        ) = Proto.ClassGeneric(name,upper,lower,variance).also { generics.add(it) }

        fun field(name:String,builder:Field.()->Unit){
            fields.field(Field(loc,name).apply(builder).buildMember())
        }
        fun method(name:String,builder:Method.()->Unit){
            methods.method(Method(loc,name).apply(builder).buildMember())
        }

    }
    class Any(
        loc:Location,
        name:String,
        accessibility: Accessibility = Accessibility.Public,
        parent: Type.Struct.Any? = Builtin.Any,
        traits: MutableList<Type.Struct.Trait> = mutableListOf()
    ):AnyBuilder(loc, name),ProtoBuilder<Proto.Any>,TypeBuilder<Type.Struct.Any>{
        private val proto = Proto.Any(loc,name,accessibility,parent,traits,generics,null,fields,methods)
        override fun buildProto() = proto
        override fun buildType() = Specialize.Any(proto).build()
    }
    class AnyVal(
        loc:Location,
        name:String,
        accessibility: Accessibility = Accessibility.Public,
        parent: Type.Struct.Any? = Builtin.Any,
        traits: MutableList<Type.Struct.Trait> = mutableListOf()
    ):AnyBuilder(loc, name),ProtoBuilder<Proto.Any>,TypeBuilder<Type.Struct.AnyVal>{
        private val proto = Proto.Any(loc,name,accessibility,parent,traits,generics,null,fields,methods)
        override fun buildProto() = proto
        override fun buildType() = Type.Struct.AnyVal(proto)
    }
    class Trait(
        loc:Location,
        name:String,
        accessibility: Accessibility = Accessibility.Public,
        traits: MutableList<Type.Struct.Trait> = mutableListOf()
    ):AnyBuilder(loc, name),ProtoBuilder<Proto.Trait>,TypeBuilder<Type.Struct.Trait>{
        private val proto = Proto.Trait(loc,name,accessibility,traits,generics,fields,methods)
        override fun buildProto() = proto
        override fun buildType() = Specialize.Trait(proto).build()
    }

    class Object(
        loc:Location,
        name:String,
        accessibility: Accessibility = Accessibility.Public,
        parent: Type.Struct.Any? = Builtin.Any,
        traits: MutableList<Type.Struct.Trait> = mutableListOf()
    ):AnyBuilder(loc, name),ProtoBuilder<Proto.Object>,TypeBuilder<Type.Struct.Object>{
        private val proto = Proto.Object(loc,name,accessibility,parent,traits,fields,methods)
        override fun buildProto() = proto
        override fun buildType() = Type.Struct.Object(proto)
    }
    open class Function(
        loc:Location,
        name:String,
        private val genericParent:GenericHolder<Proto.Generic>? = null
    ):Builder(loc,name),TypeBuilder<Type.Function>{
        private val generics = ArrayList<Proto.Generic>()
        private val types = ArrayList<Type>()

        fun generic(
            name:String,
            upper:Type = Builtin.Any,
            lower:Type = Builtin.Nothing
        ) = Proto.Generic(name,upper,lower).also {
            generics.add(it)
        }

        fun type(vararg types:Type):Function{
            this.types.addAll(types)
            return this
        }
        override fun buildType() = Type.Function(types.toTypedArray(),generics,genericParent)
    }
    class Field(
        loc:Location,
        name:String,
        private var accessibility: Accessibility = Accessibility.Public
    ):Builder(loc,name),MemberBuilder<Member.Field>{
        private var type:Type? = null
        private var mutable:Boolean = false
        fun mutable(mutable:Boolean){
            this.mutable = mutable
        }
        fun type(type:Type){
            this.type = type
        }

        fun accessibility(accessibility: Accessibility){
            this.accessibility = accessibility
        }
        override fun buildMember() = Member.Field(accessibility,false,name,type!!,loc)

    }
    class Method(
        loc:Location,
        name:String,
        private var accessibility: Accessibility = Accessibility.Public
    ):Builder(loc,name),MemberBuilder<Member.Method>{
        private var type:Type.Function? = null
        fun type(builder:Function.()->Unit){
            val function = Function(loc,name)
            type = function.apply(builder).buildType()
        }

        fun accessibility(accessibility: Accessibility){
            this.accessibility = accessibility
        }
        override fun buildMember() = Member.Method(accessibility,name,type ?: Type.Function(arrayOf(Builtin.Unit)),loc)
    }

    class ClassGeneric(
        loc:Location,
        name:String,
        private val upper:Type = Builtin.Any,
        private val lower:Type = Builtin.Nothing
    ):Builder(loc,name),ProtoBuilder<Proto.ClassGeneric>{
        override fun buildProto() = Proto.ClassGeneric(name,upper,lower)

        fun buildType(type: Type) = Specialize.ClassGeneric(buildProto(),loc).type(type).build()
    }

}


private interface Specialize<P:Proto,T>{
    val proto: P

    sealed class AnySpecialize<P:Proto.Any,T>(
        final override val proto: P,
        generics:List<Type> = emptyList(),
        init: AnySpecialize<P,T>.()->Unit = {}
    )
        :Specialize<P,T> {
        override val location = proto
        val generics = proto.newContext(generics)
        init { init() }
        fun generic(id:String, type:Type) {
            val proto = generics.unknown(id)
            val genericType = ClassGeneric(proto,location).type(type).build()
            this.generics.known(id, genericType)
        }
        fun parent():Type.Struct.Any? = proto.parent?.let { parent ->
           if (parent.isActual()) parent else Any(parent,generics.toKnownList()).build()
        }

        fun traits():List<Type.Struct.Trait> = proto.traits.map { trait ->
            if (trait.isActual()) trait else Trait(trait).build()
        }

        fun members(parent: Type.Struct.Any?,traits: List<Type.Struct.Trait>):Pair<Container.Field,Container.Method>{
            val fields = proto.fields.mapValues { it.specialize(generics) }
            val methods = proto.methods.mapValues { it.specialize(generics) }
            parent?.let { p ->
                p.fields.filterForEach({it.isPublic()},fields::field)
                p.methods.filterForEach({it.isPublic()},methods::method)
            }
            traits.forEach { trait ->
                trait.fields.forEach(fields::field)
                trait.methods.forEach(methods::method)
            }
            return fields to methods
        }
    }

    class Any(
        proto: Proto.Any,
        generics:List<Type> = emptyList(),
        init: AnySpecialize<Proto.Any, Type.Struct.Any>.()->Unit = {}
    ) : AnySpecialize<Proto.Any,Type.Struct.Any>(proto, generics, init) {
        override fun build(): Type.Struct.Any {
            val parent = parent()
            val traits = traits()
            val (fields,methods) = members(parent,traits)
            return Type.Struct.Any(proto,generics.toMap().map { (generic,type)->
                ClassGeneric(generic,location).type(type).build()
            },parent,traits,fields,methods)
        }

    }
    class Trait(
        proto: Proto.Trait,
        generics:List<Type> = emptyList(),
        init: AnySpecialize<Proto.Trait, Type.Struct.Trait>.()->Unit = {}
    ) : AnySpecialize<Proto.Trait,Type.Struct.Trait>(proto, generics, init) {
        override fun build(): Type.Struct.Trait {
            val traits = traits()
            val (fields,methods) = members(null,traits)
            return Type.Struct.Trait(
                proto,
                generics.toMap().map { (generic,type)-> ClassGeneric(generic,location).type(type).build() },
                traits,fields,methods)
        }

    }
    class ClassGeneric(override val proto: Proto.ClassGeneric, override val location: Location)
        : Specialize<Proto.ClassGeneric,Type.Generic.Class>{
        private var type:Type? = null
        fun type(type:Type):ClassGeneric{
            if (!proto.isAssignableFrom(type,location)) error("Type ${proto.id} is not assignable from ${type.id}")
            this.type = type
            return this
        }
        override fun build(): Type.Generic.Class {
            val type = type ?: error("Type is not set")
            return Type.Generic.Class(proto,type)
        }

    }
    fun build():T
    val location:Location

}