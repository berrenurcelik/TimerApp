import Foundation
import shared

/**
 * iOS bridge between shared FocusRepository and SwiftUI.
 * Mirrors FocusViewModel.kt — same logic, Swift/Foundation instead of coroutines.
 */
class FocusiOSViewModel: ObservableObject {

    private let focusRepo   = FocusRepository()
    private let sessionRepo: FocusSessionRepository

    @Published var state: shared.FocusState
    @Published var allTimeSecs: Int64 = 0
    @Published var settings = shared.FocusSettings(
        workMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15, rounds: 4
    )

    private var timer: Timer?
    private var lastSavedCycles  = 0
    private var sessionStartedAt: Int64 = 0

    init() {
        sessionRepo = FocusSessionRepository(driverFactory: DatabaseDriverFactory())
        state = focusRepo.getState()          // initialize state first so self is fully ready
        focusRepo.applySettings(settings: settings)
        state = focusRepo.getState()          // re-read after settings applied
        loadAllTimeSecs()
    }

    func updateSettings(_ newSettings: shared.FocusSettings) {
        guard !state.isRunning else { return }
        settings = newSettings
        focusRepo.applySettings(settings: newSettings)
        state = focusRepo.getState()
    }

    func start() {
        if sessionStartedAt == 0 {
            sessionStartedAt = Int64(Date().timeIntervalSince1970 * 1000)
        }
        focusRepo.start()
        state = focusRepo.getState()
        startTickLoop()
    }

    func pause() {
        focusRepo.pause()
        stopTickLoop()
        state = focusRepo.getState()
    }

    func reset() {
        let snap = state
        if snap.sessionFocusSecs > 0 { persistSession(snap) }
        focusRepo.reset()
        stopTickLoop()
        state = focusRepo.getState()
        sessionStartedAt = 0
        lastSavedCycles  = 0
    }

    private func startTickLoop() {
        stopTickLoop()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self else { return }
            let prev = self.state
            let next = self.focusRepo.tick()
            self.state = next
            if Int(next.completedCycles) > self.lastSavedCycles {
                self.lastSavedCycles = Int(next.completedCycles)
                if prev.sessionFocusSecs > 0 {
                    self.persistSession(prev)
                    self.sessionStartedAt = Int64(Date().timeIntervalSince1970 * 1000)
                }
            }
        }
    }

    private func stopTickLoop() {
        timer?.invalidate()
        timer = nil
    }

    private func persistSession(_ state: shared.FocusState) {
        guard state.sessionFocusSecs > 0 else { return }
        let startedAt  = sessionStartedAt > 0 ? sessionStartedAt : Int64(Date().timeIntervalSince1970 * 1000)
        let date       = todayDate()
        let focusSecs  = state.sessionFocusSecs
        let pauseCount = state.pauseCount
        let roundsDone = state.completedRounds
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self else { return }
            self.sessionRepo.saveSession(date: date, focusSecs: focusSecs,
                pauseCount: pauseCount, roundsDone: roundsDone, startedAt: startedAt)
            let total = self.sessionRepo.getAllTimeFocusSecs()
            DispatchQueue.main.async { self.allTimeSecs = total }
        }
    }

    private func loadAllTimeSecs() {
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self else { return }
            let total = self.sessionRepo.getAllTimeFocusSecs()
            DispatchQueue.main.async { self.allTimeSecs = total }
        }
    }

    private func todayDate() -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    deinit { stopTickLoop() }
}
