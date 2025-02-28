/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas

import org.rust.lang.core.psi.ext.RsElement

interface MirBody {
    val sourceElement: RsElement
    val basicBlocks: List<MirBasicBlock>
    val localDecls: List<MirLocal>
    val span: MirSpan
    val sourceScopes: List<MirSourceScope>
    val argCount: Int
    val varDebugInfo: List<MirVarDebugInfo>

    val startBlock: MirBasicBlock get() = basicBlocks.first()
    val outermostScope: MirSourceScope get() = sourceScopes.first()
    val returnLocal: MirLocal get() = localDecls.first()

    val sourceScopesTree: Map<MirSourceScope, List<MirSourceScope>>
        get() = buildMap<MirSourceScope, MutableList<MirSourceScope>> {
            sourceScopes.forEach { scope ->
                scope.parentScope?.let { parent ->
                    val children = getOrPut(parent) { mutableListOf() }
                    children.add(scope)
                }
            }
        }

    fun returnPlace(): MirLocal = localDecls.first()

    fun alwaysStorageLiveLocals(): Set<MirLocal> {
        return localDecls.toMutableSet().apply {
            basicBlocks.forEach { block ->
                block.statements.forEach { statement ->
                    when (statement) {
                        is MirStatement.StorageDead -> remove(statement.local)
                        is MirStatement.StorageLive -> remove(statement.local)
                        else -> Unit
                    }
                }
            }
        }
    }
}
