import SwiftUI
import shared

// MARK: - ViewModel

class FocusHistoryViewModel: ObservableObject {
    private let sessionRepo = FocusSessionRepository(driverFactory: DatabaseDriverFactory())

    @Published var summaries: [DailySummary] = []
    @Published var allTimeSecs: Int64 = 0

    init() { load() }

    func refresh() { load() }

    func clearAll() {
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self else { return }
            self.sessionRepo.clearAll()
            self.load()
        }
    }

    private func load() {
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self else { return }
            let s     = self.sessionRepo.getDailySummaries()
            let total = self.sessionRepo.getAllTimeFocusSecs()
            DispatchQueue.main.async {
                self.summaries   = s
                self.allTimeSecs = total
            }
        }
    }
}

// MARK: - Root View

struct FocusHistoryView: View {
    @StateObject private var vm = FocusHistoryViewModel()

    private let bg      = Color(red: 0.02, green: 0.04, blue: 0.09)
    private let card    = Color(red: 0.05, green: 0.08, blue: 0.15)
    private let purple  = Color(red: 0.73, green: 0.53, blue: 1.0)
    private let cyan    = Color(red: 0.00, green: 0.74, blue: 0.83)
    private let textDim = Color(red: 0.53, green: 0.57, blue: 0.66)

    @State private var showConfirm = false

    var body: some View {
        ZStack {
            bg.ignoresSafeArea()

            if vm.summaries.isEmpty {
                emptyState
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        allTimeBanner
                        sectionLabel
                        ForEach(vm.summaries, id: \.date) { summary in
                            dayCard(summary)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 24)
                }
            }
        }
        .onAppear { vm.refresh() }
        .confirmationDialog("Clear all focus history?", isPresented: $showConfirm, titleVisibility: .visible) {
            Button("Delete All", role: .destructive) { vm.clearAll() }
            Button("Cancel", role: .cancel) {}
        }
    }

    // MARK: All-time banner

    private var allTimeBanner: some View {
        let totalStars = min(Int(vm.allTimeSecs / 5), 200) // TEST: 5s/star
        let h = vm.allTimeSecs / 3600
        let m = (vm.allTimeSecs % 3600) / 60
        let s = vm.allTimeSecs % 60
        let timeLabel: String = {
            if h > 0 { return "\(h)h \(m)m" }
            if m > 0 { return "\(m)m \(s)s" }
            return "\(s)s"
        }()

        return VStack(spacing: 16) {
            Text("YOUR GALAXY")
                .font(.system(size: 11, weight: .light)).tracking(3)
                .foregroundColor(textDim)

            miniStarField(count: totalStars)

            HStack(spacing: 40) {
                statChip(value: timeLabel,        label: "TOTAL FOCUS")
                statChip(value: "\(totalStars)★", label: "STARS EARNED")
            }
            Button("Clear All History") { showConfirm = true }
                .font(.system(size: 13))
                .foregroundColor(Color(red: 0.81, green: 0.4, blue: 0.47))
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 20).fill(card))
    }

    private var sectionLabel: some View {
        HStack {
            Text("DAY BY DAY")
                .font(.system(size: 11, weight: .light)).tracking(2)
                .foregroundColor(textDim)
            Spacer()
        }
    }

    // MARK: Mini star grid

    private func miniStarField(count: Int) -> some View {
        let cols = 12
        let rows = 5
        let displayed = min(count, cols * rows)

        return VStack(spacing: 4) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 4) {
                    ForEach(0..<cols, id: \.self) { col in
                        let idx    = row * cols + col
                        let earned = idx < displayed
                        Circle()
                            .fill(earned ? purple.opacity(0.85) : Color.white.opacity(0.06))
                            .frame(width: earned ? 6 : 4, height: earned ? 6 : 4)
                    }
                }
            }
        }
    }

    // MARK: Day card

    private func dayCard(_ s: DailySummary) -> some View {
        let stars = min(Int(s.starCount), 8)
        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(formatDate(s.date))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white)
                Text("\(s.totalPauses) pauses · \(s.sessionCount) session\(s.sessionCount > 1 ? "s" : "")")
                    .font(.system(size: 12))
                    .foregroundColor(textDim)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(s.formattedDuration)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(cyan)
                HStack(spacing: 3) {
                    ForEach(0..<stars, id: \.self) { _ in
                        Circle()
                            .fill(purple.opacity(0.7))
                            .frame(width: 5, height: 5)
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(RoundedRectangle(cornerRadius: 16).fill(card))
    }

    // MARK: Empty state

    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("✦")
                .font(.system(size: 48))
                .foregroundColor(purple.opacity(0.4))
            Text("No focus sessions yet")
                .font(.system(size: 16, weight: .light))
                .foregroundColor(.white.opacity(0.5))
            Text("Start a session to earn your first stars")
                .font(.system(size: 13))
                .foregroundColor(textDim)
        }
    }

    // MARK: Stat chip

    private func statChip(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(purple)
            Text(label)
                .font(.system(size: 10)).tracking(1.5)
                .foregroundColor(textDim)
        }
    }

    // MARK: Helpers

    private func formatDate(_ iso: String) -> String {
        let input  = DateFormatter(); input.dateFormat  = "yyyy-MM-dd"
        let output = DateFormatter(); output.dateFormat = "EEEE, MMM d"
        guard let d = input.date(from: iso) else { return iso }
        return output.string(from: d)
    }
}
