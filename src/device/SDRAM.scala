package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class SDRAMIO extends Bundle {
  val clk = Output(Bool())
  val cke = Output(Bool())
  val cs  = Output(Bool())
  val ras = Output(Bool())
  val cas = Output(Bool())
  val we  = Output(Bool())
  val a   = Output(UInt(13.W))
  val ba  = Output(UInt(2.W))
  val dqm = Output(UInt(2.W))
  val dq  = Analog(16.W)
}

class sdram_top_axi extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
    val sdram = new SDRAMIO
  })
}

class sdram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val sdram = new SDRAMIO
  })
}

class sdram extends BlackBox {
  val io = IO(Flipped(new SDRAMIO))
}

class sdramHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val raddr = Input(UInt(32.W))
    val ren   = Input(Bool())
    val rdata = Output(UInt(16.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(16.W))
    val dqm   = Input(UInt(2.W))
    val wen   = Input(Bool())
  })
  setInline("sdramHelper.v",
    """module sdramHelper(
      |  input clock,
      |  input [31:0] raddr,
      |  input ren,
      |  output reg [15:0] rdata,
      |  input [31:0] waddr,
      |  input [15:0] wdata,
      |  input [1:0] dqm,
      |  input wen
      |);
      |import "DPI-C" function void sdram_read(input int raddr, output shortint rdata);
      |import "DPI-C" function void sdram_write(input int waddr, input shortint wdata, input byte dqm);
      |always @(negedge clock) begin
      |  if (ren) sdram_read(raddr, rdata);
      |  else rdata = 0;
      |  if (wen) sdram_write(waddr, wdata, {6'b0, dqm});
      |end
      |endmodule
    """.stripMargin)
}

class sdramChisel extends RawModule {
  val io = IO(Flipped(new SDRAMIO))
  val clk = io.cke & io.clk
withClockAndReset(clk.asClock, io.cs) {
  val cmd   = io.ras ## io.cas ## io.we
  val cmdA  = cmd === BitPat("b011")
  val cmdR  = cmd === BitPat("b101")
  val cmdW  = cmd === BitPat("b100")
  val cmdBT = cmd === BitPat("b110")
  val cmdM  = cmd === BitPat("b000")

  val mode        = RegInit(0.U(13.W))
  val burstLength = mode(2, 0)
  val casLatency  = mode(6, 4)
  val bankAddr    = RegInit(0.U(2.W))
  val rowAddr     = RegInit(0.U(13.W))
  val colAddr     = RegInit(0.U(9.W))
  val dqmReg      = RegInit(0.U(2.W))
  val casReg      = RegInit(0.U(3.W))
  val burstCount  = RegInit(0.U(3.W))
  val sdram       = Module(new sdramHelper)
  val outEn       = RegNext(sdram.io.ren)
  val di          = RegNext(TriStateInBuf(io.dq, RegNext(sdram.io.rdata), outEn))

  val casAfter    = casReg(casLatency - 2.U)  // -2.U instead of -1.U to fit control bug
  val burstEnd    = burstCount === (1.U >> burstLength) - 1.U
  val burstTerm   = cmdBT
  val addr        = bankAddr ## rowAddr ## colAddr

  val sIdle :: sWaitCAS :: sReadBurst :: sWriteBurst :: Nil = Enum(4)

  val state         = RegInit(sIdle)
  val isIdle        = state === sIdle
  val isWaitCAS     = state === sWaitCAS
  val isReadBurst   = state === sReadBurst
  val isWriteBurst  = state === sWriteBurst

  state := MuxLookup(state, sIdle)(Seq(
    sIdle   -> MuxCase(sIdle, Seq(
      cmdR  -> sWaitCAS,
      cmdW  -> sWriteBurst,
    )),
    sWaitCAS    -> Mux(casAfter, sReadBurst, sWaitCAS),
    sReadBurst  -> Mux(burstEnd || burstTerm, sIdle, sReadBurst),
    sWriteBurst -> Mux(burstEnd || burstTerm, sIdle, sWriteBurst),
  ))

  mode            := Mux(cmdM, io.a, mode)
  bankAddr        := Mux(cmdA, io.ba, bankAddr)
  rowAddr         := Mux(cmdA, io.a, rowAddr)
  colAddr         := Mux(cmdR || cmdW, io.a(8, 0), colAddr)
  dqmReg          := Mux((cmdR || cmdW) || isWriteBurst, io.dqm, dqmReg)
  casReg          := casReg(1, 0) ## cmdR
  burstCount      := MuxCase(0.U, Seq(
    casAfter                      -> 1.U,
    cmdW                          -> 0.U,
    (isReadBurst || isWriteBurst) -> (burstCount + 1.U),
  ))
  sdram.io.clock  := clk.asClock
  sdram.io.raddr  := addr(23, 3) ## (addr(2, 0) + burstCount)
  sdram.io.ren    := casAfter || isReadBurst
  sdram.io.waddr  := addr(23, 3) ## (addr(2, 0) + burstCount)
  sdram.io.wdata  := di
  sdram.io.dqm    := dqmReg
  sdram.io.wen    := isWriteBurst

  assert(!(!isIdle && (cmdM || cmdA)))
}
}

class AXI4SDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
        address       = address,
        executable    = true,
        supportsWrite = TransferSizes(1, beatBytes),
        supportsRead  = TransferSizes(1, beatBytes),
        interleavedId = Some(0))
    ),
    beatBytes  = beatBytes)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_axi)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}

class APBSDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_apb)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}
