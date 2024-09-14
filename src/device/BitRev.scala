package ysyx

import chisel3._
import chisel3.util._
import os.isFile

class bitrev extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

class bitrevChisel extends RawModule { // we do not need clock and reset
  val io = IO(Flipped(new SPIIO(1)))

withClockAndReset(io.sck.asBool.asClock, io.ss.asBool.asAsyncReset) {
  val shiftReg  = RegInit(0.U(8.W))
  val bitRevReg = WireDefault(0.U(8.W))
  val count     = RegInit(0.U(4.W))
  count := count + 1.U

  shiftReg  := Mux(!count(3), io.mosi ## shiftReg(7, 1), shiftReg)
  bitRevReg := Reverse(shiftReg)
  io.miso   := Mux(count(3), bitRevReg(count(2, 0)), true.B)
}
}
