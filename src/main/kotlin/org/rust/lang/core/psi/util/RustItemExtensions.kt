package org.rust.lang.core.psi.util

import org.rust.lang.core.psi.RustFieldDeclElement
import org.rust.lang.core.psi.RustStructItemElement

/**
 *  `RustItemElement` related extensions
 */

val RustStructItemElement.fields: Iterable<RustFieldDeclElement>
    get() = structDeclArgs?.fieldDeclList.orEmpty()
