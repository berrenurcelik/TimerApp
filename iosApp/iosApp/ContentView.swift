import SwiftUI
import shared

struct ContentView: View {
    /**
     * @StateObject ensures the ViewModel's lifecycle is tied to the View.
     * It acts as the bridge connecting our Kotlin Multiplatform (KMP)
     * shared logic to the native iOS UI.
     */
    @StateObject private var viewModel = TimeriOSViewModel()

    var body: some View {
        VStack(spacing: 30) {
            
            /** * Main Time Display
             * Using an HStack to separate main units and fractional milliseconds.
             */
            HStack(alignment: .bottom, spacing: 0) {
                // Main clock display (MM:SS) fetched from the shared state
                Text(viewModel.state.formattedTime)
                    .font(.system(size: 64, weight: .regular, design: .monospaced))
                
                // Milliseconds display (formatted as .SS)
                Text(viewModel.state.formattedMillis)
                    .font(.system(size: 32, weight: .regular, design: .monospaced))
                    .padding(.bottom, 12) // Aligns with the baseline of the larger text
            }
            
            /**
             * Control Panel
             * Horizontal arrangement for primary and secondary actions.
             */
            HStack(spacing: 20) {
                
                // Dynamic Start/Pause Toggle
                Button(action: {
                    // Logic flow depends on the current 'isRunning' state in the shared module
                    if viewModel.state.isRunning {
                        viewModel.pause()
                    } else {
                        viewModel.start()
                    }
                }) {
                    Text(viewModel.state.isRunning ? "Pause" : "Start")
                        .frame(width: 100)
                }
                .buttonStyle(.borderedProminent) // iOS native prominent styling

                // Reset Action
                Button(action: {
                    viewModel.reset()
                }) {
                    Text("Reset")
                        .frame(width: 100)
                }
                .buttonStyle(.bordered)
                .tint(.red) // Visual cue for a destructive/reset action
            }
        }
    }
}
