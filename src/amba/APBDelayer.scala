package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class APBDelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in    = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val out   = new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32))
}

class apb_delayer extends BlackBox {
  val io = IO(new APBDelayerIO)
}

class APBDelayerChisel extends Module {
  val io = IO(new APBDelayerIO)

  val s:      Int = 32
  val sWidth: Int = log2Ceil(s)
  val r:      Int = (5.6 * s).round.toInt

  val sIdle :: sWait :: Nil = Enum(2)

  val state  = RegInit(sIdle)
  val isIdle = state === sIdle
  val isWait = state === sWait

  val count = RegInit(0.U(64.W))
  count := MuxCase(
    count,
    Seq(
      (isWait && count =/= 0.U) -> (count - 1.U),
      (isIdle && io.out.penable && io.out.pready) -> (count >> sWidth.U),
      (isIdle && io.out.psel) -> (count + r.U - s.U)
    )
  )
  val prdataReg = RegInit(0.U(32.W))
  prdataReg := Mux(isIdle && io.out.penable && io.out.pready, io.out.prdata, prdataReg)
  val pslverrReg = RegInit(false.B)
  pslverrReg := Mux(isIdle && io.out.penable && io.out.pready, io.out.pslverr, pslverrReg)

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.out.penable && io.out.pready, sWait, sIdle),
      sWait -> Mux(count === 0.U, sIdle, sWait)
    )
  )

  io.out <> io.in
  io.out.psel    := Mux(isIdle, io.in.psel, false.B)
  io.out.penable := Mux(isIdle, io.in.penable, false.B)
  io.in.pready   := Mux(isWait && count === 0.U, true.B, false.B)
  io.in.prdata   := prdataReg
  io.in.pslverr  := pslverrReg
}

class APBDelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = APBIdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in.zip(node.out)).foreach {
      case ((in, edgeIn), (out, edgeOut)) =>
        val delayer = Module(new APBDelayerChisel)
        delayer.io.clock := clock
        delayer.io.reset := reset
        delayer.io.in <> in
        out <> delayer.io.out
    }
  }
}

object APBDelayer {
  def apply()(implicit p: Parameters): APBNode = {
    val apbdelay = LazyModule(new APBDelayerWrapper)
    apbdelay.node
  }
}
