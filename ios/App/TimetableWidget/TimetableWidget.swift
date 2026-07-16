import WidgetKit
import SwiftUI

// ----- データモデル -----
struct Course: Codable, Identifiable {
    let id: String
    let name: String
    let classroom: String?
    let period: Int
    let day: String
}

struct PeriodTime: Codable {
    let start: String
    let end: String
}

// ----- タイムライン エントリー -----
struct SimpleEntry: TimelineEntry {
    let date: Date
    let courses: [Course]
    let periodTimes: [Int: PeriodTime]
    let useSystemTheme: Bool
    let themeColor: String
}

// ----- タイムライン プロバイダー -----
struct Provider: TimelineProvider {
    
    private let appGroup = "group.com.openyaske.app"
    
    private func getSharedData() -> (courses: [Course], periods: [Int: PeriodTime], useSystem: Bool, color: String) {
        guard let sharedPrefs = UserDefaults(suiteName: appGroup) else {
            return ([], [:], true, "#17288B")
        }
        
        let coursesJson = sharedPrefs.string(forKey: "courses") ?? "[]"
        let periodsJson = sharedPrefs.string(forKey: "timetablePeriods") ?? "{}"
        let color = sharedPrefs.string(forKey: "widgetThemeColor") ?? "#17288B"
        let useSystem = (sharedPrefs.string(forKey: "widgetUseSystemTheme") ?? "true") == "true"
        
        var courses: [Course] = []
        if let data = coursesJson.data(using: .utf8) {
            courses = (try? JSONDecoder().decode([Course].self, from: data)) ?? []
        }
        
        var periods: [Int: PeriodTime] = [:]
        if let data = periodsJson.data(using: .utf8),
           let decodedPeriods = try? JSONDecoder().decode([String: PeriodTime].self, from: data) {
            for (key, val) in decodedPeriods {
                if let intKey = Int(key) {
                    periods[intKey] = val
                }
            }
        }
        
        return (courses, periods, useSystem, color)
    }
    
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), courses: [], periodTimes: [:], useSystemTheme: true, themeColor: "#17288B")
    }

    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let data = getSharedData()
        let entry = SimpleEntry(
            date: Date(),
            courses: data.courses,
            periodTimes: data.periods,
            useSystemTheme: data.useSystem,
            themeColor: data.color
        )
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let data = getSharedData()
        let entry = SimpleEntry(
            date: Date(),
            courses: data.courses,
            periodTimes: data.periods,
            useSystemTheme: data.useSystem,
            themeColor: data.color
        )
        
        // 日付が変わるタイミング、または1時間ごとに更新するように設定
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date().addingTimeInterval(3600)
        let timeline = Timeline(entries: [entry], policy: .atEnd)
        completion(timeline)
    }
}

// ----- ユーティリティ -----
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// ----- SwiftUI ウィジェットビュー -----
struct TimetableWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var family
    @Environment(\.colorScheme) var colorScheme

    private var dayKey: String {
        let dayIndex = Calendar.current.component(.weekday, from: Date())
        switch dayIndex {
        case 1: return "sun"
        case 2: return "mon"
        case 3: return "tue"
        case 4: return "wed"
        case 5: return "thu"
        case 6: return "fri"
        case 7: return "sat"
        default: return "mon"
        }
    }

    private var dayText: String {
        switch dayKey {
        case "mon": return "月曜日"
        case "tue": return "火曜日"
        case "wed": return "水曜日"
        case "thu": return "木曜日"
        case "fri": return "金曜日"
        case "sat": return "土曜日"
        case "sun": return "日曜日"
        default: return "月曜日"
        }
    }

    private var todayCourses: [Int: Course] {
        var map: [Int: Course] = [:]
        for c in entry.courses {
            if c.day == dayKey && c.period >= 1 && c.period <= 7 {
                map[c.period] = c
            }
        }
        return map
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("今日の時間割")
                    .font(.system(size: 14, weight: .bold))
                Spacer()
                Text(dayText)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(entry.useSystemTheme ? .secondary : .white.opacity(0.8))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(entry.useSystemTheme ? Color.clear : Color(hex: entry.themeColor))
            .foregroundColor(entry.useSystemTheme ? .primary : .white)
            
            if entry.useSystemTheme {
                Divider()
            }

            // Body
            if family == .systemSmall {
                // スモールサイズウィジェット: 直近の授業を表示
                VStack(alignment: .leading, spacing: 4) {
                    Spacer()
                    let nextPeriods = (1...7).filter { todayCourses[$0] != nil }
                    if let currentPeriod = nextPeriods.first, let c = todayCourses[currentPeriod] {
                        Text("\(c.period)限目の授業")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(c.name)
                            .font(.system(size: 15, weight: .bold))
                            .lineLimit(2)
                        if let classroom = c.classroom, !classroom.isEmpty {
                            Text(classroom)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    } else {
                        Text("今日の授業は")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("ありません")
                            .font(.system(size: 14, weight: .bold))
                    }
                    Spacer()
                }
                .padding(.horizontal, 12)
            } else {
                // ミディアム/ラージサイズウィジェット: 1〜7限までのリストを表示
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(1...7, id: \.self) { p in
                            HStack(spacing: 8) {
                                // Period number
                                Text("\(p)")
                                    .font(.system(size: 11, weight: .bold))
                                    .frame(width: 20, height: 20)
                                    .background(Color(colorScheme == .dark ? .systemGray5 : .systemGray6))
                                    .clipShape(Circle())
                                
                                // Course detail
                                if let course = todayCourses[p] {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(course.name)
                                            .font(.system(size: 12, weight: .bold))
                                            .lineLimit(1)
                                        if let classroom = course.classroom, !classroom.isEmpty {
                                            Text(classroom)
                                                .font(.system(size: 10))
                                                .foregroundColor(.secondary)
                                                .lineLimit(1)
                                        }
                                    }
                                } else {
                                    Text("空きコマ")
                                        .font(.system(size: 12))
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                // Period time
                                if let time = entry.periodTimes[p] {
                                    Text("\(time.start)-\(time.end)")
                                        .font(.system(size: 9))
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            
                            if p < 7 {
                                Divider().padding(.leading, 32)
                            }
                        }
                    }
                }
            }
        }
        .background(Color(colorScheme == .dark ? .black : .white))
    }
}

// ----- Widget定義 -----
@main
struct TimetableWidget: Widget {
    let kind: String = "TimetableWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            TimetableWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("時間割")
        .description("当日の時間割情報をホーム画面に表示します。")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
