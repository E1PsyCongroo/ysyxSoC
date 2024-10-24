package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class QSPIIO extends Bundle {
  val sck = Output(Bool())
  val ce_n = Output(Bool())
  val dio = Analog(4.W)
}

class psram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val qspi = new QSPIIO
  })
}

class psram extends BlackBox {
  val io = IO(Flipped(new QSPIIO))
}

class psramHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val raddr = Input(UInt(32.W))
    val ren   = Input(Bool())
    val rdata = Output(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wlen  = Input(UInt(32.W))
    val iswrite   = Input(Bool())
  })
  setInline("psramHelper.v",
    """module psramHelper(
      |  input clock,
      |  input [31:0] raddr,
      |  input ren,
      |  output reg [31:0] rdata,
      |  input [31:0] waddr,
      |  input [31:0] wdata,
      |  input [31:0] wlen,
      |  input iswrite
      |);
      |import "DPI-C" function void psram_read(input int raddr, output int rdata);
      |import "DPI-C" function void psram_write(input int waddr, input int wdata, input int wlen);
      |reg [95:0] save;
      |always @(negedge clock) begin
      |  if (ren) psram_read(raddr, rdata);
      |  else rdata = 0;
      |  save <= {waddr, wdata, wlen};
      |end
      |always @(negedge iswrite) begin
      |  psram_write(save[95:64], save[63:32], save[31:0]);
      |end
      |endmodule
    """.stripMargin)
}

class qpiHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val enterQPI  = Input(Bool())
    val quitQPI   = Input(Bool())
    val modeOut   = Output(Bool())
  })
  setInline("qpiHelper.v",
    """module qpiHelper(
      |  input enterQPI,
      |  input quitQPI,
      |  output reg modeOut
      |);
      |initial begin
      |   modeOut = 1'b0;
      |end
      |always_latch @(*) begin
      |   if (enterQPI) begin
      |     modeOut = 1'b1;
      |   end else if (quitQPI) begin
      |     modeOut = 1'b0;
      |   end
      |end
      |endmodule
    """.stripMargin)
}

class psramChisel extends RawModule {
  val io = IO(Flipped(new QSPIIO))
withClockAndReset(io.sck.asClock, io.ce_n.asAsyncReset) {
  val sReadInst :: sReadAddr :: sWriteData :: sReadData :: sSendData :: Nil = Enum(5)

  val state = RegInit(sReadInst)
  val isReadInst  = state === sReadInst
  val isReadAddr  = state === sReadAddr
  val isWriteData = state === sWriteData
  val isReadData  = state === sReadData
  val isSendData  = state === sSendData

  val qpi = Module(new qpiHelper)
  val qpiMode = qpi.io.modeOut
  val count   = RegInit(0.U(3.W))
  val readInstEnd   = isReadInst && Mux(qpiMode, count === 1.U, count === 7.U)
  val readAddrEnd   = isReadAddr && count === 5.U
  val writeDataEnd  = isWriteData && count === 7.U
  val readDataEnd   = isReadData && count === 6.U
  val sendDataEnd   = isSendData && count === 7.U
  val inst    = RegInit(0.U(8.W))
  val isRead  = inst === "hEB".U
  val isWrite = inst === "h38".U
  val isQPI   = inst === "h35".U
  val isQQPI  = inst === "hF5".U
  val addr    = RegInit(0.U(24.W))
  val data    = RegInit(0.U(32.W))
  val wlen    = RegInit(0.U(3.W))
  val dout    = Mux(count(0), data(3, 0), data(7, 4))
  val di      = TriStateInBuf(io.dio, dout, isSendData)
  val psram   = Module(new psramHelper)

  state := MuxLookup(state, sReadAddr)(Seq(
    sReadInst   -> Mux((readInstEnd && !(isQPI || isQQPI)), sReadAddr, sReadInst),
    sReadAddr   -> MuxCase(sReadAddr, Seq(
      (isWrite && readAddrEnd)  -> sWriteData,
      (isRead && readAddrEnd)   -> sReadData,
    )),
    sWriteData  -> Mux(writeDataEnd, sReadInst, sWriteData),
    sReadData   -> Mux(readDataEnd, sSendData, sReadData),
    sSendData   -> Mux(sendDataEnd, sReadInst, sSendData),
  ))

  psram.io.clock    := io.sck.asClock
  psram.io.raddr    := addr
  psram.io.ren      := isReadData && readDataEnd
  psram.io.waddr    := addr
  psram.io.wdata    := data >> ((4.U - wlen) << 3)
  psram.io.wlen     := wlen
  psram.io.iswrite  := isWrite

  count       := Mux(readInstEnd || readAddrEnd || writeDataEnd || readDataEnd || sendDataEnd, 0.U, count + 1.U)

  qpi.io.enterQPI := isQPI
  qpi.io.quitQPI  := isQQPI
  inst  := Mux(isReadInst, Mux(qpiMode, inst(3, 0) ## di, inst(6, 0) ## di(0)), inst)
  addr  := Mux(isReadAddr, addr(19, 0) ## di, addr)
  data  := MuxCase(data, Seq(
    (isWriteData && !count(0))  -> di ## data(31, 4),
    (isWriteData && count(0))   -> data(31, 28) ## di ## data(27, 4),
    isReadData                  -> psram.io.rdata,
    (isSendData && count(0))    -> 0.U(8.W) ## data(31, 8),
  ))
  wlen  := Mux(isWriteData, wlen + count(0), wlen)
}
}

class APBPSRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val qspi_bundle = IO(new QSPIIO)

    val mpsram = Module(new psram_top_apb)
    mpsram.io.clock := clock
    mpsram.io.reset := reset
    mpsram.io.in <> in
    qspi_bundle <> mpsram.io.qspi
  }
}
