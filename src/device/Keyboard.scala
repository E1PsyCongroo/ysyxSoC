package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import os.read

class PS2IO extends Bundle {
  val clk = Input(Bool())
  val data = Input(Bool())
}

class PS2CtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val ps2 = new PS2IO
}

class ps2_top_apb extends BlackBox {
  val io = IO(new PS2CtrlIO)
}

class ps2Chisel extends Module {
  val io = IO(new PS2CtrlIO)
  assert(!io.in.psel || io.in.paddr === 0x10011000.U)
  val fifo = RegInit(VecInit.fill(128)(0.U(8.W)))

  val buffer = RegInit(0.U(10.W))
  val rPtr = RegInit(0.U(7.W))
  val wPtr = RegInit(0.U(7.W))
  val count = RegInit(0.U(4.W))
  val readPS2 = WireDefault(count =/= 10.U)
  val ps2ClkSync = RegInit(0.U(3.W))

  val bufferEmpty = WireDefault(wPtr === rPtr)
  val bufferOverFlow = RegInit(false.B)
  val sampling = WireDefault(ps2ClkSync(2) & ~ps2ClkSync(1))

  val apbfire = WireDefault(io.in.psel & io.in.penable & io.in.pready)
  val ps2DataValid = WireDefault(!buffer(0) && io.ps2.data && buffer(9, 1).xorR)
  val fifoWriteEnable = WireDefault(sampling && !readPS2 && ps2DataValid)
  rPtr            := Mux(apbfire && !bufferEmpty, rPtr + 1.U, rPtr)
  ps2ClkSync      := ps2ClkSync(1, 0) ## io.ps2.clk;
  count           := Mux(sampling, Mux(readPS2, count + 1.U, 0.U), count)
  buffer          := Mux(sampling && readPS2, io.ps2.data ## buffer >> 1, buffer)
  fifo(wPtr)      := Mux(fifoWriteEnable, buffer(8, 1), fifo(wPtr))
  wPtr            := Mux(fifoWriteEnable, wPtr + 1.U, wPtr)
  bufferOverFlow  := Mux(fifoWriteEnable, bufferOverFlow || (rPtr === wPtr + 1.U), bufferOverFlow)

  io.in.pready := true.B
  io.in.prdata := Mux(bufferEmpty, 0.U, fifo(rPtr))
  io.in.pslverr := bufferOverFlow
}

class APBKeyboard(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val ps2_bundle = IO(new PS2IO)

    val mps2 = Module(new ps2Chisel)
    mps2.io.clock := clock
    mps2.io.reset := reset
    mps2.io.in <> in
    ps2_bundle <> mps2.io.ps2
  }
}
