import SwiftUI
import shared

// MARK: - Root Screen

struct FocusView: View {
    @StateObject private var vm = FocusiOSViewModel()

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.04, blue: 0.09).ignoresSafeArea()
            GalaxyView(state: vm.state)
            FocusControlsView(
                state   : vm.state,
                onStart : { vm.start() },
                onPause : { vm.pause() },
                onReset : { vm.reset() }
            )
        }
    }
}

// MARK: - Galaxy Canvas

/**
 * Animated space background driven by FocusState:
 *   progress (0→1) → stars appear + ring fills
 *   round          → planet grows, star pattern refreshes
 *   isOnBreak      → nebula glow pulses
 */
struct GalaxyView: View {
    let state: shared.FocusState

    @State private var animatedProgress: CGFloat = 0
    @State private var animatedPlanetRadius: CGFloat = 44
    @State private var nebulaPulse: CGFloat = 0
    @State private var stars: [StarPoint] = []

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            drawStars(context: &context, size: size, progress: animatedProgress)
            if state.isOnBreak {
                drawNebula(context: &context, center: center, planetRadius: animatedPlanetRadius, pulse: nebulaPulse)
            }
            drawPlanet(context: &context, center: center, radius: animatedPlanetRadius)
            drawProgressRing(context: &context, center: center, progress: animatedProgress,
                             planetRadius: animatedPlanetRadius, isOnBreak: state.isOnBreak)
        }
        .ignoresSafeArea()
        .onAppear {
            stars = generateStars(count: 160, seed: Int(state.round))
            animatedProgress     = CGFloat(state.progress)
            animatedPlanetRadius = 44 + CGFloat(state.round - 1) * 16
            withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true)) {
                nebulaPulse = 1
            }
        }
        .onChange(of: state.progress) { newProgress in
            withAnimation(.easeInOut(duration: 1.2)) {
                animatedProgress = CGFloat(newProgress)
            }
        }
        .onChange(of: state.round) { newRound in
            stars = generateStars(count: 160, seed: Int(newRound))
            withAnimation(.spring(response: 0.6, dampingFraction: 0.6)) {
                animatedPlanetRadius = 44 + CGFloat(newRound - 1) * 16
            }
        }
    }

    private func drawStars(context: inout GraphicsContext, size: CGSize, progress: CGFloat) {
        let visible = max(8, Int(progress * CGFloat(stars.count)))
        for i in 0..<min(visible, stars.count) {
            let s     = stars[i]
            let frac  = CGFloat(i) / CGFloat(max(visible, 1))
            let alpha = s.alpha * frac * (0.3 + progress * 0.7)
            let rect  = CGRect(x: s.normX * size.width - s.radius, y: s.normY * size.height - s.radius,
                               width: s.radius * 2, height: s.radius * 2)
            context.fill(Path(ellipseIn: rect), with: .color(Color.white.opacity(alpha)))
        }
    }

    private func drawPlanet(context: inout GraphicsContext, center: CGPoint, radius: CGFloat) {
        let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
        context.fill(Path(ellipseIn: rect), with: .radialGradient(
            Gradient(colors: [Color(red: 0.49, green: 0.30, blue: 1.0), Color(red: 0.10, green: 0.00, blue: 0.31)]),
            center: center, startRadius: 0, endRadius: radius
        ))
    }

    private func drawNebula(context: inout GraphicsContext, center: CGPoint, planetRadius: CGFloat, pulse: CGFloat) {
        let r1 = planetRadius + 28 + pulse * 22
        let r2 = r1 + 24
        context.stroke(Path(ellipseIn: CGRect(x: center.x-r1, y: center.y-r1, width: r1*2, height: r1*2)),
                       with: .color(Color.purple.opacity(0.34)), lineWidth: 10 + pulse * 5)
        context.stroke(Path(ellipseIn: CGRect(x: center.x-r2, y: center.y-r2, width: r2*2, height: r2*2)),
                       with: .color(Color.purple.opacity(0.11)), lineWidth: 28)
    }

    private func drawProgressRing(context: inout GraphicsContext, center: CGPoint,
                                  progress: CGFloat, planetRadius: CGFloat, isOnBreak: Bool) {
        let r     = planetRadius + 18
        let rect  = CGRect(x: center.x-r, y: center.y-r, width: r*2, height: r*2)
        let color = isOnBreak ? Color(red: 0.00, green: 0.74, blue: 0.83) : Color(red: 0.38, green: 0.00, blue: 0.93)
        context.stroke(Path(ellipseIn: rect), with: .color(color.opacity(0.15)), lineWidth: 5)
        if progress > 0 {
            var arc = Path()
            arc.addArc(center: center, radius: r, startAngle: .degrees(-90),
                       endAngle: .degrees(-90 + Double(progress) * 360), clockwise: false)
            context.stroke(arc, with: .color(color), style: StrokeStyle(lineWidth: 5, lineCap: .round))
        }
    }
}

// MARK: - Star helpers

struct StarPoint {
    let normX: CGFloat
    let normY: CGFloat
    let radius: CGFloat
    let alpha: CGFloat
}

private func generateStars(count: Int, seed: Int) -> [StarPoint] {
    var rng = SeededRandom(seed: UInt64(bitPattern: Int64(seed)))
    return (0..<count).map { _ in
        StarPoint(normX: CGFloat(rng.next()), normY: CGFloat(rng.next()),
                  radius: CGFloat(rng.next()) * 1.8 + 0.4, alpha: CGFloat(rng.next()) * 0.65 + 0.35)
    }
}

/** Simple LCG seeded RNG — used only for star positions, not security-sensitive. */
struct SeededRandom {
    private var value: UInt64
    init(seed: UInt64) { value = seed &* 6364136223846793005 &+ 1442695040888963407 }
    mutating func next() -> Float {
        value = value &* 2862933555777941757 &+ 3037000493
        return Float(Double(value >> 11) / Double(1 << 53))
    }
}

// MARK: - Controls

struct FocusControlsView: View {
    let state   : shared.FocusState
    let onStart : () -> Void
    let onPause : () -> Void
    let onReset : () -> Void

    private let workColor  = Color(red: 0.38, green: 0.00, blue: 0.93)
    private let breakColor = Color(red: 0.00, green: 0.74, blue: 0.83)

    var body: some View {
        VStack {
            VStack(spacing: 12) {
                Text(state.phaseLabel)
                    .font(.system(size: 13, weight: .light)).tracking(4)
                    .foregroundColor(.white.opacity(0.72))
                RoundDotsView(current: Int(state.round), total: Int(state.totalRounds))
            }
            .padding(.top, 88)

            Spacer()

            Text(state.isFinished ? "DONE!" : state.formattedTime)
                .font(.system(size: 84, weight: .thin, design: .monospaced))
                .foregroundColor(.white)

            Spacer()

            VStack(spacing: 14) {
                Button(action: { state.isRunning ? onPause() : onStart() }) {
                    Text(state.isFinished ? "Cycle Complete!" : state.isRunning ? "PAUSE" : "START")
                        .font(.system(size: 16, weight: .bold)).tracking(2)
                        .frame(maxWidth: .infinity).frame(height: 60)
                        .background(state.isRunning ? breakColor : workColor)
                        .foregroundColor(.white).cornerRadius(16)
                }
                .disabled(state.isFinished)

                Button(action: onReset) {
                    Text("RESET")
                        .font(.system(size: 15)).tracking(2)
                        .frame(maxWidth: .infinity).frame(height: 52)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.3), lineWidth: 1))
                        .foregroundColor(.white.opacity(0.6))
                }
            }
            .padding(.horizontal, 24).padding(.bottom, 52)
        }
    }
}

struct RoundDotsView: View {
    let current: Int
    let total  : Int
    var body: some View {
        HStack(spacing: 10) {
            ForEach(0..<total, id: \.self) { i in
                Circle()
                    .fill(i < current ? Color(red: 0.73, green: 0.53, blue: 1.0) : Color.white.opacity(0.22))
                    .frame(width: i < current ? 11 : 8, height: i < current ? 11 : 8)
            }
        }
    }
}
