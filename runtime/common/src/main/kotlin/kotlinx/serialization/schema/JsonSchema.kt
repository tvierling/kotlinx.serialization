/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.schema

import kotlinx.serialization.*
import kotlinx.serialization.json.*

internal val SerialDescriptor.jsonType
    get() = when (this.kind) {
        StructureKind.LIST -> "array"
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG,
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
        PrimitiveKind.STRING, PrimitiveKind.CHAR -> "string"
        PrimitiveKind.BOOLEAN -> "boolean"
        else -> "object"
    }

class JsonSchemaCreator : DescriptorTraverser<JsonObject> {
    private val properties: MutableMap<String, JsonObject> = mutableMapOf()
    private val requiredProperties: MutableSet<String> = mutableSetOf()

    override fun visitChild(descriptor: SerialDescriptor, child: SerialDescriptor, index: Int): Boolean {
        val elementName = descriptor.getElementName(index)
        properties[elementName] = child.traverse(JsonSchemaCreator())
        if (!descriptor.isElementOptional(index)) requiredProperties.add(elementName)
        return false // don't traverse automatically
    }

    override fun exitDescriptor(desc: SerialDescriptor): JsonObject {
        val jsonType = desc.jsonType
        val objectData: MutableMap<String, JsonElement> = mutableMapOf(
            "description" to JsonLiteral(desc.name),
            "type" to JsonLiteral(jsonType)
        )
        when (jsonType) {
            "object" -> {
                objectData["properties"] = JsonObject(properties)
                objectData["required"] = JsonArray(requiredProperties.map { JsonLiteral(it) })
            }
            "array" -> objectData["items"] = properties.values.let { check(it.size == 1); it.first() }
            else -> { /* no-op */ }
        }
        return JsonObject(objectData)
    }

    companion object {
        fun createSchema(descriptor: SerialDescriptor) = descriptor.traverse(JsonSchemaCreator())
        fun <T> createSchema(serializer: KSerializer<T>) = serializer.descriptor.traverse(JsonSchemaCreator())

        @ImplicitReflectionSerializer
        inline fun <reified T: Any> createSchema() = T::class.serializer().descriptor.traverse(JsonSchemaCreator())
    }
}
