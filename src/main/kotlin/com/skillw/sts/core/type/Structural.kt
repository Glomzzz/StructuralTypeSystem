package com.skillw.sts.core.type

open class Structural(
    override val locParent: Location,
    override val packagePath: String,
    override val any: Proto.Any?,
    override val name: String,
    private val fields:Container.Field,
    private val methods:Container.Method,
):Location,IsAssignable<Structural>{

    internal class Cache(val any:Proto.Any){
        private fun packaged(mem:Member<*>) = !mem.isPrivate() && !mem.isProtected()
        private fun child(mem:Member<*>) = !mem.isPrivate() && !mem.isPackaged()
        private fun packagedAndChild(mem:Member<*>) = !mem.isPrivate()
        private fun public (mem:Member<*>) = mem.isPublic()
        private fun generate(any:Proto.Any,fields:Container.Field,methods: Container.Method): Structural = Structural(any.locParent,any.packagePath,any,any.name,fields,methods)

        private val friend: Structural
        private val packaged: Structural
        private val child: Structural
        private val packagedAndChild: Structural
        private val public : Structural
        init {
            val friendFields = any.fields
            val packagedFields = Container.Field()
            val childFields = Container.Field()
            val packagedAndChildFields = Container.Field()
            val publicFields = Container.Field()
            friendFields.forEach {
                if (packaged(it)) packagedFields.field(it)
                if (child(it)) childFields.field(it)
                if (packagedAndChild(it)) packagedAndChildFields.field(it)
                if (public(it)) publicFields.field(it)
            }
            val friendMethods = any.methods
            val packagedMethods = Container.Method()
            val childMethods = Container.Method()
            val packagedAndChildMethods = Container.Method()
            val publicMethods = Container.Method()
            friendMethods.forEach {
                if (packaged(it)) packagedMethods.method(it)
                if (child(it)) childMethods.method(it)
                if (packagedAndChild(it)) packagedAndChildMethods.method(it)
                if (public(it)) publicMethods.method(it)
            }
            friend = generate(any,friendFields,friendMethods)
            packaged = generate(any,packagedFields,packagedMethods)
            child = generate(any,childFields,childMethods)
            packagedAndChild = generate(any,packagedAndChildFields,packagedAndChildMethods)
            public = generate(any,publicFields,publicMethods)
        }

        fun relation(location: Location): Relation {
            if (location.any?.fullName?.startsWith(any.fullName) == true) return Relation.Friend
            val packaged = location.packagePath.startsWith(any.packagePath)
            val child = location.any?.hasParent(this.any) == true
            return when {
                child ->
                    if (packaged) Relation.ChildAndPackaged
                    else Relation.Child
                packaged -> Relation.Packaged
                else -> Relation.None
            }
        }

        fun get(loc: Location) = when(relation(loc)){
            Relation.Friend -> friend
            Relation.Packaged -> packaged
            Relation.Child -> child
            Relation.ChildAndPackaged -> packagedAndChild
            Relation.None -> public
        }
        fun getWithParent(loc: Location):Structural{
            val thiz = get(loc)
            val parent = any.parent?.structural(loc) ?: thiz
            val fields = Container.Field(parent.fields)
            val methods = Container.Method(parent.methods)
            thiz.fields.forEach(fields::field)
            thiz.methods.forEach(methods::method)
            return Structural(locParent = loc,packagePath = loc.packagePath,any = any,name = any.name,fields,methods)
        }
    }


    override val id: String
        get() = name

    fun field(name:String):Member.Field? = fields.field(name)
    fun method(location: Location,name:String,vararg argTypes:Type):Member.Method? = methods.method(location,name,*argTypes)

    fun hasField(name:String) = fields.hasField(name)

    fun hasMethod(name:String) = methods.hasMethod(name)
    override fun isAssignableFrom(other: Structural, location: Location): Boolean {
        return fields.isAssignableFrom(other.fields,location) && methods.isAssignableFrom(other.methods,location)
    }

    override fun toString(): String {
        return "Structural of $id { " +
                "$fields\n" +
                "$methods\n" +
                "}"
    }

}