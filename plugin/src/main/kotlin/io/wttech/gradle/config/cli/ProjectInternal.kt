package io.wttech.gradle.config.cli

import org.gradle.api.Project

class ProjectInternal(val project: Project) {

    val serviceFactory: Any get() = invoke(project, "getServices")

    inline fun <reified T : Any> getService(): T = invoke(serviceFactory, "get", T::class.java) as T

    @Suppress("SpreadOperator")
    operator fun invoke(obj: Any, method: String, vararg args: Any): Any {
        val argumentTypes = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            argumentTypes[i] = args[i].javaClass
        }
        return obj.javaClass.getMethod(method, *argumentTypes).run {
            isAccessible = true
            invoke(obj, *args)
        }
    }

    @Suppress("SpreadOperator")
    fun invoke(obj: Any, method: String, args: List<Any?>, argTypes: List<Class<out Any>>): Any {
        return obj.javaClass.getMethod(method, *argTypes.toTypedArray()).run {
            isAccessible = true
            invoke(obj, *args.toTypedArray())
        }
    }
}

inline fun <reified T : Any> Project.getService() = ProjectInternal(this).getService<T>()
