import SwiftUI
import FirebaseCore
import FirebaseCrashlytics
import FirebaseAnalytics

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        // Crashlytics and Analytics are automatically initialized with Firebase
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}


