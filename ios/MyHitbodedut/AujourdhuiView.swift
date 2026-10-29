// L'accueil. Première étape : la date hébraïque et l'état du jour ; la
// séance, la série et la semaine viennent ensuite (cahier des charges, §3).

import SwiftUI
import UIKit

struct AujourdhuiView: View {
    @EnvironmentObject private var appli: Appli

    private var jour: JourHebreu? {
        guard let ville = appli.ville else { return nil }
        return try? appli.moteur?.calendrier(appli.isoAujourdhui(), enIsrael: ville.enIsrael)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if let image = Illustration.image("char_debout") {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 260)
                            .accessibilityHidden(true)
                    }
                    if let jour {
                        Text(jour.t)
                            .font(.title2)
                            .environment(\.layoutDirection, .rightToLeft)
                        if let paracha = jour.p {
                            Text(appli.tr.t("zmanim_paracha", paracha))
                                .foregroundStyle(.secondary)
                        }
                    }
                    Text(appli.tr.t("status_pending"))
                        .font(.headline)
                    if let erreur = appli.erreurMoteur {
                        Text(erreur).font(.footnote).foregroundStyle(.red)
                    }
                }
                .padding()
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
            .navigationTitle(appli.tr.t("app_name"))
        }
    }
}

enum Illustration {
    static func image(_ nom: String, bundle: Bundle = .main) -> UIImage? {
        guard let url = bundle.resourceURL?.appendingPathComponent("Genere/img/\(nom).webp") else { return nil }
        return UIImage(contentsOfFile: url.path)
    }
}
