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

/**
 * Base interface for a visitor of a [SerialDescriptor] tree,
 * which can return an tree processing result.
 *
 * Tree is formed from descriptors using [SerialDescriptor.getElementDescriptor] method.
 */
interface DescriptorTraverser<D> {

    /**
     * Called before iterating elements of [descriptor]
     */
    fun enterDescriptor(descriptor: SerialDescriptor) {}

    /**
     * Called on each element descriptor (child descriptor) of [descriptor].
     * This method helps determine traversal algorithm
     * if it should process descriptors recursively.
     * If it returns `true`, [SerialDescriptor.traverse]
     * would be called on [child], but its result would be discarded.
     * If you need to collect or process results, consider overriding this method with manual child visiting.
     *
     * @return If [child] should be visited.
     */
    fun visitChild(descriptor: SerialDescriptor, child: SerialDescriptor, index: Int): Boolean = true

    /**
     * Called after [visitChild] on each child descriptor was called for computing results.
     *
     * @return Result of subtree processing.
     */
    fun exitDescriptor(desc: SerialDescriptor): D
}

/**
 *  [SerialDescriptor] tree visitor which does not need any result.
 */
interface DescriptorVisitor: DescriptorTraverser<Unit> {
    override fun exitDescriptor(desc: SerialDescriptor) {}
}

/**
 * Traverses [this] with a given [traverser].
 *
 * [DescriptorTraverser.visitChild] determines if traversing is recursive.
 *
 * @see DescriptorTraverser
 * @see SerialDescriptor.getElementDescriptor
 */
fun <R> SerialDescriptor.traverse(traverser: DescriptorTraverser<R>): R {
    traverser.enterDescriptor(this)
    for (i in 0 until elementsCount) {
        val child = getElementDescriptor(i)
        if (traverser.visitChild(this, child, i)) child.traverse(traverser)
    }
    return traverser.exitDescriptor(this)
}

/**
 * Traverses [this] with a given [visitor] which does not need to return a result.
 */
fun SerialDescriptor.accept(visitor: DescriptorVisitor) = traverse(visitor)
