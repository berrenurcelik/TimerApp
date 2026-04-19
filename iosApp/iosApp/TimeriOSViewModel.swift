//
//  TimeriOSViewModel.swift
//  iosApp
//
//  Created by Berre Celik on 14.04.26.
//  Copyright © 2026 orgName. All rights reserved.
//
import Foundation
import shared

class TimeriOSViewModel: ObservableObject {
    private let repository = TimerRepository()
    
    // The source of truth for our SwiftUI views
    @Published var state = TimerState(elapsedMillis: 0, isRunning: false, laps: [])
    
    private var timer: Timer?

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
        self.state = self.repository.getState()
    }

    func reset() {
        repository.reset()
        timer?.invalidate()
        self.state = self.repository.getState()
    }

    func addLap() {
        repository.addLap()
        // Refresh state to show the new lap immediately
        self.state = self.repository.getState()
    }
}
