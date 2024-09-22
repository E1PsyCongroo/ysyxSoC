package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class GPIOIO extends Bundle {
  val out = Output(UInt(16.W))
  val in = Input(UInt(16.W))
  val seg = Output(Vec(8, UInt(8.W)))
}

class GPIOCtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val gpio = new GPIOIO
}

class gpio_top_apb extends BlackBox {
  val io = IO(new GPIOCtrlIO)
}

class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val sw  = Input(UInt(4.W))
    val seg = Output(UInt(8.W))
  })

  val sevSeg = WireDefault(0.U(8.W))
  val sevSegNums = VecInit(
    Seq(
      "b11111101".U(8.W), // 0
      "b01100000".U(8.W), // 1
      "b11011010".U(8.W), // 2
      "b11110010".U(8.W), // 3
      "b01100110".U(8.W), // 4
      "b10110110".U(8.W), // 5
      "b10111110".U(8.W), // 6
      "b11100000".U(8.W), // 7
      "b11111110".U(8.W), // 8
      "b11100110".U(8.W), // 9
      "b11101110".U(8.W), // A
      "b11111110".U(8.W), // B
      "b10011100".U(8.W), // C
      "b11111100".U(8.W), // D
      "b10011110".U(8.W), // E
      "b10001110".U(8.W), // F
    )
  )
  sevSeg := sevSegNums(io.sw)
  io.seg := ~sevSeg
}

class gpioChisel extends Module {
  val io = IO(new GPIOCtrlIO)

  val isWrite     = io.in.psel && io.in.pwrite
  val isLed       = io.in.paddr === "h10002000".U
  val isButton    = io.in.paddr === "h10002004".U
  val isSeg       = io.in.paddr === "h10002008".U
  val isReserve   = io.in.paddr === "h1000200C".U
  val invalid     = !(isLed || isButton || isSeg)

  val ledsReg     = RegInit(VecInit(Seq.fill(2)(0.U(8.W))))
  val segsReg     = RegInit(VecInit(Seq.fill(8)(0.U(4.W))))
  val segDecoders = Seq.fill(8)(Module(new SevenSegDecoder))

  for (i <- 0 until 4) {
    if (i < 2) {
      ledsReg(i)    := Mux(isWrite && isLed && io.in.pstrb(i), io.in.pwdata(7+8*i, 8*i), ledsReg(i))
    }
    segsReg(i*2)    := Mux(isWrite && isSeg && io.in.pstrb(i), io.in.pwdata(3+8*i, 8*i), segsReg(2*i))
    segsReg(1+i*2)  := Mux(isWrite && isSeg && io.in.pstrb(i), io.in.pwdata(7+8*i, 4+8*i), segsReg(1+2*i))
  }
  for (i <- 0 until 8) {
    segDecoders(i).io.sw  := segsReg(i)
    io.gpio.seg(i)        := segDecoders(i).io.seg
  }
  io.gpio.out     := ledsReg.asUInt

  io.in.pready    := io.in.psel && io.in.penable
  io.in.pslverr   := io.in.pready && (invalid || (isButton && isWrite))
  io.in.prdata    := MuxCase(0.U, Seq(
    isLed     -> (0.U(16.W) ## ledsReg.asUInt),
    isButton  -> (0.U(16.W) ## io.gpio.in),
    isSeg     -> segsReg.asUInt,
  ))
}

class APBGPIO(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val gpio_bundle = IO(new GPIOIO)

    val mgpio = Module(new gpioChisel)
    mgpio.io.clock := clock
    mgpio.io.reset := reset
    mgpio.io.in <> in
    gpio_bundle <> mgpio.io.gpio
  }
}
