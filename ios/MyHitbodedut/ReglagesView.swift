import SwiftUI

struct ReglagesView: View {
    @EnvironmentObject private var appli: Appli

    var body: some View {
        NavigationStack {
            Form {
                Picker(appli.tr.t("settings_language"), selection: $appli.langue) {
                    ForEach(Traduction.langues, id: \.code) { l in
                        Text(l.nom).tag(l.code)
                    }
                }
                NavigationLink {
                    ChoixVilleView()
                } label: {
                    LabeledContent(appli.tr.t("zmanim_choisir_lieu"), value: appli.ville?.nom(en: appli.langue) ?? "")
                }
            }
            .navigationTitle(appli.tr.t("settings_title"))
        }
    }
}

struct ChoixVilleView: View {
    @EnvironmentObject private var appli: Appli
    @Environment(\.dismiss) private var fermer
    @State private var recherche = ""

    private var resultats: [Ville] {
        let r = recherche.trimmingCharacters(in: .whitespaces)
        let liste = r.isEmpty ? appli.villes : appli.villes.filter { v in
            v.nom.values.contains { $0.localizedCaseInsensitiveContains(r) }
        }
        return Array(liste.prefix(200))
    }

    var body: some View {
        List(resultats) { v in
            Button {
                appli.villeId = v.id
                fermer()
            } label: {
                HStack {
                    Text(v.nom(en: appli.langue))
                    Spacer()
                    Text(v.pays).foregroundStyle(.secondary)
                }
            }
        }
        .searchable(text: $recherche)
        .navigationTitle(appli.tr.t("zmanim_choisir_lieu"))
    }
}
