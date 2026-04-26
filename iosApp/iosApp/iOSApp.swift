import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                ContentView()
                    .tabItem {
                        Label("Stopwatch", systemImage: "timer")
                    }

                FocusView()
                    .tabItem {
                        Label("Focus", systemImage: "sparkles")
                    }

                FocusHistoryView()
                    .tabItem {
                        Label("History", systemImage: "star.fill")
                    }
            }
        }
    }
}
