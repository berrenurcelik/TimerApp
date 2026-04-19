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
    @Published var state = TimerState(elapsedMillis: 0, isRunning: false)
    private var timer: Timer?

    func start() {
        repository.start()
        startTicking()
    }

    func pause() {
        repository.pause()
        timer?.invalidate()
    }

    func reset() {
        repository.reset()
        timer?.invalidate()
        self.state = repository.tick()
    }

    private func startTicking() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.01, repeats: true) { _ in
            self.state = self.repository.tick()
        }
    }
}
