package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import dataclass.data

class VGAIO extends Bundle {
  val r     = Output(UInt(8.W))
  val g     = Output(UInt(8.W))
  val b     = Output(UInt(8.W))
  val hsync = Output(Bool())
  val vsync = Output(Bool())
  val valid = Output(Bool())
}

class VGACtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(
    new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32))
  )
  val vga = new VGAIO
}

class vga_top_apb extends BlackBox {
  val io = IO(new VGACtrlIO)
}

case class VGAControlConfig(
  val hFrontPorch: Int,
  val hActive:     Int,
  val hResolution: Int,
  val hBackPorch:  Int,
  val vFrontPorch: Int,
  val vActive:     Int,
  val vResolution: Int,
  val vBackPorch:  Int) {

  val hFrontPorchEnd: Int = hFrontPorch
  val hShowStart:     Int = hFrontPorch + hActive
  val hShowEnd:       Int = hFrontPorch + hActive + hResolution
  val hTotal:         Int = hFrontPorch + hActive + hResolution + hBackPorch

  val vFrontPorchEnd: Int = vFrontPorch
  val vShowStart:     Int = vFrontPorch + vActive
  val vShowEnd:       Int = vFrontPorch + vActive + vResolution
  val vTotal:         Int = vFrontPorch + vActive + vResolution + vBackPorch
}

class VGAControlIO(vgaConfig: VGAControlConfig) extends Bundle {
  val vgaData = Input(UInt(24.W))
  val haddr   = Output(UInt(log2Ceil(vgaConfig.hResolution).W))
  val vaddr   = Output(UInt(log2Ceil(vgaConfig.vResolution).W))
  val hsync   = Output(Bool())
  val vsync   = Output(Bool())
  val valid   = Output(Bool())
  val vgaR    = Output(UInt(8.W))
  val vgaG    = Output(UInt(8.W))
  val vgaB    = Output(UInt(8.W))
}

class VGAControl(vgaConfig: VGAControlConfig) extends Module {
  val io = IO(new VGAControlIO(vgaConfig))

  val xCount = RegInit(1.U(log2Ceil(vgaConfig.hTotal).W))
  val yCount = RegInit(1.U(log2Ceil(vgaConfig.vTotal).W))
  val hValid = Wire(Bool())
  val vValid = Wire(Bool())

  xCount := Mux(xCount === vgaConfig.hTotal.U, 1.U, xCount + 1.U)
  yCount := Mux(
    xCount === vgaConfig.hTotal.U,
    Mux(yCount === vgaConfig.vTotal.U, 1.U, yCount + 1.U),
    yCount
  )
  hValid := (xCount > vgaConfig.hShowStart.U) & (xCount <= vgaConfig.hShowEnd.U)
  vValid := (yCount > vgaConfig.vShowStart.U) & (yCount <= vgaConfig.vShowEnd.U)

  io.valid := hValid & vValid
  io.hsync := (xCount > vgaConfig.hFrontPorchEnd.U)
  io.vsync := (yCount > vgaConfig.vFrontPorchEnd.U)
  io.haddr := Mux(hValid, (xCount - vgaConfig.hShowStart.U - 1.U), 0.U)
  io.vaddr := Mux(vValid, (yCount - vgaConfig.vShowStart.U - 1.U), 0.U)
  io.vgaR  := io.vgaData(23, 16)
  io.vgaG  := io.vgaData(15, 8)
  io.vgaB  := io.vgaData(7, 0)
}

class ForwardingMemory(addrWidth: Int, dataWidth: Int) extends Module {
  assert((dataWidth % 8) == 0)
  val io = IO(new Bundle {
    val rdAddr = Input(UInt(addrWidth.W))
    val rdData = Output(Vec(dataWidth / 8, UInt(8.W)))
    val wrAddr = Input(UInt(addrWidth.W))
    val wrData = Input(Vec(dataWidth / 8, UInt(8.W)))
    val wrMask = Input(Vec(dataWidth / 8, Bool()))
    val wrEna  = Input(Bool())
  })

  val mem          = Mem(1 << addrWidth, Vec(dataWidth / 8, UInt(8.W)))
  val doForwardReg = WireDefault(io.wrAddr === io.rdAddr && io.wrEna)
  val memData      = mem.read(io.rdAddr)
  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData, io.wrMask)
  }

  io.rdData := Mux(doForwardReg, io.wrData, memData)
}

class vgaChisel extends Module {
  val io = IO(new VGACtrlIO)

  val vgaContol = Module(
    new VGAControl(
      VGAControlConfig(
        hFrontPorch = 96,
        hActive     = 48,
        hResolution = 640,
        hBackPorch  = 16,
        vFrontPorch = 2,
        vActive     = 33,
        vResolution = 480,
        vBackPorch  = 10
      )
    )
  )
  val forwardingMem = Module(new ForwardingMemory(log2Ceil(640 * 480), 24))

  forwardingMem.io.rdAddr := vgaContol.io.vaddr * 640.U + vgaContol.io.haddr
  forwardingMem.io.wrAddr := io.in.paddr >> 2
  for (i <- 0 to 2) {
    forwardingMem.io.wrData(i) := io.in.pwdata(i * 8 + 7, i * 8)
    forwardingMem.io.wrMask(i) := io.in.pstrb(i)
  }
  forwardingMem.io.wrEna := io.in.pwrite && io.in.penable

  vgaContol.io.vgaData := forwardingMem.io.rdData.asUInt

  io.in.prdata  := forwardingMem.io.rdAddr.asUInt
  io.in.pready  := true.B
  io.in.pslverr := false.B

  io.vga.r     := vgaContol.io.vgaR
  io.vga.g     := vgaContol.io.vgaG
  io.vga.b     := vgaContol.io.vgaB
  io.vga.hsync := vgaContol.io.hsync
  io.vga.vsync := vgaContol.io.vsync
  io.vga.valid := vgaContol.io.valid
}

class APBVGA(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(
    Seq(
      APBSlavePortParameters(
        Seq(
          APBSlaveParameters(
            address       = address,
            executable    = true,
            supportsRead  = true,
            supportsWrite = true
          )
        ),
        beatBytes = 4
      )
    )
  )

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _)    = node.in(0)
    val vga_bundle = IO(new VGAIO)

    val mvga = Module(new vgaChisel)
    mvga.io.clock := clock
    mvga.io.reset := reset
    mvga.io.in <> in
    vga_bundle <> mvga.io.vga
  }
}
