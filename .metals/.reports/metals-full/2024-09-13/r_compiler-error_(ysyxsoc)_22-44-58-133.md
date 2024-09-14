file://<WORKSPACE>/src/device/SPI.scala
### java.lang.StringIndexOutOfBoundsException: offset 1461, count -8, length 3521

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.13.10
Classpath:
<WORKSPACE>/.bloop/out/ysyxsoc/bloop-bsp-clients-classes/classes-Metals-Ud4fgE3dSUWMr-_-hnUy1g== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.10.0/semanticdb-javac-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-M1/chisel_2.13-7.0.0-M1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/mainargs_2.13/0.5.4/mainargs_2.13-0.5.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson_2.13/4.0.6/json4s-jackson_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.10/scala-reflect-2.13.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/jcazevedo/moultingyaml_2.13/0.4.2/moultingyaml_2.13-0.4.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.6/json4s-native_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.9.1/os-lib_2.13-0.9.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.1.0/upickle_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.0/firtool-resolver_2.13-2.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.6/json4s-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson-core_2.13/4.0.6/json4s-jackson-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/nscala-time/nscala-time_2.13/2.22.0/nscala-time_2.13-2.22.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/yaml/snakeyaml/1.26/snakeyaml-1.26.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.6/json4s-native-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.0.0/geny_2.13-1.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.1.0/ujson_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.1.0/upack_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.1.0/upickle-implicits_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.6/json4s-ast_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.6/json4s-scalap_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.7/jackson-databind-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/joda-time/joda-time/2.10.1/joda-time-2.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/joda/joda-convert/2.2.0/joda-convert-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.1.0/upickle-core_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.7/jackson-core-2.12.7.jar [exists ], <WORKSPACE>/.bloop/out/rocketchip/bloop-bsp-clients-classes/classes-Metals-Ud4fgE3dSUWMr-_-hnUy1g== [exists ], <WORKSPACE>/.bloop/out/rocketchip.cde/bloop-bsp-clients-classes/classes-Metals-Ud4fgE3dSUWMr-_-hnUy1g== [exists ], <WORKSPACE>/.bloop/out/rocketchip.hardfloat/bloop-bsp-clients-classes/classes-Metals-Ud4fgE3dSUWMr-_-hnUy1g== [exists ], <WORKSPACE>/.bloop/out/rocketchip.macros/bloop-bsp-clients-classes/classes-Metals-Ud4fgE3dSUWMr-_-hnUy1g== [exists ]
Options:
-language:reflectiveCalls -Ymacro-annotations -Ytasty-reader -Yrangepos -Xplugin-require:semanticdb


action parameters:
offset: 1462
uri: file://<WORKSPACE>/src/device/SPI.scala
text:
```scala
package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class SPIIO(val ssWidth: Int = 8) extends Bundle {
  val sck = Output(Bool())
  val ss = Output(UInt(ssWidth.W))
  val mosi = Output(Bool())
  val miso = Input(Bool())
}

class spi_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val spi = new SPIIO
    val spi_irq_out = Output(Bool())
  })
}

class flash extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

class APBSPI(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(Seq(APBSlavePortParameters(
    Seq(APBSlaveParameters(
      address       = address,
      executable    = true,
      supportsRead  = true,
      supportsWrite = true)),
    beatBytes  = 4)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val spi_bundle = IO(new SPIIO)

    val mspi = Module(new spi_top_apb)
    mspi.io.clock := clock
    mspi.io.reset := reset

    val fire          = mspi.io.in.penable && mspi.io.in.pready
    val isSPIAccess   = address(0).contains(in.paddr)
    val isFlashAccess = address(1).contains(in.paddr)
    assert(is@@)

    val sIdle :: sAccessSPI :: sAccessTx :: sAccessDivider :: sAccessSs :: sAccessCtrl :: sWaitData :: sSendBack :: Nil = Enum(8)
    val state = RegInit(sIdle)
    state := MuxLookup(state, sIdle)(Seq(
      sIdle         -> MuxCase(sIdle, Seq(
        (in.psel && isSPIAccess)    -> sAccessSPI,
        (in.psel && isFlashAccess)  -> sAccessFlash,
      )),
      sAccessSPI    -> Mux(fire, sIdle, sAccessSPI),
      sAccessFlash  -> Mux(fire, sIdle, sAccessFlash),
    ))

    val isIdle          = state === sIdle
    val isAccessSPI     = state === sAccessSPI
    val isAccessTx      = state === sAccessTx
    val isAccessDivider = state === sAccessDivider
    val isAccessSs      = state === sAccessSs
    val isAccessCtrl    = state === sAccessCtrl
    val isWaitData      = state === sWaitData
    val isSendBack      = state === sSendBack

    val SPIReq = Wire(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    SPIReq.psel     := Mux(isIdle || isSendBack, false.B, in.psel)
    SPIReq.penable  := Mux(isIdle || isSendBack, false.B, in.psel)
    SPIReq.pwrite   := Mux(isWaitData || isSendBack, false.B, in.pwrite)
    SPIReq.paddr    := MuxCase(in.paddr, Seq(
      isAccessTx      -> "h1000_1004".U,
      isAccessDivider -> "h1000_1014".U,
      isAccessSs      -> "h1000_1018".U,
      isAccessCtrl    -> "h1000_1010".U,
      isWaitData      -> "h1000_1010".U,
    ))
    SPIReq.pprot    := in.psel
    SPIReq.pwdata   := MuxCase(in.pwdata, Seq(
      isAccessTx      -> "h1000_1004".U,
      isAccessDivider -> "h1000_1014".U,
      isAccessSs      -> "h1000_1018".U,
      isAccessCtrl    -> "h1000_1010".U,
    ))
    SPIReq.pstrb    := Output(UInt((params.dataBits/8).W))
    SPIReq.pauser   := BundleMap(params.requestFields)

    SPIReq.pready   := Mux(isIdle, false.B, mspi.io.in.pready)
    SPIReq.pslverr  := Mux(isIdle,)
    SPIReq.prdata   := Input(UInt(params.dataBits.W))
    SPIReq.pduser   := BundleMap(params.responseFields)


    SPIReq <> mspi.io.in
    SPIReq <> in

    spi_bundle <> mspi.io.spi
  }
}

```



#### Error stacktrace:

```
java.base/java.lang.String.checkBoundsOffCount(String.java:4591)
	java.base/java.lang.String.rangeCheck(String.java:304)
	java.base/java.lang.String.<init>(String.java:300)
	scala.tools.nsc.interactive.Global.typeCompletions$1(Global.scala:1244)
	scala.tools.nsc.interactive.Global.completionsAt(Global.scala:1282)
	scala.meta.internal.pc.SignatureHelpProvider.$anonfun$treeSymbol$1(SignatureHelpProvider.scala:390)
	scala.Option.map(Option.scala:242)
	scala.meta.internal.pc.SignatureHelpProvider.treeSymbol(SignatureHelpProvider.scala:388)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCall$.unapply(SignatureHelpProvider.scala:205)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.visit(SignatureHelpProvider.scala:316)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.traverse(SignatureHelpProvider.scala:310)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.fromTree(SignatureHelpProvider.scala:279)
	scala.meta.internal.pc.SignatureHelpProvider.signatureHelp(SignatureHelpProvider.scala:27)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$signatureHelp$1(ScalaPresentationCompiler.scala:332)
```
#### Short summary: 

java.lang.StringIndexOutOfBoundsException: offset 1461, count -8, length 3521