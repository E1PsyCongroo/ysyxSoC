package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class AXI4DelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in    = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
  val out   = new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4))
}

class axi4_delayer extends BlackBox {
  val io = IO(new AXI4DelayerIO)
}

class AXI4DelayerChisel extends Module {
  val io = IO(new AXI4DelayerIO)

  val s:      Int = 32
  val sWidth: Int = log2Ceil(s)
  val r:      Int = (4.7 * s).round.toInt

  val sWriteIdle :: sWriteStart :: sWriteWait :: Nil = Enum(3)

  val writeState   = RegInit(sWriteIdle)
  val isWriteIdle  = writeState === sWriteIdle
  val isWriteStart = writeState === sWriteStart
  val isWriteWait  = writeState === sWriteWait

  val countWrite = RegInit(0.U(64.W))
  countWrite := MuxLookup(writeState, countWrite)(
    Seq(
      sWriteIdle -> Mux(io.out.aw.valid || io.out.w.valid, countWrite + r.U - s.U, 0.U),
      sWriteStart -> Mux(io.out.b.valid, (countWrite >> sWidth.U) - 1.U, countWrite + r.U - s.U),
      sWriteWait -> Mux(countWrite =/= 0.U, countWrite - 1.U, countWrite)
    )
  )

  val brespReg = Reg(chiselTypeOf(io.out.b.bits))
  brespReg := Mux(isWriteStart && io.out.b.valid, io.out.b.bits, brespReg)

  writeState := MuxLookup(writeState, sWriteIdle)(
    Seq(
      sWriteIdle -> Mux(io.in.aw.valid || io.in.w.valid, sWriteStart, sWriteIdle),
      sWriteStart -> Mux(io.out.b.valid, sWriteWait, sWriteStart),
      sWriteWait -> Mux(io.in.b.fire, sWriteIdle, sWriteWait)
    )
  )

  io.out.aw <> io.in.aw
  io.out.w <> io.in.w
  when(isWriteWait && countWrite === 0.U) {
    io.out.b <> io.in.b
  }.otherwise {
    io.in.b.valid := false.B
    io.in.b.bits  := DontCare

    io.out.b.ready := false.B
  }

  val sReadIdle :: sReadReq :: sSendReqWait :: sSendResp :: Nil = Enum(4)

  val readState     = RegInit(sReadIdle)
  val isReadIdle    = readState === sReadIdle
  val isReadReq     = readState === sReadReq
  val isSendReqWait = readState === sSendReqWait
  val isSendResp    = readState === sSendResp

  val delayReadReq = RegInit(0.U(64.W))
  delayReadReq := MuxLookup(readState, delayReadReq)(
    Seq(
      sReadIdle -> Mux(io.out.ar.valid, delayReadReq + r.U - s.U, 0.U),
      sReadReq -> Mux(io.out.ar.fire, (delayReadReq >> sWidth.U) - 1.U, delayReadReq + r.U - s.U),
      sSendReqWait -> Mux(delayReadReq =/= 0.U, delayReadReq - 1.U, delayReadReq)
    )
  )

  val rlast = RegInit(false.B)
  rlast := Mux(isSendResp, Mux(io.out.r.fire, io.out.r.bits.last, rlast), 0.U)
  val countReadResp = RegInit(0.U(8.W))
  countReadResp := Mux(isSendResp, Mux(io.out.r.fire, countReadResp + 1.U, countReadResp), 0.U)
  val delayReadResp = RegInit(VecInit.fill(16)(0.U(64.W)))
  for (i <- 0 until 16) {
    delayReadResp(i) := MuxLookup(readState, 0.U)(
      Seq(
        sReadIdle -> Mux(io.out.ar.fire, delayReadResp(i) + r.U - s.U, 0.U),
        sSendReqWait -> Mux(delayReadReq === 0.U, delayReadResp(i) + r.U - s.U, 0.U),
        sSendResp -> MuxCase(
          delayReadResp(i),
          Seq(
            (delayReadResp(i) === 0.U) -> 0.U,
            (i.U === countReadResp && io.out.r.fire) -> ((delayReadResp(i) >> sWidth.U) - 1.U),
            (i.U >= countReadResp) -> (delayReadResp(i) + r.U - s.U),
            (i.U < countReadResp) -> (delayReadResp(i) - 1.U)
          )
        )
      )
    )
  }
  val rrespReg = Reg(Vec(16, chiselTypeOf(io.out.r.bits)))
  rrespReg(countReadResp) := Mux(io.out.r.fire, io.out.r.bits, rrespReg(countReadResp))
  val curResp = RegInit(0.U(8.W))
  curResp := Mux(isSendResp, Mux(delayReadResp(curResp) === 0.U, curResp + 1.U, curResp), 0.U)

  readState := MuxLookup(readState, sReadIdle)(
    Seq(
      sReadIdle -> MuxCase(
        sReadIdle,
        Seq(
          io.out.ar.fire -> sSendResp,
          io.out.ar.valid -> sReadReq
        )
      ),
      sReadReq -> Mux(io.out.ar.fire, sSendReqWait, sReadReq),
      sSendReqWait -> Mux(delayReadReq === 0.U, sSendResp, sSendReqWait),
      sSendResp -> Mux((curResp === countReadResp) && rlast, sReadIdle, sSendResp)
    )
  )

  io.out.ar <> io.in.ar
  io.out.r.ready := isSendResp

  io.in.r.valid := isSendResp && (delayReadResp(curResp) === 0.U) && !((curResp === countReadResp) && rlast)
  io.in.r.bits  := rrespReg(curResp)
}

class AXI4DelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = AXI4IdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in.zip(node.out)).foreach {
      case ((in, edgeIn), (out, edgeOut)) =>
        val delayer = Module(new AXI4DelayerChisel)
        delayer.io.clock := clock
        delayer.io.reset := reset
        delayer.io.in <> in
        out <> delayer.io.out
    }
  }
}

object AXI4Delayer {
  def apply()(implicit p: Parameters): AXI4Node = {
    val axi4delay = LazyModule(new AXI4DelayerWrapper)
    axi4delay.node
  }
}
