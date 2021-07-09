package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.compiler.reportError
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor

/**Model class used by [ServiceProcessor][com.github.darvld.krpc.compiler.ServiceProcessor] to store information
 * about individual service methods.
 *
 * @see [ServiceDefinition]*/
sealed class ServiceMethodDefinition(
    /**The name of the method as declared in the service interface.*/
    val declaredName: String,
    /**The "official" GRPC name of this method.*/
    val methodName: String,
    /**The name of the method's single argument.*/
    val requestName: String,
    /**Whether the method needs to be marked with the 'suspend' modifier.*/
    val isSuspending: Boolean,
    /**The rpc type of this method.*/
    val methodType: MethodDescriptor.MethodType,
    /**The type of the method's request (parameter).*/
    val requestType: TypeName,
    /**Return type of the method.*/
    val responseType: TypeName
) {

    /**Returns the full gRPC name for this method, consisting of the name of the service and the name of the method itself.*/
    fun qualifiedName(serviceName: String): String {
        return "$serviceName/$methodName"
    }

    companion object {
        /**Checks that this declaration is marked with the 'suspend' modifier.*/
        fun KSFunctionDeclaration.requireSuspending(required: Boolean, message: String) {
            if (required && Modifier.SUSPEND !in modifiers)
                reportError(this, message)
            else if (!required && Modifier.SUSPEND in modifiers)
                reportError(this, message)
        }

        /**Extracts the name of a service method given its declaration and the corresponding [annotation].
         *
         * The method name defined through annotation parameters will be used if present, otherwise the declared
         * name will be used.*/
        fun KSFunctionDeclaration.extractMethodName(annotation: KSAnnotation): String {
            return annotation.arguments.first().value?.toString()?.takeUnless { it.isBlank() }
                ?: simpleName.asString()
        }

        /**Extract the request information for a method declaration, using [resolver] to obtain the desired [TypeName].
         *
         * Returns a pair containing the parameter name and the resolved type, or null if there are no parameters or
         * more than one.*/
        fun <T : TypeName> KSFunctionDeclaration.extractRequestInfo(resolver: (KSTypeReference) -> T?): Pair<String, T>? {
            // For now, only zero or one parameters are allowed
            return parameters.singleOrNull()?.let { param ->
                val resolvedType = resolver(param.type) ?: return null
                param.name!!.asString() to resolvedType
            }
        }
    }
}
