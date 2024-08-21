package com.skillw.sts.core.type

interface Container{
    class Field(private val fields: LinkedHashMap<String, Member.Field> = LinkedHashMap()):IsAssignable<Field>{

        constructor(field:Field):this(LinkedHashMap(field.fields))
        val values: Collection<Member.Field>
            get() = fields.values
        val size
            get() = fields.size

        fun field(name:String): Member.Field? = fields[name]
        fun field(field: Member.Field){
            if (fields.containsKey(field.name)){
                val parent = fields[field.name]!!
                if (parent.mutable && parent.type != field.type)
                    error("Field with same name already exists, and because it's mutable, it must have the same type")
            }
            fields[field.name] = field
        }
        fun forEach(action:(Member.Field)->Unit) = fields.values.forEach(action)
        fun mapValues(action:(Member.Field)->Member.Field):Field = Field(LinkedHashMap(fields.mapValues { action(it.value) }))
        fun filterForEach(action:(Member.Field)->Boolean, todo:(Member.Field) -> Unit) = fields.values.forEach { if (action(it)) todo(it) }
        fun hasField(name:String) = fields.containsKey(name)
        override fun isAssignableFrom(other: Field, location: Location): Boolean {
            if (fields.size > other.fields.size) return false
            return fields.all { (name, field) ->
                val otherField = other.fields[name] ?: return false
                field.type.isAssignableFrom(otherField.type, location)
            }
        }

        override fun toString(): String {
            return fields.values.joinToString("\n") { it.id }
        }

    }

    class Method(private val methods: MutableMap<String, MutableList<Member.Method>> = mutableMapOf()):IsAssignable<Method>{

        constructor(method: Method):this(method.methods.mapValues { it.value.toMutableList() }.toMutableMap())
        val size
            get() = methods.size
        fun method(name:String):List<Member.Method>? = methods[name]

        fun method(location: Location, name:String, vararg argTypes: Type): Member.Method?{
            val list = methods[name] ?: return null
            return list.firstOrNull {
                it.type.apply(location,*argTypes)
                true
            }
        }
        fun forEach(action:(Member.Method)->Unit) = methods.values.flatten().forEach(action)
        fun method(method: Member.Method){
            methods.getOrPut(method.name){ mutableListOf() }.apply {
                if (any { it.type == method.type }) return
//                    error("Method with same signature already exists")
                add(method)
            }
        }
        fun mapValues(action:(Member.Method)->Member.Method):Method = Method(methods.mapValues { it.value.map(action).toMutableList() }.toMutableMap())
        fun filterForEach(action:(Member.Method)->Boolean,todo:(Member.Method)->Unit) = methods.values.flatten().forEach { if (action(it)) todo(it) }
        fun hasMethod(name:String) = methods.containsKey(name)
        override fun isAssignableFrom(other: Method, location: Location): Boolean {
            if (methods.size > other.methods.size) return false
            return methods.all { (name, list) ->
                val others = other.methods[name] ?: return false
                if (list.size > others.size) return false
                list.all { thisMethod ->
                    others.any { otherMethod ->
                        otherMethod.type.isAssignableFrom(thisMethod.type, location)
                    }
                }
            }
        }
        fun firstOrNull(): Member.Method? = methods.values.firstOrNull()?.firstOrNull()

        override fun toString(): String {
            return methods.values.flatten().joinToString("\n") { it.id }
        }
    }
}