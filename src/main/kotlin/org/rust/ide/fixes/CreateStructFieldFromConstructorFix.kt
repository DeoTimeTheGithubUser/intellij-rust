/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.RsVisibility
import org.rust.lang.core.psi.ext.parentStructLiteral
import org.rust.lang.core.resolve.ref.deepResolve
import org.rust.lang.core.types.infer.TypeVisitor
import org.rust.lang.core.types.regions.ReStatic
import org.rust.lang.core.types.regions.Region
import org.rust.lang.core.types.ty.*
import org.rust.lang.core.types.type

class CreateStructFieldFromConstructorFix private constructor(
    struct: RsStructItem,
    private val fieldName: String,
    @SafeFieldForPreview
    private val fieldType: Ty,
) : LocalQuickFixAndIntentionActionOnPsiElement(struct) {
    override fun getText() = "Create field"

    override fun getFamilyName(): String = text

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val struct = startElement as? RsStructItem ?: return
        val pub = struct.visibility == RsVisibility.Public
        val psiFactory = RsPsiFactory(project)
        val structBlockFields = struct.blockFields
        if (structBlockFields != null) {
            val rBrace = structBlockFields.rbrace ?: return
            ensureTrailingComma(structBlockFields.namedFieldDeclList)
            structBlockFields.addBefore(psiFactory.createBlockFields(pub, fieldName, fieldType).children.first(), rBrace)
            structBlockFields.addBefore(psiFactory.createComma(), rBrace)
        } else {
            val identifier = struct.identifier ?: return
            val blockFields = struct.addAfter(psiFactory.createBlockFields(pub, fieldName, fieldType), identifier) as RsBlockFields
            struct.semicolon?.delete()
            blockFields.addBefore(psiFactory.createComma(), blockFields.rbrace)
        }
    }

    private fun RsPsiFactory.createBlockFields(pub: Boolean, name: String, type: Ty): RsBlockFields {
        return createBlockFields(listOf(RsPsiFactory.BlockField(name, type, pub)))
    }

    companion object {
        fun tryCreate(field: RsStructLiteralField): CreateStructFieldFromConstructorFix? {
            val fieldName = field.identifier?.text ?: return null
            val fieldType = field.type
            if (!canUse(fieldType)) return null
            val struct = field.resolveToStructItem() ?: return null
            if (struct.tupleFields != null) return null
            val structBlockFields = struct.blockFields
            if (structBlockFields != null && structBlockFields.rbrace == null || struct.identifier == null) return null
            return CreateStructFieldFromConstructorFix(struct, fieldName, fieldType)
        }

        private fun RsStructLiteralField.resolveToStructItem(): RsStructItem? {
            return parentStructLiteral.path.reference?.deepResolve() as? RsStructItem
        }

        private fun canUse(ty: Ty): Boolean {
            val result = ty.visitWith(object : TypeVisitor {
                override fun visitTy(ty: Ty): Boolean = when (ty) {
                    is TyUnknown, is TyTypeParameter, is TyAnon, is TyInfer, is TyProjection -> true
                    else -> ty.superVisitWith(this)
                }

                override fun visitRegion(region: Region): Boolean = region !is ReStatic
            })
            return !result
        }
    }
}
