import Foundation
import shared

/**
 * iOS bridge between shared FocusRepository and SwiftUI.
 * Mirrors FocusViewModel.kt — same logic, Swift/Foundation instead of coroutines.
 */
class FocusiOSViewModel: ObservableObject {

    private let repository = FocusRepository()
    @Published var state: shared.FocusState
    private var timer: Timer?

    init() {
        self.state = repository.getState()
    }

    func start() {
        repository.start()
        state = repository.getState()
        startTickLoop()
    }

    func pause() {
        repository.pause()
        stopTickLoop()
        state = repository.getState()
    }

    func reset() {
        repository.reset()
        stopTickLoop()
        state = repository.getState()
    }

    private func startTickLoop() {
        stopTickLoop()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.state = self.repository.tick()
        }
    }

    private func stopTickLoop() {
        timer?.invalidate()
        timer = nil
    }

    deinit { stopTickLoop() }
}
