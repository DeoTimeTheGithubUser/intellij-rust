/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir

import org.rust.lang.core.psi.RsPatBinding

@JvmInline
value class LocalVar(val value: RsPatBinding)
