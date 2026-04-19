import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = TimeriOSViewModel()

    var body: some View {
        ZStack {
            // Background color for a modern look
            Color(UIColor.systemBackground).ignoresSafeArea()

            VStack(spacing: 30) {
                // Timer Display Section
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

                // Control Buttons
                HStack(spacing: 20) {
                    // Start/Pause Button
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

                    // Lap Button
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

                    // Reset Button (Icon version for a cleaner look)
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

                // Laps List Header
                HStack {
                    Text("LAP TIMES")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 10)

                // Laps List
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
                .listStyle(InsetGroupedListStyle()) // Gives that classic iOS look
            }
        }
    }
}
