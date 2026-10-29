// Les zmanim du jour, chacun avec sa méthode, comme Android et le web.

import SwiftUI

struct ZmanimView: View {
    @EnvironmentObject private var appli: Appli

    private var zmanim: [Zman] {
        guard let ville = appli.ville else { return [] }
        return (try? appli.moteur?.zmanim(ville.lieu, appli.isoAujourdhui())) ?? []
    }

    var body: some View {
        NavigationStack {
            List {
                if let ville = appli.ville {
                    Section {
                        ForEach(zmanim) { z in
                            HStack(alignment: .firstTextBaseline) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(appli.tr.t("zman_\(z.cle)"))
                                    Text(appli.tr.t("zman_methode_\(z.methode)", z.parametres))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(valeur(z))
                                    .monospacedDigit()
                                    .environment(\.layoutDirection, .leftToRight)
                            }
                        }
                    } header: {
                        Text(ville.nom(en: appli.langue))
                    } footer: {
                        Text(appli.tr.t("zmanim_a_valider") + "\n" + appli.tr.t("zmanim_source_villes"))
                    }
                }
            }
            .navigationTitle(appli.tr.t("zmanim_title"))
        }
    }

    private func valeur(_ z: Zman) -> String {
        switch z.statut {
        case "ok": return z.instant.map(appli.heure) ?? ""
        case "absent": return appli.tr.t("zmanim_absent")
        default: return appli.tr.t("zmanim_erreur")
        }
    }
}
