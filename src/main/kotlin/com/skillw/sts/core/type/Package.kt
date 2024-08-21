package com.skillw.sts.core.type

class Package(override val name:String, override val locParent:Package? = null):Location{
    override val fullName:String = if(locParent == null) name else "${locParent.fullName}.$name"
    override val packagePath = fullName
    override val any = null

    val classes = HashMap<String,Proto.Any>()
    val objects = HashMap<String,Type.Struct.Object>()

    fun child(name:String):Package{
        return Package(name,this)
    }

}