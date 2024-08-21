@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package com.skillw.sts.core.type


sealed interface Type: Proto, IsAssignable<Type>, Specializable<Type> {



    sealed interface Struct:Accessible {
        fun structural(location: Location): Structural
        fun structuralWithParent(location: Location): Structural


       open class Any internal constructor(
            proto : Proto.Any,
            generics: List<Generic.Class> = emptyList(),
            parent: Any? = proto.parent,
            traits: List<Trait> = emptyList(),
            fields: Container.Field = Container.Field(),
            methods: Container.Method = Container.Method(),
        )
           : Type,Struct, Proto.Any(proto.locParent,proto.name, proto.accessibility, parent, traits, generics, proto.genericParent, fields, methods) {
               override fun isAssignableFrom(other: Type,location: Location): Boolean {
                return when (other) {
                    this, Builtin.Nothing ->  true
                    is Generic.Function -> isAssignableFrom(other.upper,location)
                    is Generic.Class -> when(other.variance){
                        Variance.Covariant -> isAssignableFrom(other.type,location)
                        Variance.Contravariant -> other.type.isAssignableFrom(this,location)
                        Variance.Invariant -> other.type == this
                    }
                    is AnyVal -> false
                    is Function  -> false
                    is Struct -> {
                        other as Proto.Any
                        val thiz = structural(location)
                        val that = other.structural(location)
                        thiz.isAssignableFrom(that,location)
                                || thiz.isAssignableFrom(other.structuralWithParent(location),location)
                    }
                }
            }
            private val cache by lazy(LazyThreadSafetyMode.NONE) { Structural.Cache(this) }

            override fun structural(location: Location) = cache.get(location)
           override fun structuralWithParent(location: Location) = cache.getWithParent(location)

       }

        class AnyVal internal constructor(
            proto: Proto.Any,
            parent: Any? = proto.parent,
            traits: List<Trait> = emptyList(),
        ) : Any(proto, emptyList(), parent, traits, proto.fields, proto.methods){
            override fun isAssignableFrom(other: Type, location: Location): Boolean {
                return when(other){
                    this,Builtin.Nothing -> true
                    is AnyVal -> isAssignableFrom(other.parent ?: Builtin.Any,location) && traits.all { it.isAssignableFrom(other,location) }
                    else -> false
                }
            }
        }
         class Trait internal constructor(
            proto: Proto.Trait,
            generics: List<Generic.Class> = emptyList(),
            traits: List<Trait> = emptyList(),
            fields: Container.Field = Container.Field(),
            methods: Container.Method = Container.Method(),
        ) :Type,Struct,Proto.Trait(proto.locParent,proto.name, proto.accessibility, traits, generics,  fields, methods){

            override val id: String
                get() = name
            private val function = fields.size == 0 && methods.size == 1
            private val onlyMember:Member.Method? = if (function) methods.firstOrNull() else null
            override fun isAssignableFrom(other: Type, location: Location): Boolean {
                return when(other){
                    this,Builtin.Nothing -> true
                    is Function -> onlyMember?.type?.isAssignableFrom(other,location) ?: false
                    is Generic.Function -> isAssignableFrom(other.upper,location)
                    is Generic.Class -> isAssignableFrom(other.type,location)
                    is Struct ->
                        structural(location).run {
                            isAssignableFrom(other.structural(location),location)
                                    || isAssignableFrom(other.structuralWithParent(location),location)
                        }
                }
            }

             val structural:Structural = run {

                 Structural(proto.locParent,proto.packagePath,this,name,fields,methods)
             }
            override fun structural(location: Location) = structural
             override fun structuralWithParent(location: Location) = structural

         }

         class Object internal constructor(
            proto: Proto.Object,
            accessibility: Accessibility = proto.accessibility,
            parent: Any? = proto.parent,
            traits: List<Trait> = emptyList(),
            fields: Container.Field = Container.Field(),
            methods: Container.Method = Container.Method(),
        ) :Type,Struct,Proto.Object(proto.locParent,proto.name,accessibility, parent, traits, fields, methods){
            override val id: String
                get() = name
            override fun isAssignableFrom(other: Type, location: Location): Boolean {
                return when(other){
                    this -> true
                    is Struct ->
                        structural(location).isAssignableFrom(other.structural(location),location)
                                || structural(location).isAssignableFrom(other.structuralWithParent(location),location)
                    else -> false
                }
            }

             private val cache by lazy(LazyThreadSafetyMode.NONE) { Structural.Cache(this) }
            override fun structural(location: Location) = cache.get(location)
             override fun structuralWithParent(location: Location) = cache.getWithParent(location)
         }
    }

    sealed interface Generic:Type{
        val type:Type?

        class Function internal constructor(
            name:String,
            override var type: Type? = null,
            upper: Type = Builtin.Any,
            lower: Type = Builtin.Nothing
        ): Generic, Proto.Generic(name, upper, lower) {
            constructor(proto:Proto.Generic, type: Type): this(proto.name,type,proto.upper,proto.lower)

            override val id:String
                get() = name

            override fun isAssignableFrom(other: Type, location: Location): Boolean {
                return upper.isAssignableFrom(other,location) && other.isAssignableFrom(lower,location)
            }

        }

        class Class internal constructor(
            name:String,
            override val type: Type,
            upper: Type,
            lower: Type,
            variance: Variance = Variance.Covariant
        ): Generic, Proto.ClassGeneric(name,upper, lower, variance) {

            constructor(proto:Proto.ClassGeneric, type: Type): this(proto.name,type,proto.upper,proto.lower,proto.variance)
            override val id:String
                get() = name

            override fun isAssignableFrom(other: Type,location:Location) = when(variance){
                Variance.Covariant -> type.isAssignableFrom(other,location)
                Variance.Contravariant -> other.isAssignableFrom(type,location)
                Variance.Invariant -> type == other
            } || other == Builtin.Nothing

            override fun<G : Proto.Generic> specialize(context: GenericContext<G>): Type = type.specialize(context)
        }
    }

    // Function Generic



    open class Function(
        val types:Array<Type>,
        override val generics:List<Proto.Generic> = emptyList(),
        override val genericParent: GenericHolder<Proto.Generic>? = null
    ): Type, GenericHolder<Proto.Generic>{
        override val id: String
            get() = types.joinToString(prefix = "( ", separator = " -> ", postfix = " )") { it.id }

        override val name: String
            get() = id

        fun apply(location:Location,vararg args: Type): Type {
            var index = 0
            while (index++ < args.size){
                val arg = args[index]
                val param = types.getOrNull(index) ?: error("Connot apply ${arg.id} to ${this.id}")
                if (!param.isAssignableFrom(param,location)) error("Type mismatch")
            }
            return if (index == types.lastIndex) types[index]
            else Function(types.sliceArray(index until types.size))
        }

        // isAssignableFrom 1
        override fun isAssignableFrom(other: Type, location: Location): Boolean {
            if (other !is Function || types.size != other.types.size) return false
            val last = types.size-1
            for (index in 0 until  last){
                val thiz = types[index]
                val that = other.types[index]
                if (that.isAssignableFrom(thiz,location)) return false
            }
            return types[last].isAssignableFrom(other.types[last],location)
        }

        override fun<G : Proto.Generic> specialize(context: GenericContext<G>): Function = Function(types.map { it.specialize(context) }.toTypedArray())

    }
    override fun<G : Proto.Generic> specialize(context: GenericContext<G>): Type = this

}