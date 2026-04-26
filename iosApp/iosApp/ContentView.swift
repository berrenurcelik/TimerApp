import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = TimeriOSViewModel()
    @State private var showHistory = false

    private let bgColor     = Color(red: 0.02, green: 0.04, blue: 0.09)
    private let accentWork  = Color(red: 0.38, green: 0.00, blue: 0.93)
    private let accentPause = Color(red: 0.00, green: 0.74, blue: 0.83)
    private let surfaceDim  = Color(red: 0.05, green: 0.08, blue: 0.15)
    private let textDim     = Color(red: 0.53, green: 0.57, blue: 0.66)

    var body: some View {
        ZStack {
            bgColor.ignoresSafeArea()

            VStack(spacing: 0) {

                // ── Timer ring + display ──────────────────────────────────────
                ZStack {
                    TimerRingView(isRunning: viewModel.state.isRunning)
                    VStack(spacing: 4) {
                        HStack(alignment: .bottom, spacing: 2) {
                            Text(viewModel.state.formattedTime)
                                .font(.system(size: 62, weight: .thin, design: .monospaced))
                                .foregroundColor(.white)
                            Text(viewModel.state.formattedMillis)
                                .font(.system(size: 24, weight: .light, design: .monospaced))
                                .foregroundColor(.white.opacity(0.5))
                                .padding(.bottom, 10)
                        }
                        if !viewModel.state.laps.isEmpty {
                            Text("LAP \(viewModel.state.laps.count)")
                                .font(.system(size: 11, weight: .medium))
                                .tracking(3)
                                .foregroundColor(accentPause.opacity(0.85))
                        }
                    }
                }
                .frame(width: 260, height: 260)
                .padding(.top, 72)

                Spacer().frame(height: 32)

                // ── Controls ─────────────────────────────────────────────────
                HStack(spacing: 12) {
                    Button(action: {
                        viewModel.state.isRunning ? viewModel.pause() : viewModel.start()
                    }) {
                        Text(viewModel.state.isRunning ? "PAUSE" : "START")
                            .font(.system(size: 15, weight: .bold)).tracking(2)
                            .frame(maxWidth: .infinity).frame(height: 58)
                            .background(viewModel.state.isRunning ? accentPause : accentWork)
                            .foregroundColor(.white).cornerRadius(16)
                    }

                    Button(action: { viewModel.addLap() }) {
                        Text("LAP")
                            .font(.system(size: 15, weight: .medium)).tracking(2)
                            .frame(maxWidth: .infinity).frame(height: 58)
                            .overlay(RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.white.opacity(0.22), lineWidth: 1))
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .disabled(viewModel.state.elapsedMillis == 0)

                    Button(action: { viewModel.reset() }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 18, weight: .medium))
                            .frame(width: 58, height: 58)
                            .background(Color.white.opacity(0.07))
                            .foregroundColor(.white.opacity(0.55))
                            .cornerRadius(16)
                    }

                    Button(action: { showHistory = true }) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 18, weight: .medium))
                            .frame(width: 58, height: 58)
                            .background(accentWork.opacity(0.16))
                            .foregroundColor(accentWork)
                            .cornerRadius(16)
                    }
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 28)

                // ── Lap list ─────────────────────────────────────────────────
                if !viewModel.state.laps.isEmpty {
                    HStack {
                        Text("LAP").font(.system(size: 11)).tracking(3).foregroundColor(textDim)
                        Spacer()
                        Text("TIME").font(.system(size: 11)).tracking(3).foregroundColor(textDim)
                    }
                    .padding(.horizontal, 24).padding(.bottom, 8)

                    ScrollView {
                        VStack(spacing: 6) {
                            ForEach(Array(viewModel.state.laps.enumerated()), id: \.offset) { index, lapTime in
                                let lapNumber = viewModel.state.laps.count - index
                                let isLatest  = index == 0
                                HStack {
                                    Text("Lap \(lapNumber)")
                                        .font(.system(size: 14, weight: isLatest ? .semibold : .regular))
                                        .foregroundColor(isLatest ? .white : .white.opacity(0.65))
                                    Spacer()
                                    Text(lapTime)
                                        .font(.system(size: 14, design: .monospaced))
                                        .foregroundColor(isLatest ? accentWork : .white.opacity(0.5))
                                }
                                .padding(.horizontal, 16).padding(.vertical, 12)
                                .background(
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(isLatest ? accentWork.opacity(0.12) : surfaceDim)
                                )
                            }
                        }
                        .padding(.horizontal, 24)
                    }
                }

                Spacer()
            }
        }
        .sheet(isPresented: $showHistory) {
            HistoryView(
                sessions:   viewModel.history,
                onDelete:   { id in viewModel.deleteSession(id: id) },
                onClearAll: { viewModel.clearHistory() }
            )
        }
    }
}

// Decorative double ring — pulses when timer is running
struct TimerRingView: View {
    let isRunning: Bool
    @State private var pulse: CGFloat = 0

    var body: some View {
        Canvas { context, size in
            let c  = CGPoint(x: size.width / 2, y: size.height / 2)
            let r1 = min(size.width, size.height) / 2 - 8
            let r2 = r1 - 12
            let a  = isRunning ? 0.18 + pulse * 0.14 : 0.07
            let purple = Color(red: 0.38, green: 0.0, blue: 0.93)
            context.stroke(Path(ellipseIn: CGRect(x: c.x-r1, y: c.y-r1, width: r1*2, height: r1*2)),
                           with: .color(purple.opacity(a)), lineWidth: 1.5)
            context.stroke(Path(ellipseIn: CGRect(x: c.x-r2, y: c.y-r2, width: r2*2, height: r2*2)),
                           with: .color(purple.opacity(a * 0.45)), lineWidth: 0.7)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                pulse = 1
            }
        }
    }
}

// MARK: - History Sheet

struct HistoryView: View {
    let sessions:   [LapSession]
    let onDelete:   (String) -> Void
    let onClearAll: () -> Void

    private let bgColor  = Color(red: 0.02, green: 0.04, blue: 0.09)
    private let rowColor = Color(red: 0.05, green: 0.08, blue: 0.15)
    private let textDim  = Color(red: 0.53, green: 0.57, blue: 0.66)

    var body: some View {
        NavigationStack {
            ZStack {
                bgColor.ignoresSafeArea()
                if sessions.isEmpty {
                    Text("No sessions yet").foregroundColor(textDim)
                } else {
                    List {
                        ForEach(sessions) { session in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(formatDuration(session.totalElapsedMillis))
                                    .font(.system(size: 17, weight: .medium, design: .monospaced))
                                    .foregroundColor(.white)
                                Text(formatDate(session.createdAt))
                                    .font(.caption).foregroundColor(textDim)
                                if !session.laps.isEmpty {
                                    Text("\(session.laps.count) laps")
                                        .font(.caption).foregroundColor(textDim)
                                }
                            }
                            .padding(.vertical, 4)
                            .listRowBackground(rowColor)
                            .swipeActions {
                                Button(role: .destructive) { onDelete(session.id) } label: {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if !sessions.isEmpty {
                        Button("Clear All", role: .destructive) { onClearAll() }
                            .foregroundColor(Color(red: 0.81, green: 0.4, blue: 0.47))
                    }
                }
            }
        }
    }

    private func formatDate(_ timestamp: Double) -> String {
        let f = DateFormatter()
        f.dateStyle = .medium; f.timeStyle = .short
        return f.string(from: Date(timeIntervalSince1970: timestamp))
    }

    private func formatDuration(_ millis: Int64) -> String {
        let s = millis / 1000
        return String(format: "%02d:%02d.%02d", (s / 60) % 60, s % 60, (millis % 1000) / 10)
    }
}
