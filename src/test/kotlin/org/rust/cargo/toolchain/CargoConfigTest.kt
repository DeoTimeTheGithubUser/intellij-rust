/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain

import org.intellij.lang.annotations.Language
import org.rust.MinRustcVersion
import org.rust.cargo.CargoConfig
import org.rust.cargo.RsWithToolchainTestBase
import org.rust.cargo.project.model.cargoProjects
import org.rust.singleWorkspace

@MinRustcVersion("1.53.0")
class CargoConfigTest : RsWithToolchainTestBase() {

    fun `test default config`() {
        buildProject {
            file("Cargo.toml", CARGO_TOML)
            dir("src") { file("main.rs") }
        }

        val cargoConfig = project.cargoProjects.singleWorkspace().cargoConfig

        assertEquals(CargoConfig.DEFAULT, cargoConfig)
    }

    fun `test build target`() {
        buildProject {
            dir(".cargo") {
                toml("config.toml", """
                    [build]
                    target = "wasm32-unknown-unknown"
                """)
            }
            file("Cargo.toml", CARGO_TOML)
            dir("src") { file("main.rs") }
        }

        val buildTarget = project.cargoProjects.singleWorkspace().cargoConfig.buildTarget

        assertEquals("wasm32-unknown-unknown", buildTarget)
    }

    fun `test custom build target`() {
        val testProject = buildProject {
            dir(".cargo") {
                toml("config", """
                    [build]
                    target = "custom-target.json"
                """)
            }
            file("custom-target.json", """
                {
                    "llvm-target": "aarch64-unknown-none",
                    "data-layout": "e-m:e-i64:64-f80:128-n8:16:32:64-S128",
                    "arch": "aarch64",
                    "target-endian": "little",
                    "target-pointer-width": "64",
                    "target-c-int-width": "32",
                    "os": "none",
                    "executables": true,
                    "linker-flavor": "ld.lld",
                    "linker": "rust-lld",
                    "panic-strategy": "abort",
                    "disable-redzone": true,
                    "features": "-mmx,-sse,+soft-float"
                }
            """)
            file("Cargo.toml", CARGO_TOML)
            dir("src") { file("main.rs") }
        }

        val buildTarget = project.cargoProjects.singleWorkspace().cargoConfig.buildTarget

        assertEquals(testProject.root.findChild("custom-target.json")!!.path, buildTarget)
    }

    fun `test env`() {
        buildProject {
            dir(".cargo") {
                toml("config.toml", """
                    [env]
                    foo = "42"
                    bar = { value = "24", force = true }
                    baz = { value = "hello/world", relative = true }
                    qwe = { value = "value", unknown_field = 123 }
                """)
            }
            file("Cargo.toml", CARGO_TOML)
            dir("src") { file("main.rs") }
        }

        val env = project.cargoProjects.singleWorkspace().cargoConfig.env

        assertEquals(CargoConfig.EnvValue("42"), env["foo"])
        assertEquals(CargoConfig.EnvValue("24", isForced = true), env["bar"])
        assertEquals(CargoConfig.EnvValue("hello/world", isRelative = true), env["baz"])
        assertEquals(CargoConfig.EnvValue("value"), env["qwe"])
    }

    companion object {
        @Language("TOML")
        private val CARGO_TOML = """
            [package]
            name = "foo"
            version = "0.1.0"
        """.trimIndent()
    }
}
