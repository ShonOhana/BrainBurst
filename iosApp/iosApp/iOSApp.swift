import SwiftUI
import FirebaseCore
import FirebaseCrashlytics

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        // Crashlytics is automatically initialized with Firebase
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}


