/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints

import org.rust.*
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.ide.inspections.RsInspectionsTestBase

@UseNewResolve
class RsUnusedImportInspectionTest : RsInspectionsTestBase(RsUnusedImportInspection::class) {
    fun `test unused import`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>
        }
    """)

    fun `test unused import in group`() = checkByText("""
        mod foo {
            pub struct S;
            pub struct T;
        }

        mod bar {
            use super::foo::{S, <warning descr="Unused import: `T`">T</warning>};

            fn bar(_: S) {}
        }
    """)

    fun `test annotate whole unused group`() = checkByText("""
        mod foo {
            pub struct S;
            pub struct T1;
            pub struct T2;
        }

        mod bar {
            use super::foo::{S, <warning descr="Unused import: `{T1, T2}`">{T1, T2}</warning>};

            fn bar(_: S) {}
        }
    """)

    fun `test annotate whole unused use item`() = checkByText("""
        mod foo {
            pub struct S;
            pub struct T1;
            pub struct T2;
        }

        mod bar {
            <warning descr="Unused import: `super::foo::{S, {T1, T2}}`">use super::foo::{S, {T1, T2}};</warning>
        }
    """)

    fun `test unused import with nested path in group`() = checkByText("""
        mod foo {
            pub struct R;
            pub mod bar {
                pub struct S;
            }
        }

        mod bar {
            use super::foo::{R, <warning descr="Unused import: `bar::S`">bar::S</warning>};

            fn bar(_: R) {}
        }
    """)

    fun `test import used in type context`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            use super::foo::S;

            fn bar(_: S) {}
        }
    """)

    fun `test import used in expr context`() = checkByText("""
        mod foo {
            pub struct S;
            impl S {
                pub fn foo() {}
            }
        }

        mod bar {
            use super::foo::S;

            fn bar() {
                S::foo();
            }
        }
    """)

    fun `test ignore reexport`() = checkByText("""
        pub mod foo {
            pub struct S {}
        }

        pub use foo::S;
        pub(crate) use foo::S;
    """)

    fun `test shadowed path`() = checkByText("""
        mod foo {
            pub struct S {}
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>
            fn bar() {
                let S = 1;
                let x = S;
            }
        }
    """)

    fun `test path with global import`() = checkByText("""
        mod foo {
            pub struct S {}
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>
            fn bar(s: crate::foo::S) {}
        }
    """)

    fun `test unused multi-resolve import`() = checkByText("""
        mod foo {
            pub const S: u32 = 1;
            pub struct S {}
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>
        }
    """)

    fun `test used multi-resolve import`() = checkByText("""
        mod foo {
            pub const S: u32 = 1;
            pub struct S {}
        }

        mod bar {
            use super::foo::S;

            fn bar(_: S) {}
        }
    """)

    @MockAdditionalCfgOptions("intellij_rust")
    fun `test cfg-disabled usage`() = checkByText("""
        mod foo {
            pub struct S {}
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>

            #[cfg(not(intellij_rust))]
            fn bar() {
                let s: S;
            }
        }
    """)

    fun `test used trait method`() = checkByText("""
        mod foo {
            pub trait Trait {
                fn method(&self);
            }
        }

        mod bar {
            use super::foo::Trait;

            struct S;
            impl super::foo::Trait for S {
                fn method(&self) {}
            }

            fn bar(s: S) {
                s.method();
            }
        }
    """)

    fun `test used trait associated constant`() = checkByText("""
        mod foo {
            pub trait Trait {
                const FOO: u32;
            }
        }

        mod bar {
            use super::foo::Trait;

            struct S;
            impl super::foo::Trait for S {
                const FOO: u32 = 1;
            }

            fn bar() {
                let _ = S::FOO;
            }
        }
    """)

    fun `test used trait method with alias`() = checkByText("""
        mod foo {
            pub trait Trait {
                fn method(&self);
            }
        }

        mod bar {
            use super::foo::Trait as T;

            struct S;
            impl super::foo::Trait for S {
                fn method(&self) {}
            }

            fn bar(s: S) {
                s.method();
            }
        }
    """)

    fun `test used trait method with type qualifier`() = checkByText("""
        mod foo {
            pub trait Foo {
                fn func() {}
            }
            impl<T> Foo for T {}
        }

        mod bar {
            use crate::foo::Foo;
            fn main() {
                <()>::func();
            }
        }
    """)

    fun `test unused import with alias`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S as T`">use super::foo::S as T;</warning>

            fn bar(_: S) {}
        }
    """)

    fun `test used import with alias`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            use super::foo::S as T;

            fn bar(_: T) {}
        }
    """)

    fun `test unused self import`() = checkByText("""
        mod foo {
            pub mod bar {
                pub struct S;
            }
        }

        mod bar {
            <warning descr="Unused import: `super::foo::bar::{self}`">use super::foo::bar::{self};</warning>
        }
    """)

    fun `test used self import`() = checkByText("""
        mod foo {
            pub mod baz {
                pub struct S;
            }
        }

        mod bar {
            use super::foo::baz::{self};

            fn bar(_: baz::S) {}
        }
    """)

    fun `test unused star import`() = checkByText("""
        mod foo {}

        mod bar {
            <warning descr="Unused import: `super::foo::*`">use super::foo::*;</warning>
        }
    """)

    fun `test used star import`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            use super::foo::*;

            fn bar(_: S) {}
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test star import reexport`() = checkByText("""
        mod foo {
            mod bar {
                pub struct S;
            }
            pub use bar::S as T;
        }

        mod bar {
            use super::foo::*;

            fn bar(_: T) {}
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test star import star reexport`() = checkByText("""
        mod foo {
            mod bar {
                pub struct S;
            }
            pub use bar::*;
        }

        mod bar {
            use super::foo::*;

            fn bar(_: S) {}
        }
    """)

    fun `test used star import in group`() = checkByText("""
        mod foo {
            pub mod bar {
                pub struct S;
            }
        }

        mod bar {
            use super::foo::{bar::*};

            fn bar(_: S) {}
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test private import used in same module`() = checkByText("""
       mod foo {
            mod bar {
                pub struct S;
            }
            mod baz {
                use super::bar::S;
                use S as T;

                fn fun(_: T) {}
            }
        }
    """)

    fun `test unused import in function`() = checkByText("""
        mod foo {
            pub struct S;
        }

        fn bar() {
            <warning descr="Unused import: `foo::S`">use foo::S;</warning>
        }
    """)

    fun `test used import in function`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            fn bar() {
                use super::foo::S;
                let _: S;
            }
        }
    """)

    fun `test usage inside macro`() = checkByText("""
        mod foo {
            pub struct S;
        }
        mod bar {
            use super::foo::S;

            macro_rules! handle {
                (${"$"}p:path) => {
                    let _: ${"$"}p;
                }
            }

            fn bar() {
                handle!(S);
            }
        }
    """)

    fun `test deny lint`() = checkByText("""
        #![deny(unused_imports)]

        mod foo {
            pub struct S;
        }

        mod bar {
            <error descr="Unused import: `super::foo::S`">use super::foo::S;</error>
        }
    """)

    fun `test deny lint group`() = checkByText("""
        #![deny(unused)]

        mod foo {
            pub struct S;
        }

        mod bar {
            <error descr="Unused import: `super::foo::S`">use super::foo::S;</error>
        }
    """)

    fun `test empty group`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            <warning descr="Unused import: `super::foo::{}`">use super::foo::{};</warning>
        }
    """)

    fun `test unresolved import`() = checkByText("""
        mod bar {}

        mod baz {
            use foo::S;
            use super::bar::T;
        }
    """)

    fun `test use imported item in use speck 1`() = checkByText("""
        mod bar {
            pub fn func() {}
        }
        mod foo {
            use crate::bar;
            use bar::func;

            fn main() {
                func();
            }
        }
    """)

    fun `test use imported item in use speck 2`() = checkByText("""
        mod bar {
            pub fn func() {}
        }
        mod foo {
            use crate::bar;
            use bar::{func};

            fn main() {
                func();
            }
        }
    """)

    // TODO: https://github.com/intellij-rust/intellij-rust/issues/7314 needs to be fixed
    /*@MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test colon colon import`() = checkByText("""
        mod foo {
            use ::{std::collections};
            use collections::HashMap;

            fn bar(_: HashMap<u32, u32>) {}
        }
    """)*/

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test use imported item in use speck with alias`() = checkByText("""
        mod bar {
            pub fn foo1() {}
            pub fn foo2() {}
        }
        mod foo {
            use crate::bar::{foo1, foo2};
            use {foo1 as bar1, foo2 as bar2};

            fn foo() {
                bar1();
                bar2();
            }
        }
    """)

    fun `test use imported item in pat ident`() = checkByText("""
        enum A { A1, A2(u32) }

        mod bar {
            use crate::A;
            use crate::A::A1;
            use crate::A::A2;

            fn main(a: A) {
                match a {
                    A1 => {}
                    A2(_) => {}
                }
            }
        }
    """)

    fun `test star import name imported multiple times`() = checkByText("""
        mod mod1 {
            pub fn foo() {}
        }
        mod mod2 {
            pub use crate::mod1::foo;
            pub fn bar() {}
        }
        mod test {
            <warning descr="Unused import: `crate::mod2::*`">use crate::mod2::*;</warning>
            use crate::mod1::foo as bar;

            fn baz() {
                bar();
            }
        }
    """)

    fun `test used macro`() = checkByText("""
        mod foo {
            #[macro_export]
            macro_rules! mac { () => {} }
        }

        mod bar {
            use crate::mac;
            fn baz() {
                mac!();
            }
        }
    """)

    fun `test unused macro`() = checkByText("""
        mod foo {
            #[macro_export]
            macro_rules! mac { () => {} }
        }

        mod bar {
            fn baz() {
                <warning descr="Unused import: `crate::mac`">use crate::mac;</warning>
            }
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test used qualified macro`() = checkByText("""
        mod foo {
            #[macro_export]
            macro_rules! gen_ {
                () => {};
            }
            pub use gen_ as gen;
        }

        mod bar {
            use crate::foo;
            fn main() {
                foo::gen!();
            }
        }
    """)

    fun `test unresolved use`() = checkByText("""
        mod bar {
            use super::foo::Baz;

            fn baz(x: Baz) {}
        }
    """)

    /*fun `test redundant use speck`() = checkByText("""
        mod foo {
            pub struct S;
        }

        mod bar {
            <warning descr="Unused import: `super::foo::S`">use super::foo::S;</warning>

            fn bar() {
                use super::foo::S;
                let _: S;
            }
        }
    """)*/

    /*
    fun `test ignore unresolved usage in child module`() = checkByText("""
        mod foo {
            pub struct S {}
        }

        <warning descr="Unused import: `foo::S`">use foo::S;</warning>
        mod bar {
            fn foo() {
                let x: S;
            }
        }
    """)*/

    /*fun `test ignore resolved usage in child module`() = checkByText("""
        mod foo {
            pub struct S {}
        }

        <warning descr="Unused import: `foo::S`">use foo::S;</warning>
        mod bar {
            use super::foo::S;

            fn foo() {
                let x: S;
            }
        }
    """)*/

    /*@MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test private import used in child module`() = checkByText("""
       mod foo {
            mod bar {
                pub struct S;
            }
            use bar::S;

            mod baz {
                use super::S;
                fn fun(_: S) {}
            }
        }
    """)*/

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test writeln macro`() = checkByText("""
        use std::io::Write;
        fn main() {
            let mut w = Vec::new();
            writeln!(&mut w, "test").unwrap();
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test println macro`() = checkByText("""
        use std::collections::HashSet;
        fn main() {
            println!("{:?}", HashSet::new());
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test format macro`() = checkByText("""
        use std::collections::HashSet;
        fn main() {
            format!("{:?}", HashSet::new());
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test format macro with trait method call`() = checkByText("""
        struct S;
        trait Trait {
            fn method(&self) {}
        }
        impl Trait for S {}

        mod inner {
            use crate::Trait;
            fn foo(s: crate::S) {
                format!("{:?}", s.method());
            }
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @MockAdditionalCfgOptions("intellij_rust")
    fun `test cfg-disabled function`() = checkByText("""
        mod inner {
            pub fn func() {}
        }
        mod test {
            use crate::inner;
            #[cfg(not(intellij_rust))]
            fn foo1() {}
            fn foo2() {
                inner::func();
            }
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    @MockAdditionalCfgOptions("intellij_rust")
    fun `test cfg-disabled method`() = checkByText("""
        mod inner {
            pub fn func() {}
        }
        mod test {
            use crate::inner;
            struct Struct {}
            impl Struct {
                #[cfg(not(intellij_rust))]
                fn foo1() {}
                fn foo2() {
                    inner::func();
                }
            }
        }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test disabled in doctests`() = checkByText("""
        /// ```
        /// use test_project::func;
        /// ```
        pub fn func() {}
    """)
}
