import SwiftUI
import shared

// MARK: - Root

struct FocusView: View {
    @StateObject private var vm = FocusiOSViewModel()
    @State private var showSettings = false

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.04, blue: 0.09).ignoresSafeArea()
            GalaxyView(state: vm.state, allTimeSecs: vm.allTimeSecs)
            FocusControlsView(
                state      : vm.state,
                onStart    : { vm.start() },
                onPause    : { vm.pause() },
                onReset    : { vm.reset() },
                onSettings : { showSettings = true }
            )
        }
        .sheet(isPresented: $showSettings) {
            SessionSettingsSheet(vm: vm)
        }
    }
}

// MARK: - Settings Sheet

struct SessionSettingsSheet: View {
    @ObservedObject var vm: FocusiOSViewModel
    @Environment(\.dismiss) var dismiss

    @State private var work:       Int = 25
    @State private var shortBreak: Int = 5
    @State private var longBreak:  Int = 15
    @State private var rounds:     Int = 4

    private let purple  = Color(red: 0.73, green: 0.53, blue: 1.0)
    private let bg      = Color(red: 0.05, green: 0.08, blue: 0.15)

    var body: some View {
        NavigationStack {
            Form {
                Section("Focus") {
                    Stepper("Work: \(work) min", value: $work, in: 1...90)
                    Stepper("Rounds: \(rounds)", value: $rounds, in: 1...8)
                }
                Section("Breaks") {
                    Stepper("Short Break: \(shortBreak) min", value: $shortBreak, in: 1...30)
                    Stepper("Long Break: \(longBreak) min",  value: $longBreak,  in: 5...60, step: 5)
                }
                Section {
                    Text("Changes apply after Reset")
                        .font(.caption).foregroundColor(.secondary)
                }
            }
            .navigationTitle("Session Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        vm.updateSettings(shared.FocusSettings(
                            workMinutes:       Int32(work),
                            shortBreakMinutes: Int32(shortBreak),
                            longBreakMinutes:  Int32(longBreak),
                            rounds:            Int32(rounds)
                        ))
                        dismiss()
                    }
                    .foregroundColor(purple)
                }
            }
        }
        .onAppear {
            work       = Int(vm.settings.workMinutes)
            shortBreak = Int(vm.settings.shortBreakMinutes)
            longBreak  = Int(vm.settings.longBreakMinutes)
            rounds     = Int(vm.settings.rounds)
        }
    }
}

// MARK: - Galaxy Canvas

/**
 * Animated space background driven by FocusState + allTimeSecs:
 *   focusSecs      → stars earned (1 per 5 min; white = DB history, purple = live session)
 *   progress (0→1) → progress ring fills
 *   round          → planet grows
 *   isOnBreak      → nebula glow pulses
 */
struct GalaxyView: View {
    let state: shared.FocusState
    let allTimeSecs: Int64

    @State private var animatedProgress: CGFloat = 0
    @State private var animatedPlanetRadius: CGFloat = 44
    @State private var nebulaPulse: CGFloat = 0
    @State private var stars: [StarPoint] = []
    @State private var animatedStarCount: Int = 0

    private let secsPerStar: Int64 = 300
    private let starPool = 200

    private var earnedStars: Int { min(Int((allTimeSecs + state.sessionFocusSecs) / secsPerStar), starPool) }
    private var pastStars: Int   { min(Int(allTimeSecs / secsPerStar), starPool) }

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            drawStars(context: &context, size: size, totalVisible: animatedStarCount, pastCount: pastStars)
            if state.isOnBreak { drawNebula(context: &context, center: center, planetRadius: animatedPlanetRadius, pulse: nebulaPulse) }
            drawPlanet(context: &context, center: center, radius: animatedPlanetRadius)
            drawProgressRing(context: &context, center: center, progress: animatedProgress,
                             planetRadius: animatedPlanetRadius, isOnBreak: state.isOnBreak)
        }
        .ignoresSafeArea()
        .onAppear {
            stars = generateStars(count: starPool, seed: 42)
            animatedStarCount    = earnedStars
            animatedPlanetRadius = 44 + CGFloat(state.round - 1) * 16
            animatedProgress     = CGFloat(state.progress)
            withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true)) { nebulaPulse = 1 }
        }
        .onChange(of: state.progress) { newProgress in
            withAnimation(.easeInOut(duration: 1.2)) { animatedProgress = CGFloat(newProgress) }
        }
        .onChange(of: state.round) { newRound in
            withAnimation(.spring(response: 0.6, dampingFraction: 0.6)) {
                animatedPlanetRadius = 44 + CGFloat(newRound - 1) * 16
            }
        }
        .onChange(of: earnedStars) { newCount in
            withAnimation(.easeOut(duration: 1.5)) { animatedStarCount = newCount }
        }
    }

    private func drawStars(context: inout GraphicsContext, size: CGSize, totalVisible: Int, pastCount: Int) {
        for i in 0..<min(totalVisible, stars.count) {
            let s     = stars[i]
            let isPast = i < pastCount
            let color  = isPast ? Color.white : Color(red: 0.73, green: 0.53, blue: 1.0)
            let alpha  = Double(s.alpha) * (isPast ? 1.0 : 0.8)
            context.fill(Path(ellipseIn: CGRect(x: s.normX * size.width - s.radius,
                y: s.normY * size.height - s.radius, width: s.radius * 2, height: s.radius * 2)),
                with: .color(color.opacity(alpha)))
        }
    }

    private func drawPlanet(context: inout GraphicsContext, center: CGPoint, radius: CGFloat) {
        let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
        context.fill(Path(ellipseIn: rect), with: .radialGradient(
            Gradient(colors: [Color(red: 0.49, green: 0.30, blue: 1.0), Color(red: 0.10, green: 0, blue: 0.31)]),
            center: center, startRadius: 0, endRadius: radius))
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
        let color = isOnBreak ? Color(red: 0, green: 0.74, blue: 0.83) : Color(red: 0.38, green: 0, blue: 0.93)
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

struct StarPoint { let normX, normY, radius, alpha: CGFloat }

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
    let state      : shared.FocusState
    let onStart    : () -> Void
    let onPause    : () -> Void
    let onReset    : () -> Void
    let onSettings : () -> Void

    private let workColor  = Color(red: 0.38, green: 0, blue: 0.93)
    private let breakColor = Color(red: 0, green: 0.74, blue: 0.83)

    var body: some View {
        VStack {
            HStack {
                Spacer()
                VStack(spacing: 12) {
                    Text(state.phaseLabel)
                        .font(.system(size: 13, weight: .light)).tracking(4)
                        .foregroundColor(.white.opacity(0.72))
                    RoundDotsView(current: Int(state.round), total: Int(state.totalRounds))
                }
                Spacer()
                Button(action: onSettings) {
                    Image(systemName: "gearshape")
                        .font(.system(size: 18))
                        .foregroundColor(state.isRunning ? .white.opacity(0.2) : .white.opacity(0.55))
                }
                .disabled(state.isRunning)
            }
            .padding(.top, 88).padding(.horizontal, 24)

            Spacer()

            Text(state.formattedTime)
                .font(.system(size: 84, weight: .thin, design: .monospaced))
                .foregroundColor(.white)

            Spacer()

            VStack(spacing: 14) {
                if state.sessionFocusSecs > 0 {
                    Text("\(state.formattedSessionTime) focused · \(state.pauseCount) pauses")
                        .font(.system(size: 12)).tracking(1)
                        .foregroundColor(.white.opacity(0.45))
                }
                Button(action: { state.isRunning ? onPause() : onStart() }) {
                    Text(state.isRunning ? "PAUSE" : "START")
                        .font(.system(size: 16, weight: .bold)).tracking(2)
                        .frame(maxWidth: .infinity).frame(height: 60)
                        .background(state.isRunning ? breakColor : workColor)
                        .foregroundColor(.white).cornerRadius(16)
                }
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
