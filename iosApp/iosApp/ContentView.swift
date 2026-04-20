import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = TimeriOSViewModel()
    @State private var showHistory = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color(UIColor.systemBackground).ignoresSafeArea()

                VStack(spacing: 30) {
                    VStack(spacing: -10) {
                        HStack(alignment: .bottom, spacing: 0) {
                            Text(viewModel.state.formattedTime)
                                .font(.system(size: 80, weight: .thin, design: .monospaced))
                                .foregroundColor(.primary)

                            Text(viewModel.state.formattedMillis)
                                .font(.system(size: 32, weight: .light, design: .monospaced))
                                .foregroundColor(.secondary)
                                .padding(.bottom, 12)
                        }
                    }
                    .padding(.top, 60)

                    HStack(spacing: 20) {
                        Button(action: {
                            viewModel.state.isRunning ? viewModel.pause() : viewModel.start()
                        }) {
                            Text(viewModel.state.isRunning ? "Pause" : "Start")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 60)
                                .background(viewModel.state.isRunning ? Color.orange : Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(16)
                        }

                        Button(action: {
                            viewModel.addLap()
                        }) {
                            Text("Lap")
                                .fontWeight(.medium)
                                .frame(maxWidth: .infinity)
                                .frame(height: 60)
                                .background(Color(UIColor.secondarySystemBackground))
                                .foregroundColor(.primary)
                                .cornerRadius(16)
                        }
                        .disabled(viewModel.state.elapsedMillis == 0)

                        Button(action: {
                            viewModel.reset()
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 20, weight: .bold))
                                .frame(width: 60, height: 60)
                                .background(Color.red.opacity(0.1))
                                .foregroundColor(.red)
                                .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)

                    HStack {
                        Text("LAP TIMES")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                    .padding(.horizontal)
                    .padding(.top, 10)

                    List {
                        ForEach(Array(viewModel.state.laps.enumerated()), id: \.offset) { index, lapTime in
                            HStack {
                                Text("Lap \(viewModel.state.laps.count - index)")
                                    .fontWeight(.medium)
                                Spacer()
                                Text(lapTime)
                                    .font(.system(.body, design: .monospaced))
                                    .foregroundColor(.secondary)
                            }
                            .listRowBackground(Color(UIColor.secondarySystemBackground).opacity(0.5))
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
                }
            }
            .navigationTitle("Timer")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("History") {
                        showHistory = true
                    }
                }
            }
        }
        .sheet(isPresented: $showHistory) {
            HistoryView(
                sessions: viewModel.history,
                onDelete: { id in viewModel.deleteSession(id: id) },
                onClearAll: { viewModel.clearHistory() }
            )
        }
    }
}

struct HistoryView: View {
    let sessions: [LapSession]
    let onDelete: (String) -> Void
    let onClearAll: () -> Void

    var body: some View {
        NavigationStack {
            List {
                if sessions.isEmpty {
                    Text("No sessions yet")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(sessions) { session in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(formatDuration(session.totalElapsedMillis))
                                .font(.headline)

                            Text(formatDate(session.createdAt))
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Text("Laps: \(session.laps.count)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                onDelete(session.id)
                            } label: {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            .navigationTitle("History")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if !sessions.isEmpty {
                        Button("Clear All", role: .destructive) {
                            onClearAll()
                        }
                    }
                }
            }
        }
    }

    private func formatDate(_ timestamp: Double) -> String {
        let date = Date(timeIntervalSince1970: timestamp)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    private func formatDuration(_ millis: Int64) -> String {
        let totalSeconds = millis / 1000
        let minutes = (totalSeconds / 60) % 60
        let seconds = totalSeconds % 60
        let hundredths = (millis % 1000) / 10
        return String(format: "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}
