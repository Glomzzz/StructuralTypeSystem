package com.skillw.sts.core.type

import org.junit.jupiter.api.Test
class TypeTest{
    val loc = Package("kant")
    @Test
    fun assignable_Equals(){
        assert(Builtin.Int.isAssignableFrom(Builtin.Int,loc))
    }
    val traitA = Builder.Trait(loc, "traitA").apply {
        method("a"){}
    }.buildType()

    val classA  = Builder.Any(loc, "classA").apply {
        method("a"){}
    }.buildType()
    @Test
    fun assignable_Trait_Class(){
        assert(classA.isAssignableFrom(traitA,loc))
    }

    @Test
    fun assignable_Trait_Trait(){
        assert(traitA.isAssignableFrom(traitA,loc))
    }

    val genericT = Type.Generic.Function("T")
    val genericN = Type.Generic.Function("N", upper = Builtin.Number)
    @Test
    fun assignable_Trait_Generic(){
        assert(genericT.isAssignableFrom(traitA,loc))
        assert(!genericN.isAssignableFrom(traitA,loc))
        assert(!traitA.isAssignableFrom(genericN,loc))
    }


    val classB  = Builder.Any(loc, "classB").apply {
        method("b"){}
    }.buildType()
    @Test
    fun assignable_Class_Trait(){
        assert(traitA.isAssignableFrom(classA,loc))
        assert(!traitA.isAssignableFrom(classB,loc))
        assert(traitA.isAssignableFrom(Builtin.Nothing,loc))
    }

    @Test
    fun assignable_Class_Generic(){
        assert(genericT.isAssignableFrom(classA,loc))
        assert(genericN.isAssignableFrom(Builtin.Number,loc))
        assert(genericN.isAssignableFrom(Builtin.Nothing,loc))
        assert(!genericN.isAssignableFrom(classA,loc))
        assert(!classA.isAssignableFrom(genericN,loc))
    }

    @Test
    fun assignable_Generic_Generic(){
        assert(genericT.isAssignableFrom(genericN,loc))
        assert(!genericN.isAssignableFrom(genericT,loc))
    }

    val genericA = Type.Generic.Function("A", upper = traitA)
    @Test
    fun assignable_Generic_Trait(){
        assert(!traitA.isAssignableFrom(genericT,loc))
        assert(!traitA.isAssignableFrom(genericN,loc))
        assert(traitA.isAssignableFrom(genericA,loc))
    }

    @Test
    fun assignable_Generic_Class(){
        assert(classA.isAssignableFrom(genericA,loc))
        assert(!classA.isAssignableFrom(genericT,loc))
        assert(!classA.isAssignableFrom(genericN,loc))
    }

    val classGenericT = Builder.ClassGeneric(loc, "T").buildType(traitA)
    @Test
    fun assignable_ClassGeneric(){
        assert(genericA.isAssignableFrom(classGenericT,loc))
        assert(classGenericT.isAssignableFrom(genericA,loc))
        assert(kotlin.runCatching {
            Builder.ClassGeneric(loc, "T_fail", upper = classB).buildType(traitA)
        }.isFailure)
    }
    val pack2 = loc.child("sub")
    val pack3 = Package("kant2")
    val basic = Builder.Any(loc, "Basic").apply {
        method("privateMethod"){
            accessibility(Accessibility.Private)
        }
        method("protectedMethod"){
            accessibility(Accessibility.Protected)
        }
        method("publicMethod"){
            accessibility(Accessibility.Public)
        }
        method("packagedMethod"){
            accessibility(Accessibility.Packaged)
        }
    }.buildType()
    val child = Builder.Any(pack2, "Child", parent = basic).apply {

    }.buildType()
    @Test
    fun assignable_Visibility_Class(){
        assert(basic.isAssignableFrom(child,pack2))
        assert(basic.isAssignableFrom(child,child))
        assert(basic.isAssignableFrom(child,basic))
        assert(child.isAssignableFrom(basic,pack3))
    }

    val traitPrivate = Builder.Trait(loc, "TraitPrivate").apply {
        method("privateMethod"){ }
    }.buildType()

    val traitProtected = Builder.Trait(loc, "TraitProtected").apply {
        method("protectedMethod"){ }
    }.buildType()

    val traitPublic = Builder.Trait(loc, "TraitPublic").apply {
        method("publicMethod"){ }
    }.buildType()

    val traitPackaged = Builder.Trait(loc, "TraitPackaged").apply {
        method("packagedMethod"){ }
    }.buildType()


    val child3 = Builder.Any(pack3, "Child", parent = basic).apply {

    }.buildType()
    @Test
    fun assignable_Visibility_Trait(){
        assert(traitPrivate.isAssignableFrom(basic,basic))
        assert(!traitPrivate.isAssignableFrom(basic,child))
        assert(traitProtected.isAssignableFrom(basic,basic))
        assert(traitProtected.isAssignableFrom(basic,child))
        assert(!traitProtected.isAssignableFrom(child,pack2))
        assert(traitProtected.isAssignableFrom(child,child))
        assert(traitProtected.isAssignableFrom(child3,child))
    }

}