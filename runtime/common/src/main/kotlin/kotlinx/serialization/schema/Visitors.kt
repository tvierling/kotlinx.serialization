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

interface DescriptorTraveller<D> {
    fun enterDescriptor(desc: SerialDescriptor) {}
    fun visitChild(desc: SerialDescriptor, child: SerialDescriptor, index: Int): Boolean = true
    fun exitDescriptor(desc: SerialDescriptor): D
}

interface DescriptorVisitor: DescriptorTraveller<Unit> {
    override fun exitDescriptor(desc: SerialDescriptor) {}
}

fun <R> SerialDescriptor.traverse(traveller: DescriptorTraveller<R>): R {
    traveller.enterDescriptor(this)
    for (i in 0 until elementsCount) {
        val child = getElementDescriptor(i)
        if (traveller.visitChild(this, child, i)) child.traverse(traveller)
    }
    return traveller.exitDescriptor(this)
}

fun SerialDescriptor.accept(visitor: DescriptorVisitor) = traverse(visitor)
