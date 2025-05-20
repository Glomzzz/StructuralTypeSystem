# Structural TypeSystem
> 这是一拍脑袋就写出来的项目（没有深思熟虑

初版Kant是这样的：
- 跑在JVM上的
- structural type
- staging+部分求值（partial evaluation）
- 带JIT

但和朋友聊了一晚上，并且认真分析Kant应该是什么样才能投入实际使用后，我决定重新设计Kant。

这是原Kant的类型系统，是在 **Java类型系统** 上强制塞入了 **Static duck typing**

> 我知道这两者很不兼容, 我也没有继续写下去，去解决运行时的各种问题。

此项目适合只抄走java类型兼容部分然后自己造一套与java（maybe 单方面）兼容的类型系统（？

无论如何，希望能帮到你。

