package com.skillw.sts.core.type

interface Identifiable {
    val id:String
    val name:String
}

enum class Accessibility{
    Public,Protected,Private,Packaged
}
enum class Variance{
    Covariant,
    Contravariant,
    Invariant;
}
interface Accessible:Location{
    val accessibility:Accessibility
    fun isPublic() = accessibility == Accessibility.Public
    fun isProtected() = accessibility == Accessibility.Protected
    fun isPrivate() = accessibility == Accessibility.Private
    fun isPackaged() = accessibility == Accessibility.Packaged

    fun isAccessible(other:Location):Boolean{
        return isPublic()
                || (isProtected() && other.locParent == locParent)
                || (isPackaged() && other.fullName.startsWith(fullName))
                || (isPrivate() && other.fullName == fullName)
    }
}
interface GenericHolder<G: Proto.Generic>{
    val genericParent : GenericHolder<G>?
    val generics:List<G>
    fun generic(name:String): Proto.Generic? = generics.firstOrNull { it.id == name } ?: genericParent?.generic(name)
    private fun putAll(context: GenericContext<G>, knowns:List<Type>) =
        generics.forEachIndexed { index, generic ->
           knowns.getOrNull(index)?.let {
              val type =  when(generic){
                   is Proto.ClassGeneric -> Type.Generic.Class(generic,it)
                  else -> Type.Generic.Function(generic, it)
              }
               context.known(generic.id,type)
           } ?: context.unknown(generic.id,generic)
        }
    fun newContext(knowns:List<Type> = emptyList()) : GenericContext<G> =
        genericParent?.newContext()?.apply{putAll(this,knowns)}
        ?: GenericContext{ putAll(this,knowns) }
}

open class GenericContext<G: Proto.Generic>(init: GenericContext<G>.()->Unit = {}) {
    val names = mutableListOf<String>()

    private val unknowns = mutableMapOf<String, G>()
    private val knowns = mutableMapOf<String, Type.Generic>()

    init {
        init()
    }

    open fun known(name:String, value: Type.Generic){
        names += name
        knowns[name] = value
        unknowns.remove(name)
    }

    open fun unknown(name:String, value:G){
        names += name
        unknowns[name] = value
        knowns.remove(name)
    }
    fun putIfAbsent(name:String,value:()-> Type.Generic){
        if (name in knowns) error("Value $name already exists")
        known(name,value())
    }

    fun toKnownList():List<Type> = names.map {
        known(it)
    }
    fun toMap():LinkedHashMap<G, Type> = names.associate { unknown(it) to known(it) }.toMap(LinkedHashMap())


    fun known(name:String): Type = knowns[name] ?: error("Unknown variable $name")
    fun unknown(name:String):G = unknowns[name] ?: error("Unknown variable $name")


}

interface IsAssignable<T> {
    fun isAssignableFrom(other: T, location:Location): Boolean

}

interface Specializable<S> {
    fun<G : Proto.Generic> specialize(context: GenericContext<G>):S
}

interface Location :Identifiable{

    val locParent:Location?
    val fullName:String
        get() = if(locParent == null) id else "${locParent!!.fullName}.$id"

    val packagePath:String
    val any:Proto.Any?
    override val id: String
        get() = fullName

}


 enum class Relation{
    Friend,
    Child,
    Packaged,
    ChildAndPackaged,
    None
}

