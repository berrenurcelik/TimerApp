import SwiftUI

/**
 * iOSApp is the SwiftUI entry point — equivalent to MainActivity on Android.
 *
 * TabView gives us a native iOS bottom tab bar with two tabs:
 *   • Timer (existing stopwatch feature)
 *   • Focus (new galaxy pomodoro feature)
 *
 * Each tab gets its own NavigationStack, keeping the navigation state
 * of one tab independent from the other — standard iOS tab pattern.
 *
 * KMP NOTE: The ViewModel for each tab is created when the tab is first
 * selected and stays alive as long as the tab is in memory. Switching tabs
 * does not destroy the timer — the countdown keeps running in the background.
 */
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                // ── Tab 1: Stopwatch (existing feature) ──────────────────
                ContentView()
                    .tabItem {
                        Label("Stopwatch", systemImage: "timer")
                    }

                // ── Tab 2: Focus galaxy pomodoro (new feature) ───────────
                FocusView()
                    .tabItem {
                        Label("Focus", systemImage: "sparkles")
                    }
            }
        }
    }
}
