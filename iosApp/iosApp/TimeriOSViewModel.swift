//
//  TimeriOSViewModel.swift
//  iosApp
//
//  Created by Berre Celik on 14.04.26.
//  Copyright © 2026 orgName. All rights reserved.
//
import Foundation
import shared

struct LapSession: Codable, Identifiable {
    let id: String
    let createdAt: Double
    let totalElapsedMillis: Int64
    let laps: [String]
}

class TimeriOSViewModel: ObservableObject {
    private let repository = TimerRepository()
    private let elapsedKey = "timer_elapsed_millis"
    private let lapsKey = "timer_laps"

    private let historyKey = "timer_history_sessions"
    @Published var history: [LapSession] = []

    // The source of truth for our SwiftUI views
    @Published var state = TimerState(elapsedMillis: 0, isRunning: false, laps: [])
    
    private var timer: Timer?

    init() {
        history = loadHistory()
        loadPersistedState()
    }
    
    private func loadHistory() -> [LapSession] {
        guard let data = UserDefaults.standard.data(forKey: historyKey) else { return [] }
        return (try? JSONDecoder().decode([LapSession].self, from: data)) ?? []
    }

    private func saveHistory() {
        guard let data = try? JSONEncoder().encode(history) else { return }
        UserDefaults.standard.set(data, forKey: historyKey)
    }
    
    private func appendCurrentSessionIfNeeded() {
        guard state.elapsedMillis > 0 || !state.laps.isEmpty else { return }

        let session = LapSession(
            id: UUID().uuidString,
            createdAt: Date().timeIntervalSince1970,
            totalElapsedMillis: state.elapsedMillis,
            laps: state.laps
        )

        history.insert(session, at: 0)
        saveHistory()
    }

    func deleteSession(id: String) {
        history.removeAll { $0.id == id }
        saveHistory()
    }

    func clearHistory() {
        history.removeAll()
        saveHistory()
    }
        
    private func loadPersistedState() {
        let elapsed = UserDefaults.standard.object(forKey: elapsedKey) as? Int64 ?? 0
        let laps = UserDefaults.standard.stringArray(forKey: lapsKey) ?? []
        repository.restoreState(elapsedMillis: elapsed, laps: laps)
        self.state = repository.getState()
    }

    private func persistState() {
        UserDefaults.standard.set(state.elapsedMillis, forKey: elapsedKey)
        UserDefaults.standard.set(state.laps, forKey: lapsKey)
    }

    func start() {
        repository.start()
        // Poll the repository every 10ms to match Android's smoothness
        timer = Timer.scheduledTimer(withTimeInterval: 0.01, repeats: true) { _ in
            self.state = self.repository.tick()
        }
    }

    func pause() {
        repository.pause()
        timer?.invalidate()
        self.state = repository.getState()
        persistState()
    }

    func reset() {
        appendCurrentSessionIfNeeded()
        repository.reset()
        timer?.invalidate()
        self.state = repository.getState()
        persistState()
    }

    func addLap() {
        repository.addLap()
        self.state = repository.getState()
        persistState()
    }
}
