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
    assert(!(isFlashAccess && in.pwrite))

    val sIdle :: sAccessSPI :: sAccessTx :: sAccessDivider :: sAccessSs :: sAccessCtrl :: sWaitData :: sSendBack :: Nil = Enum(8)
    val state = RegInit(sIdle)
    state := MuxLookup(state, sIdle)(Seq(
      sIdle           -> MuxCase(sIdle, Seq(
        (in.psel && isSPIAccess)    -> sAccessSPI,
        (in.psel && isFlashAccess)  -> sAccessTx,
      )),
      sAccessSPI      -> Mux(fire, sIdle, sAccessSPI),
      sAccessTx       -> Mux(fire, sAccessDivider, sAccessTx),
      sAccessDivider  -> Mux(fire, sAccessSs, sAccessDivider),
      sAccessSs       -> Mux(fire, sAccessCtrl, sAccessSs),
      sAccessCtrl     -> Mux(fire, sWaitData, sAccessCtrl),
      sWaitData       -> Mux(fire && !mspi.io.in.prdata(8), sSendBack, sWaitData),
      sSendBack       -> Mux(fire, sIdle, sSendBack),
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
    SPIReq.psel     := Mux(isIdle, false.B, in.psel)
    SPIReq.penable  := Mux(isIdle, false.B, in.penable)
    SPIReq.pwrite   := MuxCase(in.pwrite, Seq(
      isAccessTx      -> true.B,
      isAccessDivider -> true.B,
      isAccessSs      -> true.B,
      isAccessCtrl    -> true.B,
      isWaitData      -> false.B,
      isSendBack      -> false.B,
    ))
    SPIReq.paddr    := MuxCase(in.paddr, Seq(
      isAccessTx      -> "h1000_1004".U,
      isAccessDivider -> "h1000_1014".U,
      isAccessSs      -> "h1000_1018".U,
      isAccessCtrl    -> "h1000_1010".U,
      isWaitData      -> "h1000_1010".U,
      isSendBack      -> "h1000_1000".U,
    ))
    SPIReq.pprot    := in.pprot
    SPIReq.pwdata   := MuxCase(in.pwdata, Seq(
      isAccessTx      -> "h03".U(8.W) ## in.paddr(23, 0),
      isAccessDivider -> "h0000_0001".U,
      isAccessSs      -> "h0000_0001".U,
      isAccessCtrl    -> "h0000_2540".U,
    ))
    SPIReq.pstrb    := MuxCase(in.pstrb, Seq(
      isAccessTx      -> "b1111".U,
      isAccessDivider -> "b0011".U,
      isAccessSs      -> "b0001".U,
      isAccessCtrl    -> "b1111".U,
    ))
    SPIReq.pauser   := DontCare

    SPIReq.pready   := Mux(isAccessSPI || isSendBack, mspi.io.in.pready, false.B)
    SPIReq.pslverr  := mspi.io.in.pslverr
    val littleEndianData = mspi.io.in.prdata(7, 0) ## mspi.io.in.prdata(15, 8) ## mspi.io.in.prdata(23, 16) ## mspi.io.in.prdata(31, 24)
    SPIReq.prdata   := Mux(isSendBack, littleEndianData, mspi.io.in.prdata)
    SPIReq.pduser   := DontCare

    mspi.io.in.psel     := SPIReq.psel
    mspi.io.in.penable  := SPIReq.penable

    mspi.io.in.pwrite   := SPIReq.pwrite
    mspi.io.in.paddr    := SPIReq.paddr
    mspi.io.in.pprot    := SPIReq.pprot
    mspi.io.in.pwdata   := SPIReq.pwdata
    mspi.io.in.pstrb    := SPIReq.pstrb

    in.pready           := SPIReq.pready
    in.pslverr          := SPIReq.pslverr
    in.prdata           := SPIReq.prdata

    spi_bundle <> mspi.io.spi
  }
}
