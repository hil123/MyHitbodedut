import SwiftUI

@main
struct MyHitbodedutApp: App {
    @StateObject private var appli = Appli()

    var body: some Scene {
        WindowGroup {
            RacineView()
                .environmentObject(appli)
                .environment(\.layoutDirection, appli.droiteAGauche ? .rightToLeft : .leftToRight)
                .environment(\.locale, Locale(identifier: appli.langue))
        }
    }
}

struct RacineView: View {
    @EnvironmentObject private var appli: Appli

    var body: some View {
        TabView {
            AujourdhuiView()
                .tabItem { Label(appli.tr.t("nav_today"), systemImage: "sun.max") }
            ZmanimView()
                .tabItem { Label(appli.tr.t("nav_zmanim"), systemImage: "clock") }
            ReglagesView()
                .tabItem { Label(appli.tr.t("settings_title"), systemImage: "gearshape") }
        }
    }
}
